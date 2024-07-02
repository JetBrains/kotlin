/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.lastExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.FirNamedReferenceWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.candidate
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaReturnType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedCallableReferenceMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

//  -------------------------- Atoms --------------------------

sealed class ConeResolutionAtom : AbstractConeResolutionAtom() {
    companion object {
        @JvmName("createRawAtomNullable")
        fun createRawAtom(expression: FirExpression?): ConeResolutionAtom? {
            return expression?.let { createRawAtom(it) }
        }

        fun createRawAtom(expression: FirExpression): ConeResolutionAtom {
            return when (expression) {
                is FirAnonymousFunctionExpression -> ConeResolutionAtomWithPostponedChild(expression)
                is FirCallableReferenceAccess -> when {
                    expression.isResolved -> ConeSimpleLeafResolutionAtom(expression)
                    else -> ConeResolutionAtomWithPostponedChild(expression)
                }
                is FirSafeCallExpression -> ConeResolutionAtomWithSingleChild(
                    expression,
                    createRawAtom((expression.selector as? FirExpression)?.unwrapSmartcastExpression())
                )
                is FirResolvable -> when (val candidate = expression.candidate()) {
                    null -> ConeSimpleLeafResolutionAtom(expression)
                    else -> ConeAtomWithCandidate(expression, candidate)
                }
                is FirWrappedArgumentExpression -> ConeResolutionAtomWithSingleChild(expression, createRawAtom(expression.expression))
                is FirErrorExpression -> ConeResolutionAtomWithSingleChild(expression, createRawAtom(expression.expression))
                is FirBlock -> ConeResolutionAtomWithSingleChild(expression, createRawAtom(expression.lastExpression))
                else -> ConeSimpleLeafResolutionAtom(expression)
            }
        }
    }
}

class ConeResolutionAtomWithSingleChild(override val fir: FirExpression, val subAtom: ConeResolutionAtom?) : ConeResolutionAtom() {
    override val expression: FirExpression
        get() = fir
}

class ConeSimpleLeafResolutionAtom(override val fir: FirExpression) : ConeResolutionAtom() {
// TODO: investigate possibility to enable this check. KT-69557
//    init {
//        check(fir.isResolved) { "ConeResolvedAtom should be created only for resolved expressions" }
//    }

    override val expression: FirExpression
        get() = fir
}

//  -------------------------- Not-resolved atoms --------------------------

class ConeAtomWithCandidate(
    override val fir: FirResolvable,
    val candidate: Candidate
) : ConeResolutionAtom() {
    override val expression: FirExpression
        get() = fir as FirExpression
}

class ConeResolutionAtomWithPostponedChild(override val fir: FirExpression) : ConeResolutionAtom() {
    override val expression: FirExpression
        get() = fir

    var subAtom: ConePostponedResolvedAtom? = null
        set(value) {
            require(field == null) { "subAtom already initialized" }
            field = value
        }
}

sealed class ConePostponedResolvedAtom : ConeResolutionAtom(), PostponedResolvedAtomMarker {
    abstract override val inputTypes: Collection<ConeKotlinType>
    abstract override val outputType: ConeKotlinType?
    override var analyzed: Boolean = false
    abstract override val expectedType: ConeKotlinType?
}

//  ------------- Lambdas -------------

class ConeResolvedLambdaAtom(
    override val fir: FirAnonymousFunction,
    private val _expression: FirAnonymousFunctionExpression?,
    expectedType: ConeKotlinType?,
    val expectedFunctionTypeKind: FunctionTypeKind?,
    val receiver: ConeKotlinType?,
    val contextReceivers: List<ConeKotlinType>,
    val parameters: List<ConeKotlinType>,
    var returnType: ConeKotlinType,
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType?,
    val coerceFirstParameterToExtensionReceiver: Boolean,
    // NB: It's not null right now only for lambdas inside the calls
    // TODO: Handle somehow that kind of lack of information once KT-67961 is fixed
    val sourceForFunctionExpression: KtSourceElement?,
) : ConePostponedResolvedAtom() {
    override val expression: FirExpression
        get() = _expression ?: errorWithAttachment("No expression for lambda") {
            withFirEntry("lambda", fir)
        }

    var typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = typeVariableForLambdaReturnType
        private set

    override var expectedType: ConeKotlinType? = expectedType
        private set

    lateinit var returnStatements: Collection<ConeResolutionAtom>

    override val inputTypes: Collection<ConeKotlinType>
        get() {
            if (receiver == null && contextReceivers.isEmpty()) return parameters
            return ArrayList<ConeKotlinType>(parameters.size + contextReceivers.size + (if (receiver != null) 1 else 0)).apply {
                addAll(parameters)
                addIfNotNull(receiver)
                addAll(contextReceivers)
            }
        }

    override val outputType: ConeKotlinType get() = returnType

    fun replaceExpectedType(expectedType: ConeKotlinType, newReturnType: ConeTypeVariableType) {
        this.expectedType = expectedType
        this.returnType = newReturnType
    }

    fun replaceTypeVariableForLambdaReturnType(typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType) {
        this.typeVariableForLambdaReturnType = typeVariableForLambdaReturnType
    }
}

class ConeLambdaWithTypeVariableAsExpectedTypeAtom(
    override val expression: FirAnonymousFunctionExpression,
    private val initialExpectedTypeType: ConeKotlinType,
    val candidateOfOuterCall: Candidate,
) : ConePostponedResolvedAtom(), LambdaWithTypeVariableAsExpectedTypeMarker {
    override val fir: FirAnonymousFunction = expression.anonymousFunction

    var subAtom: ConeResolvedLambdaAtom? = null
        set(value) {
            require(field == null) { "subAtom already initialized" }
            field = value
        }

    override var parameterTypesFromDeclaration: List<ConeKotlinType?>? = null
        private set

    override fun updateParameterTypesFromDeclaration(types: List<KotlinTypeMarker?>?) {
        @Suppress("UNCHECKED_CAST")
        types as List<ConeKotlinType?>?
        parameterTypesFromDeclaration = types
    }

    override val expectedType: ConeKotlinType
        get() = revisedExpectedType ?: initialExpectedTypeType

    override val inputTypes: Collection<ConeKotlinType> get() = listOf(initialExpectedTypeType)
    override val outputType: ConeKotlinType? get() = null
    override var revisedExpectedType: ConeKotlinType? = null
        private set

    override fun reviseExpectedType(expectedType: KotlinTypeMarker) {
        require(expectedType is ConeKotlinType)
        revisedExpectedType = expectedType
    }
}

//  ------------- References -------------

class ConeResolvedCallableReferenceAtom(
    override val fir: FirCallableReferenceAccess,
    private val initialExpectedType: ConeKotlinType?,
    val lhs: DoubleColonLHS?,
    private val session: FirSession
) : ConePostponedResolvedAtom(), PostponedCallableReferenceMarker {
    override val expression: FirCallableReferenceAccess
        get() = fir

    var subAtom: ConeAtomWithCandidate? = null
        private set

    var hasBeenResolvedOnce: Boolean = false
    var hasBeenPostponed: Boolean = false

    val mightNeedAdditionalResolution: Boolean get() = !hasBeenResolvedOnce || hasBeenPostponed

    var resultingReference: FirNamedReference? = null
        private set

    fun initializeResultingReference(resultingReference: FirNamedReference) {
        require(this.resultingReference == null) { "resultingReference already initialized" }
        this.resultingReference = resultingReference
        val candidate = (resultingReference as? FirNamedReferenceWithCandidate)?.candidate
        if (candidate != null) {
            subAtom = ConeAtomWithCandidate(fir, candidate)
        }
    }

    var resultingTypeForCallableReference: ConeKotlinType? = null

    override val inputTypes: Collection<ConeKotlinType>
        get() {
            if (!hasBeenPostponed) return emptyList()
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.inputTypes
                ?: listOfNotNull(expectedType)
        }
    override val outputType: ConeKotlinType?
        get() {
            if (!hasBeenPostponed) return null
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.outputType
        }

    override val expectedType: ConeKotlinType?
        get() = if (!hasBeenPostponed)
            initialExpectedType
        else
            revisedExpectedType ?: initialExpectedType

    override var revisedExpectedType: ConeKotlinType? = null
        get() = if (hasBeenPostponed) field else expectedType
        private set

    override fun reviseExpectedType(expectedType: KotlinTypeMarker) {
        if (!mightNeedAdditionalResolution) return
        require(expectedType is ConeKotlinType)
        revisedExpectedType = expectedType
    }
}

//  -------------------------- Utils --------------------------

internal data class InputOutputTypes(val inputTypes: List<ConeKotlinType>, val outputType: ConeKotlinType)

internal fun extractInputOutputTypesFromCallableReferenceExpectedType(
    expectedType: ConeKotlinType?,
    session: FirSession
): InputOutputTypes? {
    if (expectedType == null) return null

    return when {
        expectedType.isSomeFunctionType(session) ->
            InputOutputTypes(expectedType.valueParameterTypesIncludingReceiver(session), expectedType.returnType(session))

//        ReflectionTypes.isBaseTypeForNumberedReferenceTypes(expectedType) ->
//            InputOutputTypes(emptyList(), expectedType.arguments.single().type.unwrap())
//
//        ReflectionTypes.isNumberedKFunction(expectedType) -> {
//            val functionFromSupertype = expectedType.immediateSupertypes().first { it.isFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(functionFromSupertype)
//        }
//
//        ReflectionTypes.isNumberedKSuspendFunction(expectedType) -> {
//            val kSuspendFunctionType = expectedType.immediateSupertypes().first { it.isSuspendFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(kSuspendFunctionType)
//        }
//
//        ReflectionTypes.isNumberedKPropertyOrKMutablePropertyType(expectedType) -> {
//            val functionFromSupertype = expectedType.supertypes().first { it.isFunctionType }.unwrap()
//            extractInputOutputTypesFromFunctionType(functionFromSupertype)
//        }

        else -> null
    }
}

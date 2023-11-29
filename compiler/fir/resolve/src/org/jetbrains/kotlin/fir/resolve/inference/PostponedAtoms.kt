/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.builtins.functions.FunctionTypeKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.expressions.FirAnonymousFunctionExpression
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.resolve.DoubleColonLHS
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedCallableReferenceMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.utils.addIfNotNull

//  --------------------------- Variables ---------------------------


//  -------------------------- Atoms --------------------------

sealed class PostponedResolvedAtom : PostponedResolvedAtomMarker {
    abstract val atom: FirElement
    abstract override val inputTypes: Collection<ConeKotlinType>
    abstract override val outputType: ConeKotlinType?
    override var analyzed: Boolean = false
    abstract override val expectedType: ConeKotlinType?
}

//  ------------- Lambdas -------------

class ResolvedLambdaAtom(
    override val atom: FirAnonymousFunction,
    expectedType: ConeKotlinType?,
    val expectedFunctionTypeKind: FunctionTypeKind?,
    val receiver: ConeKotlinType?,
    val contextReceivers: List<ConeKotlinType>,
    val parameters: List<ConeKotlinType>,
    var returnType: ConeKotlinType,
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType?,
    val coerceFirstParameterToExtensionReceiver: Boolean
) : PostponedResolvedAtom() {

    var typeVariableForLambdaReturnType = typeVariableForLambdaReturnType
        private set

    override var expectedType = expectedType
        private set

    lateinit var returnStatements: Collection<FirExpression>

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

    /**
     * Set to true by resolution stage, if the corresponding parameter has BuilderInference annotation
     * ```kotlin
     *     fun foo(@BuilderInference b: Buildee<T>.() -> Unit) {}
     *     fun test() = foo({ ... }) // Will be true for the lambda argument passed to b
     * ```
     */
    var isCorrespondingParameterAnnotatedWithBuilderInference: Boolean = false
}

class LambdaWithTypeVariableAsExpectedTypeAtom(
    override val atom: FirAnonymousFunctionExpression,
    private val initialExpectedTypeType: ConeKotlinType,
    val candidateOfOuterCall: Candidate,
) : PostponedResolvedAtom(), LambdaWithTypeVariableAsExpectedTypeMarker {

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

class ResolvedCallableReferenceAtom(
    val reference: FirCallableReferenceAccess,
    private val initialExpectedType: ConeKotlinType?,
    val lhs: DoubleColonLHS?,
    private val session: FirSession
) : PostponedResolvedAtom(), PostponedCallableReferenceMarker {

    override val atom: FirCallableReferenceAccess
        get() = reference

    var hasBeenResolvedOnce: Boolean = false
    var hasBeenPostponed: Boolean = false

    val mightNeedAdditionalResolution get() = !hasBeenResolvedOnce || hasBeenPostponed

    var resultingReference: FirNamedReference? = null
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

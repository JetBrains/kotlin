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
import org.jetbrains.kotlin.fir.resolve.calls.stages.FirFakeArgumentForCallableReference
import org.jetbrains.kotlin.fir.resolve.inference.ConeTypeVariableForLambdaReturnType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.utils.exceptions.withFirEntry
import org.jetbrains.kotlin.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedCallableReferenceMarker
import org.jetbrains.kotlin.resolve.calls.model.PostponedResolvedAtomMarker
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.exceptions.checkWithAttachment

//  -------------------------- Atoms --------------------------

/**
 * [ConeResolutionAtom] is an abstract representation calls component (like argument or receiver) which is needed
 *   for convenient representation of all different kinds of arguments in the original order. Regular expressions cannot be
 *   used for this purpose, as we have postponed arguments in the resolution logic (callable references and lambdas), which
 *   require some additional information for resolution, which is provided on different steps of the resolution pipeline. So atoms
 *   allow tracking all those transformations and keep the original structure of the call at the same time.
 *
 * There are multiple different kinds of atoms:
 * - [ConeAtomWithCandidate] used for arguments, which are not completed yet and contain [Candidate] inside
 * - [ConeResolutionAtomWithSingleChild] used for expressions, which wrap some other expression. Atom of underlying expression
 *     is stored inside [ConeResolutionAtomWithSingleChild.subAtom]
 * - [ConeResolutionAtomWithPostponedChild] is created for postponed arguments at the first stage of argument processing
 *     and contain mutable [subAtom] property for more specific [ConePostponedResolvedAtom] which will be initialized later
 * - [ConePostponedResolvedAtom] and its inheritors are special atoms for proper lambda and callable reference resolution
 * - [ConeSimpleLeafResolutionAtom] is an atom for regular already completed expressions
 */
sealed class ConeResolutionAtom : AbstractConeResolutionAtom() {
    abstract override val expression: FirExpression

    companion object {
        @JvmName("createRawAtomNullable")
        fun createRawAtom(expression: FirExpression?): ConeResolutionAtom? {
            return createRawAtom(expression, allowUnresolvedExpression = false)
        }

        fun createRawAtom(expression: FirExpression): ConeResolutionAtom {
            return createRawAtom(expression, allowUnresolvedExpression = false)!!
        }

        /**
         * Creation of atoms based on potentially unresolved arguments is unsafe, as most of the atoms consumers
         *   expect that `ConeResolutionAtom.expression.resolvedType` is initialized. This way of atom creation
         *   is allowed to use only for creating arguments mapping with a condition that those atoms won't be used
         *   in the further resolution pipeline (like in `CheckArguments` stage or in call completion)
         *
         * The only known case for such potentially unresolved atoms is annotation resolution, which pipeline
         *   is following:
         * - create argument/parameter mapping
         * - analyze arguments with an expected type from corresponding parameters
         * - run proper resolution sequence for annotation constructor call (with creation of new atoms)
         */
        @UnsafeExpressionUtility
        fun createRawAtomForPotentiallyUnresolvedExpression(expression: FirExpression): ConeResolutionAtom {
            return createRawAtom(expression, allowUnresolvedExpression = true)!!
        }

        private fun createRawAtom(expression: FirExpression?, allowUnresolvedExpression: Boolean): ConeResolutionAtom? {
            return when (expression) {
                null -> null
                is FirAnonymousFunctionExpression -> ConeResolutionAtomWithPostponedChild(expression)
                is FirCallableReferenceAccess -> when {
                    expression.isResolved -> ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression)
                    else -> ConeResolutionAtomWithPostponedChild(expression)
                }
                is FirSafeCallExpression -> ConeResolutionAtomWithSingleChild(
                    expression,
                    createRawAtom((expression.selector as? FirExpression)?.unwrapSmartcastExpression(), allowUnresolvedExpression)
                )
                is FirResolvable -> when (val candidate = expression.candidate()) {
                    null -> ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression)
                    else -> ConeAtomWithCandidate(expression, candidate)
                }
                is FirWrappedArgumentExpression -> ConeResolutionAtomWithSingleChild(
                    expression,
                    createRawAtom(expression.expression, allowUnresolvedExpression)
                )
                is FirErrorExpression -> ConeResolutionAtomWithSingleChild(
                    expression,
                    createRawAtom(expression.expression, allowUnresolvedExpression)
                )
                is FirBlock -> ConeResolutionAtomWithSingleChild(
                    expression,
                    createRawAtom(expression.lastExpression, allowUnresolvedExpression)
                )
                else -> ConeSimpleLeafResolutionAtom(expression, allowUnresolvedExpression)
            }
        }
    }
}

class ConeResolutionAtomWithSingleChild(override val expression: FirExpression, val subAtom: ConeResolutionAtom?) : ConeResolutionAtom()

class ConeSimpleLeafResolutionAtom(override val expression: FirExpression, allowUnresolvedExpression: Boolean) : ConeResolutionAtom() {
    init {
        if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
            checkWithAttachment(
                allowUnresolvedExpression ||
                        expression.unwrapArgument() is FirFakeArgumentForCallableReference ||
                        expression.isResolved,
                { "ConeResolvedAtom should be created only for resolved expressions" }
            ) {
                withFirEntry("expression", expression)
            }
        }
    }
}

//  -------------------------- Not-resolved atoms --------------------------

class ConeAtomWithCandidate(override val expression: FirExpression, val candidate: Candidate) : ConeResolutionAtom()

class ConeResolutionAtomWithPostponedChild(override val expression: FirExpression) : ConeResolutionAtom() {
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
    override val expression: FirAnonymousFunctionExpression,
    expectedType: ConeKotlinType?,
    val expectedFunctionTypeKind: FunctionTypeKind?,
    internal val receiverType: ConeKotlinType?,
    internal val contextParameterTypes: List<ConeKotlinType>,
    internal val parameterTypes: List<ConeKotlinType>,
    var returnType: ConeKotlinType,
    typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType?,
    val coerceFirstParameterToExtensionReceiver: Boolean,
    // NB: It's not null right now only for lambdas inside the calls
    // TODO: Handle somehow that kind of lack of information once KT-67961 is fixed
    val sourceForFunctionExpression: KtSourceElement?,
) : ConePostponedResolvedAtom() {
    val anonymousFunction: FirAnonymousFunction = expression.anonymousFunction

    var typeVariableForLambdaReturnType: ConeTypeVariableForLambdaReturnType? = typeVariableForLambdaReturnType
        private set

    override var expectedType: ConeKotlinType? = expectedType
        private set

    lateinit var returnStatements: Collection<ConeResolutionAtom>

    override val inputTypes: Collection<ConeKotlinType>
        get() {
            if (receiverType == null && contextParameterTypes.isEmpty()) return parameterTypes
            return ArrayList<ConeKotlinType>(parameterTypes.size + contextParameterTypes.size + (if (receiverType != null) 1 else 0)).apply {
                addAll(parameterTypes)
                addIfNotNull(receiverType)
                addAll(contextParameterTypes)
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
    val anonymousFunction: FirAnonymousFunction = expression.anonymousFunction

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
    override val expression: FirCallableReferenceAccess,
    private val initialExpectedType: ConeKotlinType?,
    val lhs: DoubleColonLHS?,
    private val session: FirSession
) : ConePostponedResolvedAtom(), PostponedCallableReferenceMarker {
    var subAtom: ConeAtomWithCandidate? = null
        private set

    enum class State(val needsResolution: Boolean) {
        // Regularly, the first time we resolve `::bar` of `foo(::bar)` at `EagerResolveOfCallableReferences` of `foo`
        // Might be transformed both to `POSTPONED_BECAUSE_OF_AMBIGUITY` or `RESOLVED`
        NOT_RESOLVED_YET(needsResolution = true),

        // Means that we should try resolving again at the completion stage when the expected type is ready
        // Might be transformed only to `RESOLVED`
        POSTPONED_BECAUSE_OF_AMBIGUITY(needsResolution = true),

        // That would correspond both to successful and failed results (including final ambiguity)
        RESOLVED(needsResolution = false),
    }

    var state: State = State.NOT_RESOLVED_YET

    val isPostponedBecauseOfAmbiguity: Boolean get() = state == State.POSTPONED_BECAUSE_OF_AMBIGUITY

    val needsResolution: Boolean get() = state.needsResolution

    var resultingReference: FirNamedReference? = null
        private set

    fun initializeResultingReference(resultingReference: FirNamedReference) {
        require(this.resultingReference == null) { "resultingReference already initialized" }
        this.resultingReference = resultingReference
        this.state = State.RESOLVED
        val candidate = (resultingReference as? FirNamedReferenceWithCandidate)?.candidate
        if (candidate != null) {
            subAtom = ConeAtomWithCandidate(expression, candidate)
        }
    }

    var resultingTypeForCallableReference: ConeKotlinType? = null

    override val inputTypes: Collection<ConeKotlinType>
        get() {
            // For not resolved references we don't expose input types because for the first time,
            // we should try resolving them immediately (effectively, they're not fully blown postponed atoms)
            if (state == State.NOT_RESOLVED_YET) return emptyList()
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.inputTypes
                ?: listOfNotNull(expectedType)
        }
    override val outputType: ConeKotlinType?
        get() {
            // For not resolved references we don't expose the output type because for the first time,
            // we should try resolving them immediately (effectively, they're not fully blown postponed atoms)
            if (state == State.NOT_RESOLVED_YET) return null
            return extractInputOutputTypesFromCallableReferenceExpectedType(expectedType, session)?.outputType
        }

    override val expectedType: ConeKotlinType?
        // TODO: Consider changing `!isPostponedBecauseOfAmbiguity` to `state == State.NOT_RESOLVED_YET` (KT-74021)
        get() = if (!isPostponedBecauseOfAmbiguity)
            initialExpectedType
        else
            revisedExpectedType ?: initialExpectedType

    override var revisedExpectedType: ConeKotlinType? = null
        // TODO: Consider simplifying this (KT-74021)
        get() = if (isPostponedBecauseOfAmbiguity) field else expectedType
        private set

    override fun reviseExpectedType(expectedType: KotlinTypeMarker) {
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
    val expectedClassLikeType = expectedType?.lowerBoundIfFlexible() as? ConeClassLikeType ?: return null

    return when {
        expectedClassLikeType.isSomeFunctionType(session) ->
            InputOutputTypes(expectedClassLikeType.valueParameterTypesIncludingReceiver(session), expectedClassLikeType.returnType(session))

        else -> null
    }
}

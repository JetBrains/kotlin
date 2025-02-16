/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithCandidate
import org.jetbrains.kotlin.fir.resolve.calls.ConeResolutionAtom
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.candidate
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeSemiFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.BodyResolveContext
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.defaultType
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * @see [docs/fir/pcla.md]
 */
class FirPCLAInferenceSession(
    private val outerCandidate: Candidate,
    private val inferenceComponents: InferenceComponents,
) : FirInferenceSession() {

    var currentCommonSystem: NewConstraintSystemImpl = prepareSharedBaseSystem(outerCandidate.system, inferenceComponents)
        private set

    override fun baseConstraintStorageForCandidate(candidate: Candidate, bodyResolveContext: BodyResolveContext): ConstraintStorage? {
        if (candidate.mightBeAnalyzedAndCompletedIndependently(bodyResolveContext.returnTypeCalculator)) return null

        return currentCommonSystem.currentStorage()
    }

    override fun customCompletionModeInsteadOfFull(
        call: FirResolvable,
    ): ConstraintSystemCompletionMode? = when {
        call.candidate()?.usedOuterCs == true -> ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL
        else -> null
    }

    override fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : FirResolvable, T : FirExpression {
        call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)

        val candidate = call.candidate()
        if (candidate?.usedOuterCs != true) return

        // Integrating back would happen at FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot
        // after all other delegation-related calls are being analyzed
        if (resolutionMode == ResolutionMode.Delegate) return

        currentCommonSystem.replaceContentWith(candidate.system.currentStorage())

        if (completionMode == ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL) {
            outerCandidate.postponedPCLACalls += ConeAtomWithCandidate(call, candidate)
        }
    }

    override fun runLambdaCompletion(candidate: Candidate, forOverloadByLambdaReturnType: Boolean, block: () -> Unit): ConstraintStorage? {
        if (forOverloadByLambdaReturnType) {
            val constraintAccumulatorForLambda =
                inferenceComponents.createConstraintSystem().apply {
                    setBaseSystem(currentCommonSystem.currentStorage())
                }

            runWithSpecifiedCurrentCommonSystem(constraintAccumulatorForLambda, block)

            return constraintAccumulatorForLambda.currentStorage()
        }

        runWithSpecifiedCurrentCommonSystem(candidate.system, block)

        return null
    }

    private fun <T> runWithSpecifiedCurrentCommonSystem(newSystem: NewConstraintSystemImpl, block: () -> T): T {
        val previous = currentCommonSystem
        return try {
            currentCommonSystem = newSystem
            block()
        } finally {
            currentCommonSystem = previous
        }
    }

    fun applyResultsToMainCandidate() {
        outerCandidate.system.replaceContentWith(currentCommonSystem.currentStorage())
    }

    // Currently used only from FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot
    fun integrateChildSession(
        childCalls: Collection<ConeResolutionAtom>,
        childStorage: ConstraintStorage,
        onCompletionResultsWriting: (ConeSubstitutor) -> Unit,
    ) {
        outerCandidate.postponedPCLACalls += childCalls
        // When a delegated property belongs to a PCLA lambda, the delegate session is guaranteed either use
        // - either a delegate call which is always a nested PCLA call with an outer CS
        // - or it literally uses `currentCommonSystem` (see the definition of FirDelegatedPropertyInferenceSession.parentConstraintSystem)
        currentCommonSystem.replaceContentWith(childStorage)
        outerCandidate.onPCLACompletionResultsWritingCallbacks += onCompletionResultsWriting
    }

    @OptIn(TemporaryInferenceSessionHook::class) // Needed to override
    override fun updateExpressionReturnTypeWithCurrentSubstitutorInPCLA(expression: FirExpression, resolutionMode: ResolutionMode) {
        expression.updateReturnTypeWithCurrentSubstitutor(resolutionMode)
    }

    private fun FirExpression.updateReturnTypeWithCurrentSubstitutor(
        resolutionMode: ResolutionMode,
    ) {
        val system = (this as? FirResolvable)?.candidate()?.system ?: currentCommonSystem

        val additionalBinding = runIf(resolutionMode is ResolutionMode.ReceiverResolution) {
            semiFixCurrentResultIfTypeVariableAndReturnBinding(resolvedType, system)
        }

        val substitutor = system.buildCurrentSubstitutor(additionalBinding) as ConeSubstitutor
        val updatedType = substitutor.substituteOrNull(resolvedType)

        if (updatedType != null) {
            replaceConeTypeOrNull(updatedType)
        }
    }

    override fun getAndSemiFixCurrentResultIfTypeVariable(type: ConeKotlinType): ConeKotlinType? =
        semiFixCurrentResultIfTypeVariableAndReturnBinding(type, currentCommonSystem)?.second

    fun semiFixCurrentResultIfTypeVariableAndReturnBinding(
        type: ConeKotlinType,
        myCs: NewConstraintSystemImpl,
    ): Pair<ConeTypeVariableTypeConstructor, ConeKotlinType>? {
        val coneTypeVariableTypeConstructor = (type.unwrapToSimpleTypeUsingLowerBound() as? ConeTypeVariableType)?.typeConstructor
            ?: return null

        require(coneTypeVariableTypeConstructor in myCs.allTypeVariables) {
            "$coneTypeVariableTypeConstructor not found"
        }

        val variableWithConstraints = myCs.notFixedTypeVariables[coneTypeVariableTypeConstructor] ?: return null
        val c = myCs.getBuilder()

        if (coneTypeVariableTypeConstructor in myCs.outerTypeVariables.orEmpty()
            // Since 2.1 we do not differentiate outer variables in terms of semi-fixation
            && !is21Mode()
        ) {
            // For outer TV, we don't allow semi-fixing them (adding the new equality constraints),
            // but if there's already some proper EQ constraint, it's safe & sound to use it as a representative
            c.prepareContextForTypeVariableForSemiFixation(coneTypeVariableTypeConstructor) {
                inferenceComponents.resultTypeResolver.findResultIfThereIsEqualsConstraint(
                    c,
                    variableWithConstraints,
                    isStrictMode = true,
                ) as ConeKotlinType?
            }?.let { appropriateResultType ->
                return Pair(coneTypeVariableTypeConstructor, appropriateResultType)
            }

            return null
        }

        val resultType = c.prepareContextForTypeVariableForSemiFixation(coneTypeVariableTypeConstructor) {
            inferenceComponents.resultTypeResolver.findResultType(
                c,
                variableWithConstraints,
                TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
            ) as ConeKotlinType
        } ?: return null
        val variable = variableWithConstraints.typeVariable
        c.addEqualityConstraint(variable.defaultType(c), resultType, ConeSemiFixVariableConstraintPosition(variable))

        return Pair(coneTypeVariableTypeConstructor, resultType)
    }

    private fun ConstraintSystemCompletionContext.prepareContextForTypeVariableForSemiFixation(
        coneTypeVariableTypeConstructor: ConeTypeVariableTypeConstructor,
        resultTypeCallback: () -> ConeKotlinType?,
    ): ConeKotlinType? = withTypeVariablesThatAreCountedAsProperTypes(
        if (is21Mode())
            notFixedTypeVariables.keys
        else
            outerTypeVariables.orEmpty()
    ) {
        if (!inferenceComponents.variableFixationFinder.isTypeVariableHasProperConstraint(this, coneTypeVariableTypeConstructor)) {
            return@withTypeVariablesThatAreCountedAsProperTypes null
        }

        resultTypeCallback()
    }

    private fun is21Mode(): Boolean =
        inferenceComponents.session.languageVersionSettings.supportsFeature(LanguageFeature.PCLAEnhancementsIn21)

    /**
     * This function returns true only when it's safe & sound to analyze and complete the candidate outside the PCLA context,
     * i.e., independently of outer CS.
     *
     * That might be some plain variable accesses that do not contain type variables or regular function calls with only trivial arguments.
     *
     * The basic purpose of that function is performance enhancement because resolving all the calls inside PCLA lambda in the outer context
     *  might be too much.
     *
     * Mostly, that means that this function might always return false and it should be correct.
     * TODO: Currently, making it always returning "false" leads to few test failures
     * TODO: due to some corner cases like annotations calls (KT-65465)
     */
    private fun Candidate.mightBeAnalyzedAndCompletedIndependently(returnTypeCalculator: ReturnTypeCalculator): Boolean {
        when (callInfo.resolutionMode) {
            // Currently, we handle delegates specifically, not completing them even if they are trivial function calls
            // Thus they are being resolved in the context of outer CS
            is ResolutionMode.Delegate -> return false
            is ResolutionMode.WithExpectedType -> when {
                // For assignments like myVarContainingTV = SomeCallWithNonTrivialInference(...)
                // We should integrate even simple calls into the PCLA tree, too
                callInfo.resolutionMode.expectedType.containsNotFixedTypeVariables() -> return false
            }
            is ResolutionMode.WithStatus ->
                error("$this call should not be analyzed in ${callInfo.resolutionMode}")

            is ResolutionMode.AssignmentLValue,
            is ResolutionMode.ContextDependent,
            is ResolutionMode.ContextIndependent,
            is ResolutionMode.ReceiverResolution,
            -> {
                // Regular cases, just continue execution.
                // Enumerating all the cases just to make sure we don't forget to handle some mode.
            }
        }

        val callSite = callInfo.callSite
        // Annotation calls and collection literals (allowed only inside annotations)
        // should be completed independently since that can't somehow affect PCLA
        if (callSite is FirAnnotationCall || callSite is FirArrayLiteral) return true

        // I'd say that this might be an assertion, but let's do an early return
        if (callSite !is FirResolvable && callSite !is FirVariableAssignment) return false

        // We can't analyze independently the calls which have postponed receivers
        // Even if the calls themselves are trivial
        if (dispatchReceiver?.expression?.isReceiverPostponed() == true) return false
        if (givenExtensionReceiverOptions.any { it.expression.isReceiverPostponed() }) return false
        // At the step of candidate's system creation, there are no chosen context receiver values, yet
        // (see org.jetbrains.kotlin.fir.resolve.calls.CheckContextArguments)
        // Thus, we just postpone everything with symbols requiring some context receivers
        if ((symbol as? FirCallableSymbol)?.resolvedContextParameters?.isNotEmpty() == true) return false

        // Accesses to local variables or local functions which return types contain not fixed TVs
        val returnType = (symbol as? FirCallableSymbol)?.let(returnTypeCalculator::tryCalculateReturnType)
        if (returnType?.coneType?.containsNotFixedTypeVariables() == true) return false

        // Now, we've got some sort of call/variable access/callable reference/synthetic call (see hierarchy of FirResolvable)
        // It has regular independent receivers and trivial return type
        // The only thing we need to check if it has only trivial arguments
        if (callInfo.arguments.any { !it.isTrivialArgument() }) return false

        return true
    }

    private fun FirExpression.isTrivialArgument(): Boolean =
        when (this) {
            // Callable references might be unresolved at this stage, so obtaining `resolvedType` would lead to exceptions
            // Anyway, they should lead to integrated resolution of containing call
            is FirCallableReferenceAccess -> false

            is FirResolvable -> when (val candidate = candidate()) {
                null -> !resolvedType.containsNotFixedTypeVariables()
                else -> !candidate.usedOuterCs
            }

            is FirWrappedExpression -> expression.isTrivialArgument()
            is FirSamConversionExpression -> expression.isTrivialArgument()
            is FirSmartCastExpression -> originalExpression.isTrivialArgument()

            is FirCall -> argumentList.arguments.all { it.isTrivialArgument() }

            is FirBooleanOperatorExpression -> leftOperand.isTrivialArgument() && rightOperand.isTrivialArgument()
            is FirComparisonExpression -> compareToCall.isTrivialArgument()

            is FirCheckedSafeCallSubject -> originalReceiverRef.value.isTrivialArgument()
            is FirSafeCallExpression -> receiver.isTrivialArgument() && (selector as? FirExpression)?.isTrivialArgument() == true
            is FirVarargArgumentsExpression -> arguments.all { it.isTrivialArgument() }

            is FirLiteralExpression, is FirResolvedQualifier, is FirResolvedReifiedParameterReference -> true

            // Be default, we consider all the unknown cases as unsafe to resolve independently
            else -> false
        }

    private fun FirExpression.isReceiverPostponed(): Boolean {
        return when {
            resolvedType.containsNotFixedTypeVariables() -> true
            (this as? FirResolvable)?.candidate()?.usedOuterCs == true -> true
            else -> false
        }
    }

    private fun ConeKotlinType.containsNotFixedTypeVariables(): Boolean =
        contains {
            // TODO: Investigate why using `notFixedTypeVariables` instead of `allTypeVariables` leads to failure of the test (KT-64861)
            // org.jetbrains.kotlin.test.runners.codegen.FirPsiBlackBoxCodegenTestGenerated.BuilderInference.OneParameter.OneTypeVariable.
            // OneTypeInfoOrigin.SourceSinkFeedContexts.testThroughDelegatedLocalVariableYieldCase
            it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.allTypeVariables
        }

    override fun addSubtypeConstraintIfCompatible(lowerType: ConeKotlinType, upperType: ConeKotlinType, element: FirElement) {
        currentCommonSystem.addSubtypeConstraintIfCompatible(lowerType, upperType, ConeExpectedTypeConstraintPosition)
    }
}

class FirTypeVariablesAfterPCLATransformer(private val substitutor: ConeSubstitutor) : FirDefaultTransformer<Nothing?>() {

    override fun <E : FirElement> transformElement(element: E, data: Nothing?): E {
        // All resolvable nodes should be implemented separately to cover substitution of receivers in the candidate
        if (element is FirResolvable) {
            element.candidate()?.let { processCandidate(it) }
        }

        // Since FirExpressions don't have typeRefs, they need to be updated separately.
        // FirAnonymousFunctionExpression doesn't support replacing the type
        // since it delegates the getter to the underlying FirAnonymousFunction.
        // WrappedArgumentExpression delegates the type to the inner expression and doesn't need to be updated.
        if (element is FirExpression && element !is FirAnonymousFunctionExpression && element !is FirWrappedArgumentExpression) {
            element.resolvedType
                .let(substitutor::substituteOrNull)
                ?.let { element.replaceConeTypeOrNull(it) }
        }

        @Suppress("UNCHECKED_CAST")
        return element.transformChildren(this, data = null) as E
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): FirTypeRef =
        substitutor.substituteOrNull(resolvedTypeRef.coneType)?.let {
            resolvedTypeRef.withReplacedConeType(it)
        } ?: resolvedTypeRef

    /*
     * We should manually update all receivers in the all not completed candidates, because not all calls with candidates
     *   contained in partiallyResolvedCalls and candidate stores not receiver values, which are updated, (TODO: remove this comment after removal of updating values)
     *   and receivers of candidates are not direct FIR children of calls, so they won't be visited during regular transformChildren
     */
    private fun processCandidate(candidate: Candidate) {
        candidate.dispatchReceiver = ConeResolutionAtom.createRawAtom(
            candidate.dispatchReceiver?.expression?.transform(this, data = null)
        )
        candidate.chosenExtensionReceiver = ConeResolutionAtom.createRawAtom(
            candidate.chosenExtensionReceiver?.expression?.transform(this, data = null)
        )
        candidate.contextArguments = candidate.contextArguments?.map {
            ConeResolutionAtom.createRawAtom(it.expression.transform(this, data = null))
        }
    }
}

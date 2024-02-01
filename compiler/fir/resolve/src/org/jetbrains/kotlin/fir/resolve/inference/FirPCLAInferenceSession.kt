/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.CallKind
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.calls.processConstraintStorageFromExpression
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirDefaultTransformer
import org.jetbrains.kotlin.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.defaultType


class FirPCLAInferenceSession(
    private val outerCandidate: Candidate,
    private val inferenceComponents: InferenceComponents,
    private val returnTypeCalculator: ReturnTypeCalculator,
) : FirInferenceSession() {

    var currentCommonSystem = prepareSharedBaseSystem(outerCandidate.system, inferenceComponents)
        private set

    override fun baseConstraintStorageForCandidate(candidate: Candidate): ConstraintStorage? {
        if (candidate.mightBeAnalyzedAndCompletedIndependently()) return null

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
    ) where T : FirResolvable, T : FirStatement {
        if (call is FirExpression) {
            call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)
        }

        val candidate = call.candidate()
        if (candidate?.usedOuterCs != true) return

        // Integrating back would happen at FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot
        // after all other delegation-related calls are being analyzed
        if (resolutionMode == ResolutionMode.Delegate) return

        currentCommonSystem.replaceContentWith(candidate.system.currentStorage())

        if (completionMode == ConstraintSystemCompletionMode.PCLA_POSTPONED_CALL) {
            outerCandidate.postponedPCLACalls += call
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

    override fun <T> runCallableReferenceResolution(candidate: Candidate, block: () -> T): T {
        candidate.system.apply {
            // It's necessary because otherwise when we create CS for a child, it would simplify constraints
            // (see 3rd constructor of MutableVariableWithConstraints)
            // and merging it back might become a problem for transaction logic because the latter literally remembers
            // the number of constraints for each variable and then restores it back.
            // But since the constraints are simplified in the child, their number might become even fewer, leading to incorrect behavior
            // or runtime exceptions.
            // See callableReferenceAsArgumentForTransaction.kt test data
            notFixedTypeVariables.values.forEach { it.runConstraintsSimplification() }
        }
        return runWithSpecifiedCurrentCommonSystem(candidate.system, block)
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

    fun integrateChildSession(
        childCalls: Collection<FirStatement>,
        childStorage: ConstraintStorage,
        onCompletionResultsWriting: (ConeSubstitutor) -> Unit,
    ) {
        outerCandidate.postponedPCLACalls += childCalls
        currentCommonSystem.addOtherSystem(childStorage)
        outerCandidate.onCompletionResultsWritingCallbacks += onCompletionResultsWriting
    }

    private fun FirExpression.updateReturnTypeWithCurrentSubstitutor(
        resolutionMode: ResolutionMode,
    ) {
        val additionalBindings = mutableMapOf<TypeConstructorMarker, ConeKotlinType>()
        val system = (this as? FirResolvable)?.candidate()?.system ?: currentCommonSystem

        if (resolutionMode is ResolutionMode.ReceiverResolution) {
            fixVariablesForMemberScope(resolvedType, system)?.let { additionalBindings += it }
        }

        val substitutor = system.buildCurrentSubstitutor(additionalBindings) as ConeSubstitutor
        val updatedType = substitutor.substituteOrNull(resolvedType)

        if (updatedType != null) {
            replaceConeTypeOrNull(updatedType)
        }
    }

    fun fixVariablesForMemberScope(
        type: ConeKotlinType,
        myCs: NewConstraintSystemImpl,
    ): Pair<ConeTypeVariableTypeConstructor, ConeKotlinType>? {
        return when (type) {
            is ConeFlexibleType -> fixVariablesForMemberScope(type.lowerBound, myCs)
            is ConeDefinitelyNotNullType -> fixVariablesForMemberScope(type.original, myCs)
            is ConeTypeVariableType -> fixVariablesForMemberScope(type, myCs)
            else -> null
        }
    }

    private fun fixVariablesForMemberScope(
        type: ConeTypeVariableType,
        myCs: NewConstraintSystemImpl,
    ): Pair<ConeTypeVariableTypeConstructor, ConeKotlinType>? {
        val coneTypeVariableTypeConstructor = type.typeConstructor

        require(coneTypeVariableTypeConstructor in myCs.allTypeVariables) {
            "$coneTypeVariableTypeConstructor not found"
        }

        if (coneTypeVariableTypeConstructor in myCs.outerTypeVariables.orEmpty()) return null

        val variableWithConstraints = myCs.notFixedTypeVariables[coneTypeVariableTypeConstructor] ?: return null
        val c = myCs.getBuilder()

        val resultType = c.run {
            c.withTypeVariablesThatAreCountedAsProperTypes(c.outerTypeVariables.orEmpty()) {
                if (!inferenceComponents.variableFixationFinder.isTypeVariableHasProperConstraint(c, coneTypeVariableTypeConstructor)) {
                    return@withTypeVariablesThatAreCountedAsProperTypes null
                }
                inferenceComponents.resultTypeResolver.findResultType(
                    c,
                    variableWithConstraints,
                    TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
                ) as ConeKotlinType
            }
        } ?: return null
        val variable = variableWithConstraints.typeVariable
        // TODO: Consider using different position (KT-64860)
        c.addEqualityConstraint(variable.defaultType(c), resultType, ConeFixVariableConstraintPosition(variable))

        return Pair(coneTypeVariableTypeConstructor, resultType)
    }

    private fun FirExpression.doesArgumentLeadToCallPostponement(): Boolean {
        if (this is FirWrappedArgumentExpression) return expression.doesArgumentLeadToCallPostponement()

        if (doesArgumentUseOuterCS()) return true
        if (isQualifiedAccessContainingTypeVariables()) return true

        // Postponed atoms
        if (this is FirAnonymousFunctionExpression) return true
        if (this is FirCallableReferenceAccess) return true
        if (this is FirAnonymousObjectExpression) return true

        if (this is FirStringConcatenationCall && this.arguments.any { it.doesArgumentLeadToCallPostponement() }) return true

        return false
    }

    private fun Candidate.needsToBePostponed(): Boolean {
        if (dispatchReceiver?.isReceiverPostponed() == true) return true
        if (givenExtensionReceiverOptions.any { it.isReceiverPostponed() }) return true

        if (callInfo.arguments.any { it.doesArgumentLeadToCallPostponement() }) return true

        if (callInfo.callKind == CallKind.VariableAccess) {
            val returnType = (symbol as? FirVariableSymbol)?.let(returnTypeCalculator::tryCalculateReturnType)
            if (returnType?.type?.containsNotFixedTypeVariables() == true) return true
        }

        if (callInfo.resolutionMode is ResolutionMode.Delegate) return true

        // For assignments like myVarContainingTV = SomeCallWithNonTrivialInference(...)
        // We should integrate the call into the PCLA tree, too
        if ((callInfo.resolutionMode as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.type?.containsNotFixedTypeVariables() == true) {
            return true
        }

        // Synthetic calls with blocks work like lambdas
        if (callInfo.callKind == CallKind.SyntheticSelect) return true

        return false
    }

    private fun FirExpression.isTrivialArgument(): Boolean =
        when (this) {
            is FirWrappedExpression -> expression.isTrivialArgument()
            is FirSamConversionExpression -> expression.isTrivialArgument()
            is FirSmartCastExpression -> originalExpression.isTrivialArgument()

            is FirCallableReferenceAccess -> false

            is FirResolvable -> when (val candidate = candidate()) {
                null -> !resolvedType.containsNotFixedTypeVariables()
                else -> !candidate.usedOuterCs
            }
            is FirCall -> argumentList.arguments.all { it.isTrivialArgument() }

            is FirBinaryLogicExpression -> leftOperand.isTrivialArgument() && rightOperand.isTrivialArgument()
            is FirComparisonExpression -> compareToCall.isTrivialArgument()

            is FirCheckedSafeCallSubject -> originalReceiverRef.value.isTrivialArgument()
            is FirSafeCallExpression -> receiver.isTrivialArgument() && (selector as? FirExpression)?.isTrivialArgument() == true
            is FirVarargArgumentsExpression -> arguments.all { it.isTrivialArgument() }

            is FirLiteralExpression<*>, is FirResolvedQualifier, is FirResolvedReifiedParameterReference -> true
            else -> false
        }

    private fun Candidate.mightBeAnalyzedAndCompletedIndependently(): Boolean {
        when (callInfo.resolutionMode) {
            // Currently, we handle delegates specifically, not completing them even if they are trivial function calls
            // Thus they are being resolved in the context of outer CS
            is ResolutionMode.Delegate -> return false
            is ResolutionMode.WithExpectedType -> when {
                // For assignments like myVarContainingTV = SomeCallWithNonTrivialInference(...)
                // We should integrate even simple calls into the PCLA tree, too
                callInfo.resolutionMode.expectedTypeRef.type.containsNotFixedTypeVariables() -> return false
            }
            is ResolutionMode.WithStatus, is ResolutionMode.LambdaResolution ->
                error("$this call should not be analyzed in ${callInfo.resolutionMode}")

            is ResolutionMode.AssignmentLValue,
            is ResolutionMode.ContextDependent,
            is ResolutionMode.ContextIndependent,
            is ResolutionMode.ReceiverResolution,
            -> {
                // Just do nothing, enumerating all the cases just to make sure we don't forget to handle some mode
            }
        }

        // I'd say that this might be an assertion, but let's do an early return
        // TODO: try assertion or proof by type
        if (callInfo.callSite !is FirResolvable && callInfo.callSite !is FirVariableAssignment) return false

        if (callInfo.callSite is FirAnnotationCall) return true

        // We can't analyze independently the calls which have postponed receivers
        // Even if the calls themselves are trivial
        if (dispatchReceiver?.isReceiverPostponed() == true) return false
        if (givenExtensionReceiverOptions.any { it.isReceiverPostponed() }) return false

        val returnType = (symbol as? FirCallableSymbol)?.let(returnTypeCalculator::tryCalculateReturnType)
        if (returnType?.type?.containsNotFixedTypeVariables() == true) return false

        // Now, we've got some sort of call/variable access/callable reference/synthetic call (see hierarchy of FirResolvable)
        // It has regular independent receivers and trivial return type
        // The only thing we need to check if it has only trivial arguments
        if (callInfo.arguments.any { !it.isTrivialArgument() }) return false

        return true
    }

    private fun FirExpression.doesArgumentUseOuterCS(): Boolean {
        var result = false
        processConstraintStorageFromExpression(this) {
            if (it.usesOuterCs) {
                result = true
            }
        }

        return result
    }

    private fun FirExpression.isReceiverPostponed(): Boolean {
        if (resolvedType.containsNotFixedTypeVariables()) return true
        if ((this as? FirResolvable)?.candidate()?.usedOuterCs == true) return true

        return false
    }

    private fun FirExpression.isQualifiedAccessContainingTypeVariables(): Boolean {
        if (this is FirNamedArgumentExpression) return expression.isQualifiedAccessContainingTypeVariables()

        if (!isQualifiedAccessOrSmartCastOnIt()) return false
        if (this is FirCallableReferenceAccess) return false

        return resolvedType.containsNotFixedTypeVariables() && (this as? FirResolvable)?.candidate() == null
    }

    private fun FirExpression.isQualifiedAccessOrSmartCastOnIt(): Boolean = when (this) {
        is FirQualifiedAccessExpression -> true
        is FirSmartCastExpression -> originalExpression.isQualifiedAccessOrSmartCastOnIt()
        else -> false
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
        if (element is FirExpression && element !is FirAnonymousFunctionExpression) {
            // TODO Check why some expressions have unresolved type in builder inference session KT-61835
            @OptIn(UnresolvedExpressionTypeAccess::class)
            element.coneTypeOrNull
                ?.let(substitutor::substituteOrNull)
                ?.let { element.replaceConeTypeOrNull(it) }
        }

        @Suppress("UNCHECKED_CAST")
        return element.transformChildren(this, data = null) as E
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: Nothing?): FirTypeRef =
        substitutor.substituteOrNull(resolvedTypeRef.type)?.let {
            resolvedTypeRef.withReplacedConeType(it)
        } ?: resolvedTypeRef

    /*
     * We should manually update all receivers in the all not completed candidates, because not all calls with candidates
     *   contained in partiallyResolvedCalls and candidate stores not receiver values, which are updated, (TODO: remove this comment after removal of updating values)
     *   and receivers of candidates are not direct FIR children of calls, so they won't be visited during regular transformChildren
     */
    private fun processCandidate(candidate: Candidate) {
        candidate.dispatchReceiver = candidate.dispatchReceiver?.transform(this, data = null)
        candidate.chosenExtensionReceiver = candidate.chosenExtensionReceiver?.transform(this, data = null)
        candidate.contextReceiverArguments = candidate.contextReceiverArguments?.map { it.transform(this, data = null) }
    }
}

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
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
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
) : FirInferenceSession() {

    var currentCommonSystem = prepareSharedBaseSystem(outerCandidate.system, inferenceComponents)
        private set

    private val qualifiedAccessesToProcess = mutableSetOf<FirExpression>()

    override fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val candidate = call.candidate() ?: return false
        return candidate.usedOuterCs /*&& candidate.postponedAtoms.isEmpty()*/ /*call.candidate()?.usedOuterCs == true*/
    }

    // Should write completion results
    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement =
        call.candidate()?.usedOuterCs != true

    override fun handleQualifiedAccess(qualifiedAccessExpression: FirExpression, data: ResolutionMode) {
        // Callable references are being processed together with containing its call
        if (qualifiedAccessExpression is FirCallableReferenceAccess) return

        if (qualifiedAccessExpression.resolvedType.containsNotFixedTypeVariables() && (qualifiedAccessExpression as? FirResolvable)?.candidate() == null) {
            qualifiedAccessesToProcess.add(qualifiedAccessExpression)

            if (qualifiedAccessExpression is FirSmartCastExpression) {
                handleQualifiedAccess(qualifiedAccessExpression.originalExpression, data)
            }

            qualifiedAccessExpression.updateReturnTypeWithCurrentSubstitutor(data)
        }
    }

    override fun <T> runLambdaCompletion(candidate: Candidate, block: () -> T): T {
        return runWithSpecifiedCurrentCommonSystem(candidate.system, block)
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

    override fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode
    ) where T : FirResolvable, T : FirStatement {
        if (call is FirExpression) {
            call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)
        }

        val candidate = call.candidate() ?: return
        if (!candidate.isNotTrivial()) return

        // Integrating back would happen at FirDelegatedPropertyInferenceSession.completeSessionOrPostponeIfNonRoot
        // after all other delegation-related calls are being analyzed
        if (resolutionMode == ResolutionMode.ContextDependent.Delegate) return

        currentCommonSystem.replaceContentWith(candidate.system.currentStorage())

        if (!resolutionMode.isReceiverOrTopLevel) return

        outerCandidate.postponedPCLACalls += call
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

        resolutionMode.hashCode()
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
        // outerYield_1_3.kt
        // org.jetbrains.kotlin.test.runners.FirPsiOldFrontendDiagnosticsTestGenerated.TestsWithStdLib.Coroutines.RestrictSuspension.testOuterYield_1_3
        //if ("".hashCode() == 0) return null
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
        // TODO: Position
        c.addEqualityConstraint(variable.defaultType(c), resultType, ConeFixVariableConstraintPosition(variable))

        return Pair(coneTypeVariableTypeConstructor, resultType)
    }

    private fun Candidate.isNotTrivial(): Boolean =
        usedOuterCs

    override fun baseConstraintStorageForCandidate(candidate: Candidate): ConstraintStorage? {
        if (candidate.needsToBePostponed()) return currentCommonSystem.currentStorage()
        if (candidate.callInfo.arguments.any { it.isLambda() }) return currentCommonSystem.currentStorage()
        // TODO: context receivers
        return null
    }

    private fun FirExpression.isLambda(): Boolean {
        if (this is FirWrappedArgumentExpression) return expression.isLambda()
        if (this is FirAnonymousFunctionExpression) return true
        if (this is FirCallableReferenceAccess) return true
        if (this is FirAnonymousObjectExpression) return true
        return false
    }

    private fun Candidate.needsToBePostponed(): Boolean {
        // For invokeExtension calls
        if (callInfo.candidateForCommonInvokeReceiver != null) {
            callInfo.arguments.getOrNull(0)
                ?.takeIf { it is FirQualifiedAccessExpression && it !in qualifiedAccessesToProcess }
                ?.let { handleQualifiedAccess(it, ResolutionMode.ContextDependent) }
        }

        if (dispatchReceiver?.isReceiverPostponed() == true) return true
        if (givenExtensionReceiverOptions.any { it.isReceiverPostponed() }) return true
        if (callInfo.arguments.any { it.isArgumentAmongPostponedQualifiedAccess() }) return true
        if (callInfo.resolutionMode is ResolutionMode.ContextDependent.Delegate) return true
        // For assignments
        if ((callInfo.resolutionMode as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.type?.containsNotFixedTypeVariables() == true) {
            return true
        }
        // Synthetic calls with blocks work like lambdas
        if (callInfo.callKind == CallKind.SyntheticSelect) return true

        return false
    }

    private fun FirExpression.isReceiverPostponed(): Boolean {
        if (resolvedType.containsNotFixedTypeVariables()) return true
        if ((this as? FirResolvable)?.candidate()?.usedOuterCs == true) return true

        return false
    }

    private fun FirExpression.isArgumentAmongPostponedQualifiedAccess(): Boolean {
        if (this is FirNamedArgumentExpression) return expression.isArgumentAmongPostponedQualifiedAccess()
        return this in qualifiedAccessesToProcess
    }

    private fun ConeKotlinType.containsNotFixedTypeVariables(): Boolean =
        contains {
            it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.allTypeVariables
        }

    override fun addSubtypeConstraintIfCompatible(lowerType: ConeKotlinType, upperType: ConeKotlinType, element: FirElement) {
        currentCommonSystem.addSubtypeConstraintIfCompatible(lowerType, upperType, ConeExpectedTypeConstraintPosition(element))
    }

    // TODO: Get rid of them

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}
}

class FirStubTypeTransformer(private val substitutor: ConeSubstitutor) : FirDefaultTransformer<Nothing?>() {

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

    override fun transformTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: Nothing?): FirStatement {
        if (typeOperatorCall.argument.resolvedType is ConeStubType) {
            typeOperatorCall.replaceArgFromStubType(true)
        }
        return super.transformTypeOperatorCall(typeOperatorCall, data)
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

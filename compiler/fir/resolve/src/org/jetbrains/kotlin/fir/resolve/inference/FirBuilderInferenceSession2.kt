/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.CallKind
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.defaultType



class FirBuilderInferenceSession2(
    private val outerCandidate: Candidate,
    private val inferenceComponents: InferenceComponents,
) : FirInferenceSession() {

    var currentCommonSystem = outerCandidate.system

    private val initialOuterTypeVariables = outerCandidate.system.currentStorage().allTypeVariables.keys
    private val qualifiedAccessesToProcess = mutableSetOf<FirExpression>()

    override fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val candidate = call.candidate() ?: return false
        return candidate.usedOuterCs /*&& candidate.postponedAtoms.isEmpty()*/ /*call.candidate()?.usedOuterCs == true*/
    }

    // Should write completion results
    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement =
        call.candidate()?.usedOuterCs != true

    override fun handleQualifiedAccess(qualifiedAccessExpression: FirExpression, data: ResolutionMode) {
        if (qualifiedAccessExpression.resolvedType.containsNotFixedTypeVariables() && (qualifiedAccessExpression as? FirResolvable)?.candidate() == null) {
            qualifiedAccessesToProcess.add(qualifiedAccessExpression)
            outerCandidate.postponedAccesses += qualifiedAccessExpression

            if (qualifiedAccessExpression is FirSmartCastExpression) {
                handleQualifiedAccess(qualifiedAccessExpression.originalExpression, data)
            }

            (data as? ResolutionMode.ContextIndependent.ForDeclaration)?.declaration?.let(outerCandidate.updateDeclarations::add)

            qualifiedAccessExpression.updateReturnTypeWithCurrentSubstitutor(data)
        }
    }

    override fun <T> runLambdaCompletion(candidate: Candidate, block: () -> T): T {
        val previous = currentCommonSystem
        return try {
            currentCommonSystem = candidate.system
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
        if (completionMode == ConstraintSystemCompletionMode.PARTIAL && !resolutionMode.isReceiverOrTopLevel) return

        if (call is FirExpression) {
            call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)
        }

        if (!resolutionMode.isReceiverOrTopLevel) return

        val candidate = call.candidate() ?: return
        if (!candidate.isNotTrivial()) return

        outerCandidate.postponedCalls += call

        (resolutionMode as? ResolutionMode.ContextIndependent.ForDeclaration)?.declaration?.let(outerCandidate.updateDeclarations::add)
        currentCommonSystem.addOtherSystem(candidate.system.currentStorage(), isAddingOuter = false)
    }

    fun integrateChildSession(
        childCalls: Collection<FirStatement>,
        childStorage: ConstraintStorage,
        afterCompletion: (ConeSubstitutor) -> Unit,
    ) {
        outerCandidate.postponedCalls += childCalls
        currentCommonSystem.addOtherSystem(childStorage)
        outerCandidate.callbacks += afterCompletion
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
        // outerYield_1_3.kt
        // org.jetbrains.kotlin.test.runners.FirPsiOldFrontendDiagnosticsTestGenerated.TestsWithStdLib.Coroutines.RestrictSuspension.testOuterYield_1_3
        //if ("".hashCode() == 0) return null
        val coneTypeVariableTypeConstructor = type.lookupTag

        require(coneTypeVariableTypeConstructor in myCs.allTypeVariables) {
            "$coneTypeVariableTypeConstructor not found"
        }

        val variableWithConstraints = myCs.notFixedTypeVariables[coneTypeVariableTypeConstructor] ?: return null
        val c = myCs.getBuilder()
        val resultType = c.run {
            withTypeVariablesThatAreNotCountedAsProperTypes(myCs.allTypeVariables.keys - initialOuterTypeVariables) {
                inferenceComponents.resultTypeResolver.findResultType(
                    c,
                    variableWithConstraints,
                    TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
                ) as ConeKotlinType
            }
        }
        val variable = variableWithConstraints.typeVariable
        // TODO: Position
        c.addEqualityConstraint(variable.defaultType(c), resultType, ConeFixVariableConstraintPosition(variable))

        return Pair(coneTypeVariableTypeConstructor, resultType)
    }

    private fun Candidate.isNotTrivial(): Boolean =
        usedOuterCs

    override fun outerCSForCandidate(candidate: Candidate): ConstraintStorage? {
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

        if (dispatchReceiver?.resolvedType?.containsNotFixedTypeVariables() == true) return true
        if (givenExtensionReceiverOptions.any { it.resolvedType.containsNotFixedTypeVariables() }) return true
        if (callInfo.arguments.any { it in qualifiedAccessesToProcess }) return true
        if (callInfo.isDelegateExpression) return true
        // Synthetic calls with blocks work like lambdas
        if (callInfo.callKind == CallKind.SyntheticSelect) return true

        return false
    }

    private fun ConeKotlinType.containsNotFixedTypeVariables(): Boolean =
        contains {
            it is ConeTypeVariableType && it.typeConstructor in currentCommonSystem.allTypeVariables
        }

    // TODO: Get rid of them

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}
}
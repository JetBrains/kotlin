/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.types.model.defaultType

class FirBuilderInferenceSession2(
    val outerCandidate: Candidate,
    private val inferenceComponents: InferenceComponents,
) : FirInferenceSession() {

    private val outerCS: ConstraintStorage = outerCandidate.system.currentStorage()
    private val qualifiedAccessesToProcess = mutableSetOf<FirExpression>()

    override fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        val candidate = call.candidate() ?: return false
        return candidate.usedOuterCs /*&& candidate.postponedAtoms.isEmpty()*/ /*call.candidate()?.usedOuterCs == true*/
    }

    // Should write completion results
    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement =
        call.candidate()?.usedOuterCs != true

    override fun handleQualifiedAccess(qualifiedAccessExpression: FirExpression, data: ResolutionMode) {
        if (qualifiedAccessExpression.resultType.containsNotFixedTypeVariables() && (qualifiedAccessExpression as? FirResolvable)?.candidate() == null) {
            qualifiedAccessesToProcess.add(qualifiedAccessExpression)
            outerCandidate.postponedAccesses += qualifiedAccessExpression

            if (qualifiedAccessExpression is FirSmartCastExpression) {
                handleQualifiedAccess(qualifiedAccessExpression.originalExpression, data)
            }

            (data as? ResolutionMode.ContextIndependent.ForDeclaration)?.declaration?.let(outerCandidate.updateDeclarations::add)

            qualifiedAccessExpression.updateReturnTypeWithCurrentSubstitutor(data)
        }
    }

    override fun <T> processPartiallyResolvedCall(call: T, resolutionMode: ResolutionMode) where T : FirResolvable, T : FirStatement {
        if (!resolutionMode.forceFullCompletion) return

        val candidate = call.candidate() ?: return
        if (!candidate.isNotTrivial()) return

        outerCandidate.postponedCalls += call

        (resolutionMode as? ResolutionMode.ContextIndependent.ForDeclaration)?.declaration?.let(outerCandidate.updateDeclarations::add)
        outerCandidate.system.addOtherSystem(candidate.system.currentStorage())

        if (call is FirExpression) {
            call.updateReturnTypeWithCurrentSubstitutor(resolutionMode)
        }
    }

    private fun FirExpression.updateReturnTypeWithCurrentSubstitutor(
        resolutionMode: ResolutionMode,
    ) {
        val additionalBindings = mutableMapOf<TypeConstructorMarker, ConeKotlinType>()
        if (resolutionMode == ResolutionMode.ReceiverResolution) {
            fixVariablesForMemberScope(resultType.coneType, outerCandidate)?.let { additionalBindings += it }
        }

        val updatedType =
            (outerCandidate.system.buildCurrentSubstitutor(additionalBindings) as ConeSubstitutor).substituteOrNull(typeRef.coneType)
        if (updatedType != null) {
            resultType = resultType.withReplacedConeType(updatedType)
        }
    }

    fun fixVariablesForMemberScope(
        type: ConeKotlinType,
        outerCandidate: Candidate,
    ): Pair<ConeTypeVariableTypeConstructor, ConeKotlinType>? {
        return when (type) {
            is ConeFlexibleType -> fixVariablesForMemberScope(type.lowerBound, outerCandidate)
            is ConeDefinitelyNotNullType -> fixVariablesForMemberScope(type.original, outerCandidate)
            is ConeTypeVariableType -> fixVariablesForMemberScope(type, outerCandidate)
            else -> null
        }
    }

    private fun fixVariablesForMemberScope(
        type: ConeTypeVariableType,
        myCandidate: Candidate,
    ): Pair<ConeTypeVariableTypeConstructor, ConeKotlinType>? {
        val coneTypeVariableTypeConstructor = type.lookupTag
        val myCs = myCandidate.system

        require(coneTypeVariableTypeConstructor in myCs.allTypeVariables) {
            "$coneTypeVariableTypeConstructor not found"
        }

        val variableWithConstraints = myCs.notFixedTypeVariables[coneTypeVariableTypeConstructor] ?: return null
        val c = myCandidate.csBuilder
        val resultType = inferenceComponents.resultTypeResolver.findResultType(
            c,
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN
        ) as ConeKotlinType
        val variable = variableWithConstraints.typeVariable
        // TODO: Position
        c.addEqualityConstraint(variable.defaultType(c), resultType, ConeFixVariableConstraintPosition(variable))

        return Pair(coneTypeVariableTypeConstructor, resultType)
    }

    private fun Candidate.isNotTrivial(): Boolean =
        usedOuterCs || postponedAtoms.isNotEmpty()

    override fun outerCSForCandidate(candidate: Candidate): ConstraintStorage? {
        if (candidate.needsToBePostponed()) return outerCS
        if (candidate.callInfo.arguments.any { it.isLambda() }) return outerCS
        // TODO: context receivers
        return null
    }

    private fun FirExpression.isLambda(): Boolean {
        if (this is FirWrappedArgumentExpression) return expression.isLambda()
        if (this is FirAnonymousFunctionExpression) return true
        return false
    }

    private fun Candidate.needsToBePostponed(): Boolean {
        if (dispatchReceiver?.typeRef?.containsNotFixedTypeVariables() == true) return true
        if (givenExtensionReceiverOptions.any { it.typeRef.containsNotFixedTypeVariables() }) return true
        if (callInfo.arguments.any { it in qualifiedAccessesToProcess }) return true

        return false
    }

    private fun FirTypeRef.containsNotFixedTypeVariables(): Boolean =
        coneTypeOrNull?.contains {
            it is ConeTypeVariableType && it.lookupTag in outerCandidate.system.allTypeVariables
        } == true

    // TODO: Get rid of them

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}
}
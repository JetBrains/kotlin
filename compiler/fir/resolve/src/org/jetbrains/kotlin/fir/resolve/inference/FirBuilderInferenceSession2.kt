/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.resultType
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage

class FirBuilderInferenceSession2(
    private val outerCandidate: Candidate,
) : FirInferenceSession() {

    private val outerCS: ConstraintStorage = outerCandidate.system.currentStorage()
    private val qualifiedAccessesToProcess = mutableSetOf<FirExpression>()

    override fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return call.candidate()?.usedOuterCs == true
    }

    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement =
        true

    override fun handleQualifiedAccess(qualifiedAccessExpression: FirExpression, data: ResolutionMode) {
        if (qualifiedAccessExpression.resultType.containsNotFixedTypeVariables() && (qualifiedAccessExpression as? FirResolvable)?.candidate() == null) {
            qualifiedAccessesToProcess.add(qualifiedAccessExpression)
            outerCandidate.postponedAccesses += qualifiedAccessExpression

            if (qualifiedAccessExpression is FirSmartCastExpression) {
                handleQualifiedAccess(qualifiedAccessExpression.originalExpression, data)
            }

            (data as? ResolutionMode.ContextIndependent.ForDeclaration)?.declaration?.let(outerCandidate.updateDeclarations::add)
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
            val updatedType = (outerCandidate.system.buildCurrentSubstitutor() as ConeSubstitutor).substituteOrNull(call.typeRef.coneType)
            if (updatedType != null) {
                call.resultType = call.resultType.withReplacedConeType(updatedType)
            }
        }
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
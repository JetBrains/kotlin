/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.fir.types.ConeTypeVariableType
import org.jetbrains.kotlin.fir.types.coneTypeOrNull
import org.jetbrains.kotlin.fir.types.contains
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage

class FirBuilderInferenceSession2(
    private val outerCandidate: Candidate,
) : FirInferenceSession() {

    private val outerCS: ConstraintStorage = outerCandidate.system.currentStorage()

    override fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return call.candidate()?.isNotTrivial() == true
    }

    override fun <T> processPartiallyResolvedCall(call: T, resolutionMode: ResolutionMode) where T : FirResolvable, T : FirStatement {
        if (!resolutionMode.forceFullCompletion) return

        val candidate = call.candidate() ?: return
        if (!candidate.isNotTrivial()) return

        outerCandidate.postponedCalls += call

        (resolutionMode as? ResolutionMode.ContextIndependent.ForDeclaration)?.declaration?.let(outerCandidate.updateDeclarations::add)
        outerCandidate.system.addOtherSystem(candidate.system.currentStorage())
    }

    private fun Candidate.isNotTrivial(): Boolean =
        usedOuterCs || postponedAtoms.isNotEmpty()

    override fun outerCSForCandidate(candidate: Candidate): ConstraintStorage? {
        if (candidate.dispatchReceiver?.containsNotFixedTypeVariables() == true) return outerCS
        if (candidate.givenExtensionReceiverOptions.any { it.containsNotFixedTypeVariables() }) return outerCS

        // TODO: context receivers
        return null
    }

    private fun FirExpression.containsNotFixedTypeVariables(): Boolean =
        typeRef.coneTypeOrNull?.contains {
            it is ConeTypeVariableType && it.lookupTag in outerCandidate.system.notFixedTypeVariables
        } == true

    // TODO: Get rid of them
    override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = true
    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {}
}
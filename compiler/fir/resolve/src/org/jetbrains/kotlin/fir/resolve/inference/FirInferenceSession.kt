/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ResolutionMode
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage

abstract class FirInferenceSession {
    companion object {
        val DEFAULT: FirInferenceSession = object : FirInferenceSession() {
            override fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = true

            override fun <T> processPartiallyResolvedCall(
                call: T,
                resolutionMode: ResolutionMode,
                completionMode: ConstraintSystemCompletionMode,
            ) where T : FirResolvable, T : FirStatement {
                // Do nothing
            }

            override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
                // Do nothing
            }
        }
    }

    open fun handleQualifiedAccess(qualifiedAccessExpression: FirExpression, data: ResolutionMode) {}

    abstract fun <T> shouldRunCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement

    /**
     * In some cases (like getValue/setValue resolution of property delegation convention), it might be necessary to postpone full completion
     * even with the ContextIndependent mode (which is used for delegated accessors bodies)
     */
    open fun <T> shouldAvoidFullCompletion(call: T): Boolean where T : FirResolvable, T : FirStatement = false

    abstract fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: ResolutionMode,
        completionMode: ConstraintSystemCompletionMode
    ) where T : FirResolvable, T : FirStatement
    abstract fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement

    open fun <R> onCandidatesResolution(call: FirFunctionCall, candidatesResolutionCallback: () -> R) = candidatesResolutionCallback()

    open fun outerCSForCandidate(candidate: Candidate): ConstraintStorage? = null
}

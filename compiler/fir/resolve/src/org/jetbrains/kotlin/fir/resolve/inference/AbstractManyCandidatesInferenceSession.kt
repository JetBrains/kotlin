/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage

abstract class AbstractManyCandidatesInferenceSession(
    protected val resolutionContext: ResolutionContext
) : FirInferenceSession() {
    private val errorCalls: MutableList<FirResolvable> = mutableListOf()
    protected val partiallyResolvedCalls: MutableList<Pair<FirResolvable, Candidate>> = mutableListOf()
    private val completedCalls: MutableSet<FirResolvable> = mutableSetOf()

    protected val components: BodyResolveComponents
        get() = resolutionContext.bodyResolveComponents

    override val currentConstraintSystem: ConstraintStorage
        get() = partiallyResolvedCalls.lastOrNull()
            ?.second
            ?.system
            ?.currentStorage()
            ?: ConstraintStorage.Empty

    override fun <T> addCompetedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        // do nothing
    }

    final override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {
        partiallyResolvedCalls += call to call.candidate
    }

    final override fun <T> addErrorCall(call: T) where T : FirResolvable, T : FirStatement {
        errorCalls += call
    }

    final override fun <T> callCompleted(call: T): Boolean where T : FirResolvable, T : FirStatement {
        return !completedCalls.add(call)
    }

    protected val FirResolvable.candidate: Candidate
        get() = candidate()!!
}

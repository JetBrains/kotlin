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
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.jetbrains.kotlin.types.model.StubTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

abstract class AbstractManyCandidatesInferenceSession(
    protected val resolutionContext: ResolutionContext
) : FirInferenceSession() {
    override fun fixSyntheticTypeVariableWithNotEnoughInformation(
        typeVariable: TypeVariableMarker,
        completionContext: ConstraintSystemCompletionContext
    ) {
    }

    protected val partiallyResolvedCalls: MutableList<Pair<FirResolvable, Candidate>> = mutableListOf()
    private val completedCalls: MutableSet<FirResolvable> = mutableSetOf()

    protected val components: BodyResolveComponents
        get() = resolutionContext.bodyResolveComponents

    override fun <T> addCompletedCall(call: T, candidate: Candidate) where T : FirResolvable, T : FirStatement {
        // do nothing
    }

    override fun <T> addPartiallyResolvedCall(call: T) where T : FirResolvable, T : FirStatement {
        partiallyResolvedCalls += call to call.candidate
    }

    override fun registerStubTypes(map: Map<TypeVariableMarker, StubTypeMarker>) {}

    protected val FirResolvable.candidate: Candidate
        get() = candidate()!!

    override fun clear() {
        partiallyResolvedCalls.clear()
        completedCalls.clear()
    }

}

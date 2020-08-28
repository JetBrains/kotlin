/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.inference.InferenceComponents
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

class ResolutionStageRunner(val components: InferenceComponents) {
    fun processCandidate(candidate: Candidate, stopOnFirstError: Boolean = true): CandidateApplicability {
        val sink = CheckerSinkImpl(components, stopOnFirstError = stopOnFirstError)
        with (candidate.bodyResolveComponents) { candidate.symbol.phasedFir }
        var finished = false
        sink.continuation = suspend {
            candidate.callInfo.callKind.resolutionSequence.forEachIndexed { index, stage ->
                if (index < candidate.passedStages) return@forEachIndexed
                candidate.passedStages++
                stage.check(candidate, sink, candidate.callInfo)
            }
        }.createCoroutineUnintercepted(completion = object : Continuation<Unit> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Unit>) {
                result.exceptionOrNull()?.let { throw it }
                finished = true
            }
        })

        while (!finished) {
            sink.continuation!!.resume(Unit)
            if (sink.current < CandidateApplicability.SYNTHETIC_RESOLVED) {
                break
            }
        }
        return sink.current
    }
}

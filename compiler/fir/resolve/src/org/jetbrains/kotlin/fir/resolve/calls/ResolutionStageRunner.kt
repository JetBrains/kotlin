/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

class ResolutionStageRunner(val components: InferenceComponents) {
    fun processCandidate(candidate: Candidate): CandidateApplicability {
        val sink = CheckerSinkImpl(components)
        var finished = false
        sink.continuation = suspend {
            for (stage in candidate.callInfo.callKind.resolutionSequence) {
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
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

class ResolutionStageRunner {
    fun processCandidate(candidate: Candidate, context: ResolutionContext, stopOnFirstError: Boolean = true): CandidateApplicability {
        val sink = CheckerSinkImpl(candidate, stopOnFirstError = stopOnFirstError)
        var finished = false
        sink.continuation = suspend {
            candidate.callInfo.callKind.resolutionSequence.forEachIndexed { index, stage ->
                if (index < candidate.passedStages) return@forEachIndexed
                candidate.passedStages++
                stage.check(candidate, candidate.callInfo, sink, context)
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
            if (!candidate.isSuccessful) {
                break
            }
        }
        return candidate.currentApplicability
    }
}

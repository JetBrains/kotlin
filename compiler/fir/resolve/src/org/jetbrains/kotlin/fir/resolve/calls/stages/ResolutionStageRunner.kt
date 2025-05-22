/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSinkImpl
import org.jetbrains.kotlin.fir.resolve.inference.constraintsLogger
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import kotlin.context
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.resume

class ResolutionStageRunner {
    fun processCandidate(candidate: Candidate, context: ResolutionContext, stopOnFirstError: Boolean = true): CandidateApplicability {
        val sink = CheckerSinkImpl(candidate, stopOnFirstError = stopOnFirstError)
        val constraintsLogger = candidate.callInfo.session.constraintsLogger
        constraintsLogger?.logCandidate(candidate)
        var finished = false
        sink.continuation = suspend {
            // Multiple runs on the same candidate are possible,
            // that's why we have to skip already processed stages on the next run.
            // Neither regular `for` loop nor iterating by index don't work here,
            // because we have to start from the next unprocessed stage and mutate `Candidate.passedStages` on every iteration.
            val resolutionSequence = candidate.callInfo.callKind.resolutionSequence
            while (candidate.passedStages < resolutionSequence.size) {
                with(context) {
                    with(sink) {
                        val nextStage = resolutionSequence[candidate.passedStages++]
                        constraintsLogger?.logStage("Resolution Stages > ${nextStage::class.simpleName}", candidate.system)
                        nextStage.check(candidate, candidate.callInfo)
                    }
                }
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
        return candidate.lowestApplicability
    }
}

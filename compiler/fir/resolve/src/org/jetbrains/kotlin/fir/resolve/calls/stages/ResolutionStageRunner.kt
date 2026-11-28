/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.stages

import org.jetbrains.kotlin.fir.resolve.calls.ResolutionContext
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSinkImpl
import org.jetbrains.kotlin.fir.resolve.inference.inferenceLogger
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

class ResolutionStageRunner {
    fun processCandidate(
        candidate: Candidate,
        context: ResolutionContext,
        stopOnFirstError: Boolean = true,
        runAdditionalStages: Boolean = false,
    ): CandidateApplicability {
        val sink = CheckerSinkImpl(candidate, stopOnFirstError = stopOnFirstError)
        val inferenceLogger = candidate.callInfo.session.inferenceLogger
        inferenceLogger?.logCandidate(candidate)

        // Multiple runs on the same candidate are possible,
        // that's why we have to skip already processed stages on the next run.
        val resolutionSequence = candidate.callInfo.callKind.let {
            if (runAdditionalStages) it.resolutionSequenceWithAdditionalStages
            else it.resolutionSequence
        }
        while (candidate.passedStages < resolutionSequence.size) {
            val nextStage = resolutionSequence[candidate.passedStages++]
            inferenceLogger?.logStage("Resolution Stages > ${nextStage::class.simpleName}", candidate.system)
            nextStage.check(candidate, sink = sink, context = context)
            if (sink.needYielding) break
        }
        return candidate.lowestApplicability
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.resolve.calls.*


fun createErrorReferenceWithErrorCandidate(
    callInfo: CallInfo,
    diagnostic: ConeDiagnostic,
    source: KtSourceElement?,
    resolutionContext: ResolutionContext,
    resolutionStageRunner: ResolutionStageRunner
): FirErrorReferenceWithCandidate {
    return FirErrorReferenceWithCandidate(
        source,
        callInfo.name,
        resolutionStageRunner.createErrorCandidate(callInfo, resolutionContext, diagnostic),
        diagnostic
    )
}

fun createErrorReferenceWithExistingCandidate(
    candidate: Candidate,
    diagnostic: ConeDiagnostic,
    source: KtSourceElement?,
    resolutionContext: ResolutionContext,
    resolutionStageRunner: ResolutionStageRunner,
): FirErrorReferenceWithCandidate {
    resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
    return FirErrorReferenceWithCandidate(source, candidate.callInfo.name, candidate, diagnostic)
}

fun ResolutionStageRunner.createErrorCandidate(
    callInfo: CallInfo,
    resolutionContext: ResolutionContext,
    diagnostic: ConeDiagnostic
): Candidate {
    val candidate = CandidateFactory(resolutionContext, callInfo).createErrorCandidate(callInfo, diagnostic)
    processCandidate(candidate, resolutionContext, stopOnFirstError = false)
    return candidate
}

fun ResolutionStageRunner.fullyProcessCandidate(candidate: Candidate, resolutionContext: ResolutionContext) {
    if (!candidate.fullyAnalyzed) {
        processCandidate(candidate, resolutionContext, stopOnFirstError = false)
    }
}

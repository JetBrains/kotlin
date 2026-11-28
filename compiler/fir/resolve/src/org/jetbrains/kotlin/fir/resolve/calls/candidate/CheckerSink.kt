/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls.candidate

import org.jetbrains.kotlin.fir.resolve.calls.ResolutionDiagnostic

abstract class CheckerSink {
    abstract fun reportDiagnostic(diagnostic: ResolutionDiagnostic)

    /**
     * Requests [org.jetbrains.kotlin.fir.resolve.calls.stages.ResolutionStageRunner] to skip
     * the remaining stages of the candidate being processed.
     *
     * Does nothing unless the candidate is already unsuccessful and the run is in stop-on-first-error mode.
     * The current stage always runs to completion.
     */
    abstract fun yieldIfNeed()

    /** Whether stopping was requested by [yieldIfNeed]; consumed by the runner after each stage. */
    abstract val needYielding: Boolean
}

fun CheckerSink.yieldDiagnostic(diagnostic: ResolutionDiagnostic) {
    reportDiagnostic(diagnostic)
    yieldIfNeed()
}

class CheckerSinkImpl(
    private val candidate: Candidate,
    private val stopOnFirstError: Boolean = true,
) : CheckerSink() {
    private var stopRequested = false

    override fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
        candidate.addDiagnostic(diagnostic)
    }

    override fun yieldIfNeed() {
        if (stopOnFirstError && !candidate.isSuccessful) {
            stopRequested = true
        }
    }

    override val needYielding: Boolean
        get() = stopRequested
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSink

class CollectionLiteralOuterCandidateContext(
    /**
     * [Candidate] whose constraint system must be expanded by the CL's system.
     * During overload resolution, it is always the immediate containing candidate.
     * During completion, it may be an arbitrary outer call.
     */
    val containingCandidate: Candidate,
    /**
     * [CheckerSink] of outer candidate.
     * Only non-`null` when CL is expanded as part of the overload resolution of some outer call.
     */
    val checkerSink: CheckerSink? = null,
)

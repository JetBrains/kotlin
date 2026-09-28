/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve

import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CheckerSink

/**
 * Describes the outer call on behalf of which a [org.jetbrains.kotlin.fir.resolve.calls.ConeAtomWithExpectedTypeAsStaticReceiver]
 * is being resolved.
 */
class OuterCandidateContextForAtomWithExpectedTypeAsStaticReceiver(
    /**
     * [Candidate] whose constraint system must be expanded by the results of the atom's resolution.
     * During overload resolution, it is always the immediate containing candidate.
     * During completion, it may be an arbitrary outer call.
     */
    val containingCandidate: Candidate,
    /**
     * [CheckerSink] of outer candidate.
     * Only non-`null` when the atom is resolved as part of the overload resolution of some outer call.
     */
    val checkerSink: CheckerSink? = null,
) {
    val isDuringOverloadResolution: Boolean
        get() = checkerSink != null
}

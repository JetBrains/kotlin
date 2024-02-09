/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
import org.jetbrains.kotlin.resolve.calls.tower.ApplicabilityDetail
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
import org.jetbrains.kotlin.resolve.calls.tower.isSuccess
import org.jetbrains.kotlin.resolve.calls.tower.shouldStopResolve

open class CandidateCollector(
    val components: BodyResolveComponents,
    private val resolutionStageRunner: ResolutionStageRunner
) {
    private val groupNumbers = mutableListOf<TowerGroup>()
    private val candidates = mutableListOf<Candidate>()

    var currentApplicability = CandidateApplicability.HIDDEN
        private set

    private var bestGroup = TowerGroup.Last

    fun newDataSet() {
        groupNumbers.clear()
        candidates.clear()
        currentApplicability = CandidateApplicability.HIDDEN
        bestGroup = TowerGroup.Last
    }

    open fun consumeCandidate(group: TowerGroup, candidate: Candidate, context: ResolutionContext): CandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate, context)

        if (applicability > currentApplicability || (applicability == currentApplicability && group < bestGroup)) {
            // Only throw away previous candidates if the new one is successful. If we don't find a successful candidate, we keep all
            // unsuccessful ones so that we can run all stages and pick the one with the least bad applicability.
            // See FirCallResolver.reduceCandidates.
            if (applicability >= CandidateApplicability.RESOLVED_LOW_PRIORITY) {
                candidates.clear()
            }

            if (currentApplicability == CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY &&
                applicability > currentApplicability
            ) {
                candidate.addDiagnostic(ResolutionResultOverridesOtherToPreserveCompatibility)
            }

            currentApplicability = applicability
            bestGroup = group
        }

        /*
         * Here we would like to consider error candidates with `INAPPLICABLE_WRONG_RECEIVER` and `INAPPLICABLE_ARGUMENTS_MAPPING_ERROR` kinda
         *   "same error level". Generally it's questionable which candidates we should keep and which of those applicabilities is more
         *   specific and we should consider it during work on improvement of error reporting. But this particular check is needed
         *   to fix the KT-65218, which provoked by different stdlib declarations order in CLI compilation mode and AA mode (see
         *   the issue for more details)
         */
        if (
            (applicability == currentApplicability && group == bestGroup) ||
            (currentApplicability == INAPPLICABLE_ARGUMENTS_MAPPING_ERROR && applicability == INAPPLICABLE_WRONG_RECEIVER)
        ) {
            candidates.add(candidate)
        }

        return applicability
    }

    fun bestCandidates(): List<Candidate> = candidates

    open fun shouldStopAtTheGroup(group: TowerGroup): Boolean =
        shouldStopResolve && bestGroup < group

    val shouldStopResolve: Boolean
        get() = currentApplicability.shouldStopResolve

    @OptIn(ApplicabilityDetail::class)
    val isSuccess: Boolean
        get() = currentApplicability.isSuccess
}

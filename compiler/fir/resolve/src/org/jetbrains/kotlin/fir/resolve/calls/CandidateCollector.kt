/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability
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

            currentApplicability = applicability
            bestGroup = group
        }

        if (applicability == currentApplicability && group == bestGroup) {
            candidates.add(candidate)
        }

        return applicability
    }

    fun bestCandidates(): List<Candidate> = candidates

    open fun shouldStopAtTheGroup(group: TowerGroup): Boolean =
        shouldStopResolve && bestGroup < group

    val shouldStopResolve: Boolean
        get() = currentApplicability.shouldStopResolve

    val isSuccess: Boolean
        get() = currentApplicability.isSuccess
}

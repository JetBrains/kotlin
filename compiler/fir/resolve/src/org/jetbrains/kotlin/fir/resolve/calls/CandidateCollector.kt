/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents

open class CandidateCollector(
    val components: BodyResolveComponents,
    private val resolutionStageRunner: ResolutionStageRunner
) {
    private val groupNumbers = mutableListOf<TowerGroup>()
    private val candidates = mutableListOf<Candidate>()

    var currentApplicability = CandidateApplicability.HIDDEN
        private set

    private var currentGroup = TowerGroup.Start

    fun newDataSet() {
        groupNumbers.clear()
        candidates.clear()
        currentApplicability = CandidateApplicability.HIDDEN
    }

    open fun consumeCandidate(group: TowerGroup, candidate: Candidate): CandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate)

        if (applicability > currentApplicability) {
            groupNumbers.clear()
            candidates.clear()
            currentApplicability = applicability
            currentGroup = group
        }

        if (applicability == currentApplicability) {
            candidates.add(candidate)
            groupNumbers.add(group)
        }

        return applicability
    }

    fun bestCandidates(): List<Candidate> {
        if (groupNumbers.isEmpty()) return emptyList()
        if (groupNumbers.size == 1) return candidates
        val result = mutableListOf<Candidate>()
        var bestGroup = groupNumbers.first()
        for ((index, candidate) in candidates.withIndex()) {
            val group = groupNumbers[index]
            if (bestGroup > group) {
                bestGroup = group
                result.clear()
            }
            if (bestGroup == group) {
                result.add(candidate)
            }
        }
        return result
    }

    fun isSuccess(): Boolean {
        return currentApplicability >= CandidateApplicability.SYNTHETIC_RESOLVED
    }
}


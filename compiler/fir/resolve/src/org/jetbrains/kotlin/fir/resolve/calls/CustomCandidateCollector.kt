/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

class CustomCandidateCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CandidateCollector(components, resolutionStageRunner) {
    override fun consumeCandidate(group: TowerGroup, candidate: Candidate, context: ResolutionContext): CandidateApplicability {
        val applicability = resolutionStageRunner.processCandidate(candidate, context)

        if (applicability > currentApplicability || (applicability == currentApplicability && group < bestGroup)) {
            candidates.clear()
            currentApplicability = applicability
            bestGroup = group
        }

        if (applicability == currentApplicability /*&& group == bestGroup*/) {
            candidates.add(candidate)
        }

        return applicability
    }

    override fun shouldStopAtTheLevel(group: TowerGroup): Boolean = false
//        /*isSuccess() &&*/ bestGroup < group
}
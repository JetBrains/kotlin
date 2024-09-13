/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.calls

import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.resolve.BodyResolveComponents
import org.jetbrains.kotlin.fir.resolve.calls.candidate.Candidate
import org.jetbrains.kotlin.fir.resolve.calls.candidate.CandidateCollector
import org.jetbrains.kotlin.fir.resolve.calls.stages.ResolutionStageRunner
import org.jetbrains.kotlin.fir.resolve.calls.tower.TowerGroup
import org.jetbrains.kotlin.fir.scopes.impl.originalConstructorIfTypeAlias
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.resolve.calls.tower.CandidateApplicability

/** Used for IDE */
class AllCandidatesCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CandidateCollector(components, resolutionStageRunner) {
    private val allCandidatesMap = mutableMapOf<FirBasedSymbol<*>, Candidate>()

    override fun consumeCandidate(group: TowerGroup, candidate: Candidate, context: ResolutionContext): CandidateApplicability {
        // Filter duplicate symbols. In the case of typealias constructor calls, we consider the original constructor for uniqueness.
        val key = (candidate.symbol.fir as? FirConstructor)?.originalConstructorIfTypeAlias?.symbol
            ?: candidate.symbol

        // To preserve the behavior of a HashSet which keeps the first added item, we use getOrPut instead of put.
        // Changing this behavior breaks testData/components/callResolver/resolveCandidates/singleCandidate/functionTypeVariableCall_extensionReceiver.kt
        allCandidatesMap.getOrPut(key) { candidate }
        return super.consumeCandidate(group, candidate, context)
    }

    // We want to get candidates at all tower levels.
    override fun shouldStopAtTheGroup(group: TowerGroup): Boolean = false

    val allCandidates: Collection<Candidate>
        get() = allCandidatesMap.values
}

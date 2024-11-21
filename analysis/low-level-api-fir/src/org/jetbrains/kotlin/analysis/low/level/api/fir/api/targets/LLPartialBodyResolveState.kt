/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext

/**
 * Represents the incomplete body resolve state.
 * The attribute is only present if the function body was analyzed partially.
 */
internal data class LLPartialBodyResolveState(
    val totalPsiStatementCount: Int,
    val analyzedPsiStatementCount: Int,
    val analyzedFirStatementCount: Int,
    val performedAnalysesCount: Int,
    val analysisStateSnapshot: LLPartialBodyResolveSnapshot?
) {
    @Volatile
    var isValid: Boolean = true
        private set

    fun invalidate() {
        isValid = false
    }

    val isFullyAnalyzed: Boolean
        get() = totalPsiStatementCount == analyzedPsiStatementCount

    override fun toString(): String {
        return "$analyzedFirStatementCount($analyzedPsiStatementCount/$totalPsiStatementCount) #$performedAnalysesCount"
    }
}

internal class LLPartialBodyResolveSnapshot(
    val towerDataContext: FirTowerDataContext,
    val dataFlowAnalyzerContext: DataFlowAnalyzerContext
)
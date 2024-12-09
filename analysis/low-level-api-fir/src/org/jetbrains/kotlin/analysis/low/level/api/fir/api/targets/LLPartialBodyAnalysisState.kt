/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CfgInternals
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

/**
 * Represents the incomplete body resolve state.
 * The attribute is only present if the function body was analyzed partially.
 */
internal data class LLPartialBodyAnalysisState(
    val totalPsiStatementCount: Int,
    val analyzedPsiStatementCount: Int,
    val analyzedFirStatementCount: Int,
    val performedAnalysesCount: Int,
    val analysisStateSnapshot: LLPartialBodyAnalysisSnapshot?,
    val previousState: LLPartialBodyAnalysisState?
) {
    init {
        if (previousState != null) {
            require(previousState.analyzedFirStatementCount < analyzedFirStatementCount && previousState.analyzedPsiStatementCount < analyzedPsiStatementCount)
        }
    }

    val isFullyAnalyzed: Boolean
        get() = totalPsiStatementCount == analyzedPsiStatementCount

    override fun toString(): String {
        return "$analyzedFirStatementCount($analyzedPsiStatementCount/$totalPsiStatementCount) #$performedAnalysesCount"
    }
}

internal class LLPartialBodyAnalysisSnapshot(
    val result: LLPartialBodyAnalysisResult,
    val towerDataContext: FirTowerDataContext,
    val dataFlowAnalyzerContext: DataFlowAnalyzerContext
) {
    @OptIn(CfgInternals::class)
    val controlFlowGraphNodes: List<CFGNode<*>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        /**
         * Here we cannot use [ControlFlowGraph.nodes] directly, as the graph is incomplete.
         * Still, partial body resolution only covers the topmost block statements, and there should be no jumps between them.
         * So CFG for the analyzed part is complete and can be used for getting smartcast information.
         * */
        dataFlowAnalyzerContext.currentGraph.orderNodes(isComplete = false)
    }
}

internal class LLPartialBodyAnalysisResult(
    val statements: List<FirStatement>,
    val delegatedConstructorCall: FirDelegatedConstructorCall?,
    val defaultParameterValues: Map<FirValueParameter, FirExpression>
)
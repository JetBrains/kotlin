/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api.targets

import org.jetbrains.kotlin.fir.declarations.FirTowerDataContext
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.dfa.DataFlowAnalyzerContext
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CfgInternals
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

/**
 * Represents the partial (incomplete) body resolve state.
 * The attribute is only present if the function body was analyzed partially.
 *
 * If the attribute is added, it must never be removed. For a fully analyzed declaration, [analyzedPsiStatementCount] is equal to
 * [totalPsiStatementCount]. Also, [analysisStateSnapshot] is kept `null` as unneeded (a fully analyzed function is never analyzed again).
 */
internal data class LLPartialBodyAnalysisState(
    /**
     * A number of topmost [com.intellij.psi.PsiExpression] statements in the declaration's body block.
     *
     * A topmost statement is a statement directly nested inside the declaration's body block.
     * Such as, in the following example, `foo()` and `baz {}` are topmost statements while `baz()` is not.
     *
     * ```
     * fun test() {
     *     foo()
     *     bar {
     *         baz()
     *     }
     * }
     * ```
     */
    val totalPsiStatementCount: Int,

    /**
     * A number of analyzed [com.intellij.psi.PsiExpression] statements in the declaration's body block.
     * @see totalPsiStatementCount
     */
    val analyzedPsiStatementCount: Int,

    /**
     * A number of analyzed [FirStatement] in the declaration's body block.
     */
    val analyzedFirStatementCount: Int,

    /**
     * A number of partial body analyses happened with the declaration up to this point.
     * Note that even if [performedAnalysesCount] is greater than zero, only function signature parts (e.g., default parameter values)
     * might be analyzed.
     *
     * After a certain number of analyses, the resolution engine might decide to perform full body analysis.
     */
    val performedAnalysesCount: Int,

    /**
     * Data required for subsequent analysis.
     * [analysisStateSnapshot] is `null` if the declaration is fully analyzed.
     */
    val analysisStateSnapshot: LLPartialBodyAnalysisSnapshot?
) {
    /**
     * Whether a declaration is fully analyzed.
     * Prefer [org.jetbrains.kotlin.fir.declarations.FirResolveState.resolvePhase] if you have a declaration in hands.
     */
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

/**
 * Contains already resolved parts of the declaration.
 *
 * In cases when a declaration may be only partially resolved, access the resolved state through this class instead of checking
 * the FIR tree directly (such as getting statements from [org.jetbrains.kotlin.fir.declarations.FirFunction.body]),
 * as these parts can be concurrently replaced with lazy blocks/expressions by a state keeper.
 */
internal class LLPartialBodyAnalysisResult(
    /**
     * A list of already resolved statements.
     * The list contains [LLPartialBodyAnalysisState.analyzedFirStatementCount] first statements from the function body.
     */
    val statements: List<FirStatement>,
    /**
     * A resolved delegated constructor call.
     */
    val delegatedConstructorCall: FirDelegatedConstructorCall?,
    /**
     * A list of resolved default parameter values.
     * The list is empty if there are no value parameters with default values.
     *
     * Now, this does not include corresponding parameter names as this has never been needed.
     */
    val defaultParameterValues: List<FirExpression>
)
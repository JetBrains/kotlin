/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

object UnreachableCodeChecker : FirControlFlowChecker(MppCheckerKind.Common) {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        val nodes = graph.allNodes()
        val (unreachableNodes, reachableNodes) = nodes.filterNot { it.skipNode() }.partition { it.isDead }
        if (unreachableNodes.isEmpty()) return
        val unreachableSources = unreachableNodes.mapNotNull { it.fir.source }.toSet()
        val reachableSources = reachableNodes.mapNotNull { it.fir.source }.toSet()
        val unreachableElements = unreachableNodes.map { it.fir }
        val innerNodes = mutableSetOf<FirElement>()
        unreachableElements.forEach { it.collectInnerNodes(innerNodes) }
        unreachableElements.distinctBy { it.source }.forEach { element ->
            if (element !in innerNodes) {
                reporter.reportOn(element.source, FirErrors.UNREACHABLE_CODE, reachableSources, unreachableSources, context)
            }
        }
    }

    private fun ControlFlowGraph.allNodes(acc: MutableList<CFGNode<*>> = mutableListOf()): List<CFGNode<*>> {
        acc.addAll(this.nodes)
        subGraphs.forEach { it.allNodes(acc) }
        return acc
    }

    private val sourceKindsToSkip = setOf(
        KtFakeSourceElementKind.ImplicitReturn.FromExpressionBody,
        KtFakeSourceElementKind.ImplicitReturn.FromLastStatement,
        KtFakeSourceElementKind.DesugaredForLoop
    )

    private fun CFGNode<*>.skipNode(): Boolean {
        val skipType = this is ExitNodeMarker ||
                this is EnterNodeMarker ||
                this is StubNode ||
                this is SplitPostponedLambdasNode ||
                this is BinaryOrExitLeftOperandNode ||
                this is BinaryOrEnterRightOperandNode ||
                this is BinaryAndExitLeftOperandNode ||
                this is BinaryAndEnterRightOperandNode ||
                this is WhenSyntheticElseBranchNode ||
                this is WhenBranchResultEnterNode ||
                this is WhenBranchResultExitNode
        val allowType = this is LoopEnterNode ||
                this is LoopBlockEnterNode ||
                this is TryExpressionEnterNode
        return !allowType && (skipType || sourceKindsToSkip.contains(this.fir.source?.kind))
    }


    private fun FirElement.collectInnerNodes(nodes: MutableSet<FirElement>) {
        acceptChildren(CollectNodesVisitor(nodes))
    }

    private class CollectNodesVisitor(private val nodes: MutableSet<FirElement>) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            nodes.add(element)
            element.acceptChildren(this)
        }
    }
}

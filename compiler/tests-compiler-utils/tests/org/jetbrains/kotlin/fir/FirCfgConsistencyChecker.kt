/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.EdgeKind
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.Assertions

class FirCfgConsistencyChecker(private val assertions: Assertions) : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        val graph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
        assertions.assertEquals(ControlFlowGraph.State.Completed, graph.state)
        checkConsistency(graph)
        checkOrder(graph)
    }

    private fun checkConsistency(graph: ControlFlowGraph) {
        for (node in graph.nodes) {
            for (to in node.followingNodes) {
                checkEdge(node, to)
            }
            for (from in node.previousNodes) {
                checkEdge(from, node)
            }
            if (node.followingNodes.isEmpty() && node.previousNodes.isEmpty()) {
                throw AssertionError("Unconnected CFG node: $node")
            }
        }
    }

    private val cfgKinds = listOf(EdgeKind.DeadForward, EdgeKind.CfgForward, EdgeKind.DeadBackward, EdgeKind.CfgBackward)

    private fun checkEdge(from: CFGNode<*>, to: CFGNode<*>) {
        assertions.assertContainsElements(from.followingNodes, to)
        assertions.assertContainsElements(to.previousNodes, from)
        val fromKind = from.outgoingEdges.getValue(to).kind
        val toKind = to.incomingEdges.getValue(from).kind
        assertions.assertEquals(fromKind, toKind)
        if (from.isDead && to.isDead) {
            assertions.assertContainsElements(cfgKinds, toKind)
        }
    }

    private fun checkOrder(graph: ControlFlowGraph) {
        val visited = mutableSetOf<CFGNode<*>>()
        for (node in graph.nodes) {
            for (previousNode in node.previousNodes) {
                if (previousNode.owner != graph) continue
                if (!node.incomingEdges.getValue(previousNode).kind.isBack) {
                    assertions.assertTrue(previousNode in visited)
                }
            }
            visited += node
        }
    }
}

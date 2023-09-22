/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.fir.references.FirControlFlowGraphReference
import org.jetbrains.kotlin.fir.resolve.dfa.FirControlFlowGraphReferenceImpl
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.test.Assertions

class FirCfgConsistencyChecker(private val assertions: Assertions) : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {
        val graph = (controlFlowGraphReference as? FirControlFlowGraphReferenceImpl)?.controlFlowGraph ?: return
        assertions.assertEquals(graph.nodes.single { it is GraphEnterNodeMarker }, graph.enterNode)
        assertions.assertEquals(graph.nodes.single { it is GraphExitNodeMarker }, graph.exitNode)
        checkConsistency(graph)
        checkOrder(graph)
    }

    private fun checkConsistency(graph: ControlFlowGraph) {
        for (node in graph.nodes) {
            assertions.assertEquals(node.followingNodes.size, node.followingNodes.toSet().size) { "followingNodes has repeats: $node" }
            assertions.assertEquals(node.previousNodes.size, node.previousNodes.toSet().size) { "previousNodes has repeats: $node" }
            for (to in node.followingNodes) {
                assertions.assertContainsElements(to.previousNodes, node)
                assertions.assertFalse(node.isDead && to.isDead && to.edgeFrom(node).kind.usedInDfa) {
                    "data flow between dead nodes: $node -> $to"
                }
            }
            for (from in node.previousNodes) {
                assertions.assertContainsElements(from.followingNodes, node)
            }
            assertions.assertFalse(node.followingNodes.isEmpty() && node.previousNodes.isEmpty()) { "Unconnected CFG node: $node" }
            assertions.assertTrue(node is ClassExitNode || node.flowInitialized) { "All nodes must have a flow: $node" }
        }
    }

    private fun checkOrder(graph: ControlFlowGraph) {
        val visited = mutableSetOf<CFGNode<*>>()
        for (node in graph.nodes) {
            for (previousNode in node.previousNodes) {
                if (previousNode.owner != graph) continue
                if (!node.edgeFrom(previousNode).kind.isBack) {
                    assertions.assertTrue(previousNode in visited)
                }
            }
            visited += node
        }
    }
}

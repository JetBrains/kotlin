/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNodeWithCfgOwner
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

fun ControlFlowGraph.getEnterNode(direction: TraverseDirection): CFGNode<*> = when (direction) {
    TraverseDirection.Forward -> enterNode
    TraverseDirection.Backward -> exitNode
}

fun ControlFlowGraph.getNodesInOrder(direction: TraverseDirection): List<CFGNode<*>> = when (direction) {
    TraverseDirection.Forward -> nodes
    TraverseDirection.Backward -> nodes.asReversed()
}

fun CFGNode<*>.isEnterNode(direction: TraverseDirection): Boolean = when (direction) {
    TraverseDirection.Forward -> owner.enterNode == this
    TraverseDirection.Backward -> owner.exitNode == this
}

val CFGNode<*>.previousCfgNodes: List<CFGNode<*>>
    get() = previousNodes.filter {
        val kind = incomingEdges.getValue(it).kind
        if (this.isDead) {
            kind.usedInCfa
        } else {
            kind.usedInCfa && !kind.isDead
        }
    }

val CFGNode<*>.followingCfgNodes: List<CFGNode<*>>
    get() {
        val nodes = mutableListOf<CFGNode<*>>()

        followingNodes.filterTo(nodes) {
            val kind = outgoingEdges.getValue(it).kind
            kind.usedInCfa && !kind.isDead
        }
        (this as? CFGNodeWithCfgOwner<*>)?.subGraphs?.mapTo(nodes) { it.enterNode }

        return nodes
    }
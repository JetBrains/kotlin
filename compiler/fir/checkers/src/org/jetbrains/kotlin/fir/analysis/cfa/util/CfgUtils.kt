/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph

fun ControlFlowGraph.getEnterNode(direction: TraverseDirection): CFGNode<*> = when (direction) {
    TraverseDirection.Forward -> enterNode
    TraverseDirection.Backward -> exitNode
}

fun ControlFlowGraph.getNodesInOrder(direction: TraverseDirection): List<CFGNode<*>> = when (direction) {
    TraverseDirection.Forward -> nodes
    TraverseDirection.Backward -> nodes.asReversed()
}

val CFGNode<*>.previousCfgNodes: List<CFGNode<*>>
    get() = previousNodes.filter {
        val kind = edgeFrom(it).kind
        if (this.isDead) {
            kind.usedInCfa
        } else {
            kind.usedInCfa && !kind.isDead
        }
    }

val CFGNode<*>.followingCfgNodes: List<CFGNode<*>>
    get() = followingNodes.filter {
        val kind = edgeTo(it).kind
        kind.usedInCfa && !kind.isDead
    }

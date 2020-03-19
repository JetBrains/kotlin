/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitorVoid

enum class TraverseDirection {
    Forward, Backward
}

@OptIn(ExperimentalStdlibApi::class)
fun <D> ControlFlowGraph.traverse(
    direction: TraverseDirection,
    visitor: ControlFlowGraphVisitor<*, D>,
    data: D
) {
    val visitedNodes = mutableSetOf<CFGNode<*>>()
    // used to prevent infinite cycle
    val delayedNodes = mutableSetOf<CFGNode<*>>()
    val stack = ArrayDeque<CFGNode<*>>()
    val initialNode = when (direction) {
        TraverseDirection.Forward -> enterNode
        TraverseDirection.Backward -> exitNode
    }
    stack.addFirst(initialNode)
    while (stack.isNotEmpty()) {
        val node = stack.removeFirst()
        val previousNodes = when (direction) {
            TraverseDirection.Forward -> node.previousNodes
            TraverseDirection.Backward -> node.followingNodes
        }
        if (!previousNodes.all { it in visitedNodes }) {
            if (!delayedNodes.add(node)) {
                throw IllegalArgumentException("Infinite loop")
            }
            stack.addLast(node)
        }

        node.accept(visitor, data)

        val followingNodes = when (direction) {
            TraverseDirection.Forward -> node.followingNodes
            TraverseDirection.Backward -> node.previousNodes
        }

        followingNodes.forEach {
            stack.addFirst(it)
        }
    }
}

fun ControlFlowGraph.traverse(
    direction: TraverseDirection,
    visitor: ControlFlowGraphVisitorVoid
) {
    traverse(direction, visitor, null)
}
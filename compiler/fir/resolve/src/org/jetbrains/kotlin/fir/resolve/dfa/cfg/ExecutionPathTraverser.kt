/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.cfg

interface ExecutionPathTraverser<T> {
    fun traverse(graph: ControlFlowGraph, initialData: T) = traverse(graph.enterNode, initialData)

    fun traverse(initialNode: CFGNode<*>, initialData: T) {
        val stack = ArrayDeque<Pair<CFGNode<*>, T>>()
        // Manually maintain a stack so that we won't go stack overflow for very large graphs.
        stack.addLast(initialNode to initialData)
        while (stack.isNotEmpty()) {
            val (currentNode, currentData) = stack.removeLast()
            for (nextNode in currentNode.followingNodes.asReversed()) {
                val edge = currentNode.outgoingEdges[nextNode] ?: Edge.Normal_Forward
                val kind = edge.kind
                // Only traverse valid CFA node
                if (kind.isDead || !kind.usedInCfa || !shouldFollowEdge(currentNode, nextNode, edge)) continue
                val nextData = handleEdge(currentNode, nextNode, edge, currentData)
                stack.addLast(nextNode to nextData)
            }
        }
    }

    fun shouldFollowEdge(src: CFGNode<*>, dst: CFGNode<*>, edge: Edge): Boolean = true
    fun handleEdge(src: CFGNode<*>, dst: CFGNode<*>, edge: Edge, data: T): T
}

abstract class BackEdgeOnceExecutionPathTraverser<T> : ExecutionPathTraverser<T> {
    private val traversedBackEdges = mutableSetOf<Pair<CFGNode<*>, CFGNode<*>>>()
    override fun shouldFollowEdge(src: CFGNode<*>, dst: CFGNode<*>, edge: Edge): Boolean {
        val kind = edge.kind
        if (!kind.isBack) return true
        val backEdge = src to dst
        return traversedBackEdges.add(backEdge)
    }
}

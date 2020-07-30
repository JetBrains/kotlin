/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

enum class TraverseDirection {
    Forward, Backward
}

fun <D> ControlFlowGraph.traverse(
    direction: TraverseDirection,
    visitor: ControlFlowGraphVisitor<*, D>,
    data: D
) {
    for (node in getNodesInOrder(direction)) {
        node.accept(visitor, data)
        (node as? CFGNodeWithCfgOwner<*>)?.subGraphs?.forEach { it.traverse(direction, visitor, data) }
    }
}

fun ControlFlowGraph.traverse(
    direction: TraverseDirection,
    visitor: ControlFlowGraphVisitorVoid
) {
    traverse(direction, visitor, null)
}

fun <I : ControlFlowInfo<I, K, V>, K : Any, V : Any> ControlFlowGraph.collectDataForNode(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: ControlFlowGraphVisitor<I, Collection<I>>
): Map<CFGNode<*>, I> {
    val nodeMap = LinkedHashMap<CFGNode<*>, I>()
    val startNode = getEnterNode(direction)
    nodeMap[startNode] = initialInfo

    val changed = mutableMapOf<CFGNode<*>, Boolean>()
    do {
        collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap, changed)
    } while (changed.any { it.value })

    return nodeMap
}

private fun <I : ControlFlowInfo<I, K, V>, K : Any, V : Any> ControlFlowGraph.collectDataForNodeInternal(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: ControlFlowGraphVisitor<I, Collection<I>>,
    nodeMap: MutableMap<CFGNode<*>, I>,
    changed: MutableMap<CFGNode<*>, Boolean>
) {
    val nodes = getNodesInOrder(direction)
    for (node in nodes) {
        if (!(node.isEnterNode(direction) && node.owner.owner == null)) {
            val previousNodes = when (direction) {
                TraverseDirection.Forward -> node.previousCfgNodes
                TraverseDirection.Backward -> node.followingCfgNodes
            }
            val previousData = previousNodes.mapNotNull { nodeMap[it] }
            val data = nodeMap[node]
            val newData = node.accept(visitor, previousData)
            val hasChanged = newData != data
            changed[node] = hasChanged
            if (hasChanged) {
                nodeMap[node] = newData
            }
        }
        if (node is CFGNodeWithCfgOwner<*>) {
            node.subGraphs.forEach { it.collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap, changed) }
        }
    }
}

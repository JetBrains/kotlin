/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*

// ------------------------------ Graph Traversal ------------------------------

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

// ---------------------- Path-sensitive data collection -----------------------

fun <I> ControlFlowGraph.collectDataForNode(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: ControlFlowGraphVisitor<I, Collection<Pair<EdgeLabel, I>>>,
    visitSubGraphs: Boolean = true
): Map<CFGNode<*>, I> {
    val nodeMap = LinkedHashMap<CFGNode<*>, I>()
    val startNode = getEnterNode(direction)
    nodeMap[startNode] = initialInfo

    val changed = mutableMapOf<CFGNode<*>, Boolean>()
    do {
        collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap, changed, visitSubGraphs)
    } while (changed.any { it.value })

    return nodeMap
}

private fun <I> ControlFlowGraph.collectDataForNodeInternal(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: ControlFlowGraphVisitor<I, Collection<Pair<EdgeLabel, I>>>,
    nodeMap: MutableMap<CFGNode<*>, I>,
    changed: MutableMap<CFGNode<*>, Boolean>,
    visitSubGraphs: Boolean = true
) {
    val nodes = getNodesInOrder(direction)
    for (node in nodes) {
        if (visitSubGraphs && direction == TraverseDirection.Backward && node is CFGNodeWithCfgOwner<*>) {
            node.subGraphs.forEach { it.collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap, changed) }
        }
        val previousNodes = when (direction) {
            TraverseDirection.Forward -> node.previousCfgNodes
            TraverseDirection.Backward -> node.followingCfgNodes
        }
        // One noticeable different against the path-unaware version is, here, we pair the control-flow info with the label.
        val previousData =
            previousNodes.mapNotNull {
                val k = when (direction) {
                    TraverseDirection.Forward -> node.incomingEdges[it]?.label ?: NormalPath
                    TraverseDirection.Backward -> node.outgoingEdges[it]?.label ?: NormalPath
                }
                val v = nodeMap[it] ?: return@mapNotNull null
                k to v
            }
        val data = nodeMap[node]
        val newData = node.accept(visitor, previousData)
        val hasChanged = newData != data
        changed[node] = hasChanged
        if (hasChanged) {
            nodeMap[node] = newData
        }
        if (visitSubGraphs && direction == TraverseDirection.Forward && node is CFGNodeWithCfgOwner<*>) {
            node.subGraphs.forEach { it.collectDataForNodeInternal(direction, initialInfo, visitor, nodeMap, changed) }
        }
    }
}

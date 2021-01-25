/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol


fun ControlFlowGraph.traverseInterprocedural(
    direction: TraverseDirection,
    visitor: InterproceduralVisitorVoid,
    visitedSymbols: List<AbstractFirBasedSymbol<*>> = listOf()
) {
    for (node in getNodesInOrder(direction)) {
        node.accept(visitor)
        (node as? CFGNodeWithCfgOwner<*>)?.subGraphs?.forEach { it.traverseInterprocedural(direction, visitor, visitedSymbols) }

        if ((node is FunctionCallNode || node is QualifiedAccessNode)) {
            visitor.onNestedCall(direction, visitedSymbols, node)
        }
    }
}

fun <I : PathAwareControlFlowInfo<I, V>, V : ControlFlowInfo<V, *, *>> ControlFlowGraph.collectDataForNodeInterprocedural(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: InterproceduralVisitor<I, V>,
    functionsWhitelist: Collection<FirNamedFunctionSymbol>
): Map<CFGNode<*>, I> {
    val nodeMap = LinkedHashMap<CFGNode<*>, I>()
    val startNode = getEnterNode(direction)
    nodeMap[startNode] = initialInfo

    val changed = mutableMapOf<CFGNode<*>, Boolean>()
    do {
        collectInterDataForNode(direction, initialInfo, visitor, nodeMap, changed, functionsWhitelist)
    } while (changed.any { it.value })

    return nodeMap
}

@Suppress("DuplicatedCode")
internal fun <I : PathAwareControlFlowInfo<I, V>, V : ControlFlowInfo<V, *, *>> ControlFlowGraph.collectInterDataForNode(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: InterproceduralVisitor<I, V>,
    nodeMap: MutableMap<CFGNode<*>, I>,
    changed: MutableMap<CFGNode<*>, Boolean>,
    functionsWhitelist: Collection<FirNamedFunctionSymbol> = listOf(),
    visitedSymbols: Collection<FirBasedSymbol<*>> = listOf(),
    visitedNodes: MutableSet<CFGNode<*>> = mutableSetOf()
) {
    val nodes = getNodesInOrder(direction)
    for (node in nodes) {
        if (direction == TraverseDirection.Backward && node is CFGNodeWithCfgOwner<*>) {
            node.subGraphs.forEach {
                it.collectInterDataForNode(
                    direction,
                    initialInfo,
                    visitor,
                    nodeMap,
                    changed,
                    functionsWhitelist,
                    visitedSymbols,
                    visitedNodes
                )
            }
        }

        visitedNodes.add(node)

        val previousData = nodeMap.filterKeys { it in visitedNodes }.toMutableMap()

        val newData = node.accept(visitor, previousData)
        val hasChanged = newData != nodeMap[node]
        changed[node] = hasChanged
        if (hasChanged) {
            nodeMap[node] = newData
        }

        // if this node is call node, onNestingCall is calling
        if ((node is FunctionCallNode || node is QualifiedAccessNode)) {
            visitor.onFunctionCall(
                direction,
                initialInfo,
                nodeMap,
                changed,
                functionsWhitelist,
                visitedSymbols,
                node,
                visitedNodes
            )
        }

        if (direction == TraverseDirection.Forward && node is CFGNodeWithCfgOwner<*>) {
            node.subGraphs.forEach {
                it.collectInterDataForNode(
                    direction,
                    initialInfo,
                    visitor,
                    nodeMap,
                    changed,
                    functionsWhitelist,
                    visitedSymbols,
                    visitedNodes
                )
            }
        }
    }
}
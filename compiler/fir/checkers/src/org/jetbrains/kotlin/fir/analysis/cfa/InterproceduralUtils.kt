/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol


fun ControlFlowGraph.traverseInterprocedural(
    direction: TraverseDirection,
    visitor: InterproceduralVisitorVoid,
    functionsWhitelist: Set<FirNamedFunctionSymbol>,
    visitedSymbols: List<AbstractFirBasedSymbol<*>> = listOf(),
) {
    for (node in getNodesInOrder(direction)) {
        node.accept(visitor)
        (node as? CFGNodeWithCfgOwner<*>)?.subGraphs?.forEach {
            it.traverseInterprocedural(
                direction,
                visitor,
                functionsWhitelist,
                visitedSymbols
            )
        }

        if ((node is FunctionCallNode && node.fir.toResolvedCallableSymbol() in functionsWhitelist || node is QualifiedAccessNode)) {
            visitor.onNestedCall(direction, visitedSymbols, node, functionsWhitelist)
        }
    }
}

fun <I : PathAwareControlFlowInfo<I, V>, V : ControlFlowInfo<V, *, *>> ControlFlowGraph.collectDataForNodeInterprocedural(
    direction: TraverseDirection,
    initialInfo: I,
    visitor: InterproceduralVisitor<I, V>,
    functionsWhitelist: HashSet<FirNamedFunctionSymbol>
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
    functionsWhitelist: HashSet<FirNamedFunctionSymbol> = hashSetOf(),
    visitedSymbols: Collection<FirBasedSymbol<*>> = listOf(),
    visitedNodes: MutableSet<CFGNode<*>> = mutableSetOf()
) {
    val nodes = getNodesInOrder(direction)
    var prevInitPartExit: CFGNode<*>? = null

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

        if (node is PartOfClassInitializationNode) {
            if (prevInitPartExit != null) {
                val initEnterNode = node.subGraphs.first().enterNode
                visitor.connectInitializerParts(prevInitPartExit, initEnterNode)
            }

            val newExitNode = node.subGraphs.first().exitNode
            prevInitPartExit = newExitNode
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
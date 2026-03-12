/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.analysis.cfa.newNearestNonInPlaceGraph
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

enum class PropertyAccessType {
    InPlace,
    Captured,
}

@JvmInline
value class VariableWriteData(val value: PersistentMap<FirPropertySymbol, PersistentSet<CFGNode<*>>>) {
    constructor(symbol: FirPropertySymbol, node: CFGNode<*>) : this(persistentMapOf(symbol to persistentSetOf(node)))
    constructor(symbol: FirPropertySymbol, data: PersistentSet<CFGNode<*>>) : this(persistentMapOf(symbol to data))

    operator fun get(symbol: FirPropertySymbol): PersistentSet<CFGNode<*>>? = value[symbol]

    operator fun plus(other: VariableWriteData): VariableWriteData {
        return VariableWriteData(value.merge(other.value, PersistentSet<CFGNode<*>>::addAll))
    }

    fun add(symbol: FirPropertySymbol, node: CFGNode<*>): VariableWriteData {
        val nodes = value[symbol] ?: persistentSetOf()
        return VariableWriteData(value.put(symbol, nodes.add(node)))
    }

    fun remove(symbol: FirPropertySymbol): VariableWriteData? {
        val newValue = value.remove(symbol)
        return when {
            newValue.isEmpty() -> null
            else -> VariableWriteData(newValue)
        }
    }

    operator fun iterator(): Iterator<Map.Entry<FirPropertySymbol, PersistentSet<CFGNode<*>>>> {
        return value.iterator()
    }
}

private fun PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>.add(
    type: PropertyAccessType,
    symbol: FirPropertySymbol,
    node: CFGNode<*>,
): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
    return transformValues { oldData ->
        oldData.put(type, oldData[type]?.add(symbol, node) ?: VariableWriteData(symbol, node))
    }
}

private fun PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>.remove(
    symbol: FirPropertySymbol,
): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
    return transformValues { oldData ->
        oldData.mutate {
            for ((type, writes) in oldData) {
                when (val value = writes.remove(symbol)) {
                    null -> it.remove(type)
                    else -> it[type] = value
                }
            }
        }
    }
}

private fun PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>.overwrite(
    symbol: FirPropertySymbol, nodes: PersistentSet<CFGNode<*>>,
): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
    return transformValues {
        val data = when (val data = it[PropertyAccessType.InPlace]) {
            null -> VariableWriteData(symbol, nodes)
            else -> VariableWriteData(data.value.put(symbol, nodes))
        }
        it.put(PropertyAccessType.InPlace, data)
    }
}

/**
 * Back-propagate all variable assignments as [PropertyAccessType.Captured]. This information
 * is then used when calculating visible assignments for each node.
 */
internal class FindCapturedWrites(
    private val properties: Set<FirPropertySymbol>,
) : PathAwareControlFlowGraphVisitor<PropertyAccessType, VariableWriteData>(CfgTraverseDirection.Backward) {

    override fun mergeInfo(
        a: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
        b: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
        node: CFGNode<*>,
    ): ControlFlowInfo<PropertyAccessType, VariableWriteData> {
        return a.merge(b, VariableWriteData::plus)
    }

    override fun visitEdge(
        from: CFGNode<*>, to: CFGNode<*>, metadata: Edge,
        data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
    ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
        if (metadata.label == CapturedByValue) return persistentMapOf() // Ignore CapturedByValue edges.

        return when (from) {
            // Do not propagate captured writes beyond the property declaration.
            is VariableDeclarationExitNode -> super.visitEdge(from, to, metadata, data.remove(from.fir.symbol))

            else -> super.visitEdge(from, to, metadata, data)
        }
    }

    override fun visitVariableDeclarationExitNode(
        node: VariableDeclarationExitNode,
        data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
    ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
        return when {
            node.fir.name.isSpecial || node.fir.initializer == null -> data
            else -> data.add(PropertyAccessType.Captured, node.fir.symbol, node)
        }
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
    ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
        val symbol = node.fir.calleeReference?.toResolvedPropertySymbol()?.takeIf { it in properties } ?: return data
        if (symbol.name.isSpecial) return data
        return data.add(PropertyAccessType.Captured, symbol, node)
    }
}

/**
 * Forward-propagates the most recent assignments as [PropertyAccessType.InPlace] for each node
 * and combines this with [PropertyAccessType.Captured] assignments from [FindCapturedWrites].
 */
internal class FindVisibleWrites(
    private val futureWrites: Map<CFGNode<*>, PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>>,
    private val properties: Set<FirPropertySymbol>,
) : PathAwareControlFlowGraphVisitor<PropertyAccessType, VariableWriteData>() {

    override fun mergeInfo(
        a: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
        b: ControlFlowInfo<PropertyAccessType, VariableWriteData>,
        node: CFGNode<*>,
    ): ControlFlowInfo<PropertyAccessType, VariableWriteData> {
        return a.merge(b, VariableWriteData::plus)
    }

    override fun visitEdge(
        from: CFGNode<*>, to: CFGNode<*>, metadata: Edge,
        data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
    ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
        if (metadata.label == CapturedByValue) return persistentMapOf() // Ignore CapturedByValue edges.
        var result = super.visitEdge(from, to, metadata, data)

        val fromGraph = from.owner.newNearestNonInPlaceGraph()
        val toGraph = to.owner.newNearestNonInPlaceGraph()
        when {
            // Inherit parent-graph captured writes to properties as visible writes.
            // Subgraphs should inherit their own captured writes if they are not in-place,
            // as they could be concurrently executed with themselves, and any write could be read.
            fromGraph != toGraph -> {
                val capturedWrites = futureWrites[from]
                if (capturedWrites != null) {
                    val parentInfo = super.visitEdge(from, to, metadata, capturedWrites)
                    result = result.merge(parentInfo) { a, b -> mergeInfo(a, b, to) }
                }
            }

            // Inherit non-in-place subgraph captured writes to properties as visible writes.
            from is CFGNodeWithSubgraphs<*> -> {
                for (subGraph in from.subGraphs) {
                    if (fromGraph != subGraph.newNearestNonInPlaceGraph()) {
                        val node = subGraph.enterNode
                        val nodeData = super.visitEdge(from, node, metadata, futureWrites[node] ?: continue)
                        result = result.merge(nodeData) { a, b -> mergeInfo(a, b, to) }
                    }
                }
            }
        }

        return result
    }

    override fun visitVariableDeclarationExitNode(
        node: VariableDeclarationExitNode,
        data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
    ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> =
        if (node.fir.name.isSpecial) data
        else data
            .remove(node.fir.symbol) // Remove all captured writes of property as well.
            .overwrite(node.fir.symbol, if (node.fir.initializer != null) persistentSetOf(node) else persistentSetOf())

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData>,
    ): PathAwareControlFlowInfo<PropertyAccessType, VariableWriteData> {
        val symbol = node.fir.calleeReference?.toResolvedPropertySymbol()?.takeIf { it in properties } ?: return data
        if (symbol.name.isSpecial) return data
        return data.overwrite(symbol, persistentSetOf(node))
    }
}



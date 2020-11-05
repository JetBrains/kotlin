/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

abstract class EventOccurrencesRangeInfo<E : EventOccurrencesRangeInfo<E, K>, K : Any>(
    map: PersistentMap<K, EventOccurrencesRange> = persistentMapOf()
) : ControlFlowInfo<E, K, EventOccurrencesRange>(map) {

    override fun merge(other: E): E {
        @Suppress("UNCHECKED_CAST")
        var result = this as E
        for (symbol in keys.union(other.keys)) {
            val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
            val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
            result = result.put(symbol, kind1 or kind2)
        }
        return result
    }
}

class PropertyInitializationInfo(
    map: PersistentMap<FirPropertySymbol, EventOccurrencesRange> = persistentMapOf()
) : EventOccurrencesRangeInfo<PropertyInitializationInfo, FirPropertySymbol>(map) {
    companion object {
        val EMPTY = PropertyInitializationInfo()
    }

    override val constructor: (PersistentMap<FirPropertySymbol, EventOccurrencesRange>) -> PropertyInitializationInfo =
        ::PropertyInitializationInfo

    override val empty: () -> PropertyInitializationInfo =
        ::EMPTY
}

class LocalPropertyCollector private constructor() : ControlFlowGraphVisitorVoid() {
    companion object {
        fun collect(graph: ControlFlowGraph): MutableSet<FirPropertySymbol> {
            val collector = LocalPropertyCollector()
            graph.traverse(TraverseDirection.Forward, collector)
            return collector.symbols
        }
    }

    private val symbols: MutableSet<FirPropertySymbol> = mutableSetOf()

    override fun visitNode(node: CFGNode<*>) {}

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
        symbols += node.fir.symbol
    }
}

class PathAwarePropertyInitializationInfo(
    map: PersistentMap<EdgeLabel, PropertyInitializationInfo> = persistentMapOf()
) : PathAwareControlFlowInfo<PathAwarePropertyInitializationInfo, PropertyInitializationInfo>(map) {
    companion object {
        val EMPTY = PathAwarePropertyInitializationInfo(persistentMapOf(NormalPath to PropertyInitializationInfo.EMPTY))
    }

    override val constructor: (PersistentMap<EdgeLabel, PropertyInitializationInfo>) -> PathAwarePropertyInitializationInfo =
        ::PathAwarePropertyInitializationInfo

    override val empty: () -> PathAwarePropertyInitializationInfo =
        ::EMPTY
}

class PropertyInitializationInfoCollector(private val localProperties: Set<FirPropertySymbol>) :
    ControlFlowGraphVisitor<PathAwarePropertyInitializationInfo, Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>>() {
    override fun visitNode(
        node: CFGNode<*>,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        if (data.isEmpty()) return PathAwarePropertyInitializationInfo.EMPTY
        return data.map { (label, info) -> info.applyLabel(node, label) }
            .reduce(PathAwarePropertyInitializationInfo::merge)
    }

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        val reference = node.fir.lValue as? FirResolvedNamedReference ?: return dataForNode
        val symbol = reference.resolvedSymbol as? FirPropertySymbol ?: return dataForNode
        return if (symbol !in localProperties) {
            dataForNode
        } else {
            processVariableWithAssignment(dataForNode, symbol)
        }
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: Collection<Pair<EdgeLabel, PathAwarePropertyInitializationInfo>>
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        return if (node.fir.initializer == null && node.fir.delegate == null) {
            dataForNode
        } else {
            processVariableWithAssignment(dataForNode, node.fir.symbol)
        }
    }

    fun getData(graph: ControlFlowGraph) =
        graph.collectPathAwareDataForNode(
            TraverseDirection.Forward,
            PathAwarePropertyInitializationInfo.EMPTY,
            this
        )

    private fun processVariableWithAssignment(
        dataForNode: PathAwarePropertyInitializationInfo,
        symbol: FirPropertySymbol
    ): PathAwarePropertyInitializationInfo {
        assert(dataForNode.keys.isNotEmpty())
        var resultMap = persistentMapOf<EdgeLabel, PropertyInitializationInfo>()
        // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 }
        for (label in dataForNode.keys) {
            val dataPerLabel = dataForNode[label]!!
            val existingKind = dataPerLabel[symbol] ?: EventOccurrencesRange.ZERO
            val kind = existingKind + EventOccurrencesRange.EXACTLY_ONCE
            resultMap = resultMap.put(label, dataPerLabel.put(symbol, kind))
        }
        // after (if symbol is p1):
        //   { |-> { p1 |-> PI1 + 1 }, l1 |-> { p1 |-> [1, 1], p2 |-> PI2 }
        return PathAwarePropertyInitializationInfo(resultMap)
    }
}
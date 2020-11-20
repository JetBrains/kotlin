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
import java.lang.IllegalStateException

class PropertyInitializationInfo(
    map: PersistentMap<FirPropertySymbol, EventOccurrencesRange> = persistentMapOf()
) : ControlFlowInfo<PropertyInitializationInfo, FirPropertySymbol, EventOccurrencesRange>(map) {
    companion object {
        val EMPTY = PropertyInitializationInfo()
    }

    override val constructor: (PersistentMap<FirPropertySymbol, EventOccurrencesRange>) -> PropertyInitializationInfo =
        ::PropertyInitializationInfo

    fun merge(other: PropertyInitializationInfo): PropertyInitializationInfo {
        var result = this
        for (symbol in keys.union(other.keys)) {
            val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
            val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
            result = result.put(symbol, kind1 or kind2)
        }
        return result
    }
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
) : ControlFlowInfo<PathAwarePropertyInitializationInfo, EdgeLabel, PropertyInitializationInfo>(map) {
    companion object {
        val EMPTY = PathAwarePropertyInitializationInfo(persistentMapOf(NormalPath to PropertyInitializationInfo.EMPTY))
    }

    override val constructor: (PersistentMap<EdgeLabel, PropertyInitializationInfo>) -> PathAwarePropertyInitializationInfo =
        ::PathAwarePropertyInitializationInfo

    val infoAtNormalPath: PropertyInitializationInfo
        get() = map[NormalPath] ?: PropertyInitializationInfo.EMPTY

    val hasNormalPath: Boolean
        get() = map.containsKey(NormalPath)

    fun applyLabel(node: CFGNode<*>, label: EdgeLabel): PathAwarePropertyInitializationInfo {
        if (label.isNormal) {
            // Special case: when we exit the try expression, null label means a normal path.
            // Filter out any info bound to non-null label
            // One day, if we allow multiple edges between nodes with different labels, e.g., labeling all paths in try/catch/finally,
            // instead of this kind of special handling, proxy enter/exit nodes per label are preferred.
            if (node is TryExpressionExitNode) {
                return if (hasNormalPath) {
                    constructor(persistentMapOf(NormalPath to infoAtNormalPath))
                } else {
                    /* This means no info for normal path. */
                    EMPTY
                }
            }
            // In general, null label means no additional path info, hence return `this` as-is.
            return this
        }

        val hasAbnormalLabels = map.keys.any { !it.isNormal }
        return if (hasAbnormalLabels) {
            // { |-> ... l1 |-> I1, l2 |-> I2, ... }
            //   | l1         // path exit: if the given info has non-null labels, this acts like a filtering
            // { |-> I1 }     // NB: remove the path info
            if (map.keys.contains(label)) {
                constructor(persistentMapOf(NormalPath to map[label]!!))
            } else {
                /* This means no info for the specific label. */
                EMPTY
            }
        } else {
            // { |-> ... }    // empty path info
            //   | l1         // path entry
            // { l1 -> ... }  // now, every info bound to the label
            constructor(persistentMapOf(label to infoAtNormalPath))
        }
    }

    fun merge(other: PathAwarePropertyInitializationInfo): PathAwarePropertyInitializationInfo {
        var resultMap = persistentMapOf<EdgeLabel, PropertyInitializationInfo>()
        for (label in keys.union(other.keys)) {
            // disjoint merging to preserve paths. i.e., merge the property initialization info if and only if both have the key.
            // merge({ |-> I1 }, { |-> I2, l1 |-> I3 }
            //   == { |-> merge(I1, I2), l1 |-> I3 }
            val i1 = this[label]
            val i2 = other[label]
            resultMap = when {
                i1 != null && i2 != null ->
                    resultMap.put(label, i1.merge(i2))
                i1 != null ->
                    resultMap.put(label, i1)
                i2 != null ->
                    resultMap.put(label, i2)
                else ->
                    throw IllegalStateException()
            }
        }
        return constructor(resultMap)
    }
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
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

abstract class AbstractFirPropertyInitializationChecker {
    abstract fun analyze(
        graph: ControlFlowGraph,
        reporter: DiagnosticReporter,
        data: Map<CFGNode<*>, PropertyInitializationInfo>,
        properties: Set<FirPropertySymbol>
    )

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

    class DataCollector(private val localProperties: Set<FirPropertySymbol>) :
        ControlFlowGraphVisitor<PropertyInitializationInfo, Collection<PropertyInitializationInfo>>() {
        override fun visitNode(node: CFGNode<*>, data: Collection<PropertyInitializationInfo>): PropertyInitializationInfo {
            if (data.isEmpty()) return PropertyInitializationInfo.EMPTY
            return data.reduce(PropertyInitializationInfo::merge)
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: Collection<PropertyInitializationInfo>
        ): PropertyInitializationInfo {
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
            data: Collection<PropertyInitializationInfo>
        ): PropertyInitializationInfo {
            val dataForNode = visitNode(node, data)
            return if (node.fir.initializer == null && node.fir.delegate == null) {
                dataForNode
            } else {
                processVariableWithAssignment(dataForNode, node.fir.symbol)
            }
        }

        fun getData(graph: ControlFlowGraph) =
            graph.collectDataForNode(TraverseDirection.Forward, PropertyInitializationInfo.EMPTY, DataCollector(localProperties))

        private fun processVariableWithAssignment(
            dataForNode: PropertyInitializationInfo,
            symbol: FirPropertySymbol
        ): PropertyInitializationInfo {
            val existingKind = dataForNode[symbol] ?: EventOccurrencesRange.ZERO
            val kind = existingKind + EventOccurrencesRange.EXACTLY_ONCE
            return dataForNode.put(symbol, kind)
        }
    }
}
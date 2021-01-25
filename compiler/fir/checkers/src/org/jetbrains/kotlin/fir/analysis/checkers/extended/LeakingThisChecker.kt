/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.analysis.cfa.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.resolvedSymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.LEAKING_THIS
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object LeakingThisChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val properties = declaration.declarations
            .filterIsInstance<FirProperty>()
            .map { it.symbol }
            .filter { it.fir.initializer == null && !it.fir.isLateInit }
        if (properties.isEmpty()) return

        val functions = declaration.declarations
            .filterIsInstance<FirSimpleFunction>()
            .map { it.symbol }
        val classGraph = (declaration as FirControlFlowGraphOwner).controlFlowGraphReference?.controlFlowGraph ?: return

        val data = InterproceduralCollector(properties, functions).collect(classGraph)
        GraphReporterVisitor(data, properties, reporter).reportForGraph(classGraph)
    }

    private class InterproceduralCollector(
        private val globalProperties: Collection<FirPropertySymbol>,
        private val functionsWhitelist: List<FirNamedFunctionSymbol>
    ) : InterproceduralVisitor<PathAwarePropertyUsageInfo, PropertyUsageInfo>() {
        fun collect(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwarePropertyUsageInfo> {
            return graph.collectDataForNodeInterprocedural(
                TraverseDirection.Forward,
                PathAwarePropertyUsageInfo.EMPTY,
                this,
                functionsWhitelist
            )
        }

        override fun visitNode(
            node: CFGNode<*>,
            data: MutableMap<CFGNode<*>, PathAwarePropertyUsageInfo>
        ): PathAwarePropertyUsageInfo {
            return data[node] ?: PathAwarePropertyUsageInfo.EMPTY
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: MutableMap<CFGNode<*>, PathAwarePropertyUsageInfo>
        ): PathAwarePropertyUsageInfo {
            val dataForNode = visitNode(node, data)
            val symbol = node.fir.lValue.resolvedSymbol ?: return dataForNode
            if (symbol !is FirPropertySymbol) return dataForNode
            if (symbol !in globalProperties) return dataForNode
            if (node.fir.source?.kind is FirFakeSourceElementKind) return dataForNode

            return dataForNode.update(symbol) {
                it + EventOccurrencesRange.EXACTLY_ONCE
            }
        }

        override fun visitQualifiedAccessNode(
            node: QualifiedAccessNode,
            data: MutableMap<CFGNode<*>, PathAwarePropertyUsageInfo>
        ): PathAwarePropertyUsageInfo {
            val dataForNode = visitNode(node, data)
            val symbol = node.fir.calleeReference.resolvedSymbol ?: return dataForNode
            if (symbol !is FirPropertySymbol) return dataForNode
            if (!isAccessNodeMayBeProcessed(symbol, node)) return dataForNode

            return data
                .mapNotNull { (k, v) ->
                    val resolvedSymbol = (k.fir as? FirVariableAssignment)?.lValue?.resolvedSymbol
                    if (resolvedSymbol == symbol) v else null
                }
                .firstOrNull() ?: dataForNode
        }

        private fun isAccessNodeMayBeProcessed(symbol: FirPropertySymbol, node: QualifiedAccessNode): Boolean {
            if (symbol !in globalProperties) return false
            if (node.fir.source?.kind is FirFakeSourceElementKind) return false
            if (node.owner.declaration is FirConstructor) return false

            return true
        }

        private fun PathAwarePropertyUsageInfo.update(
            vararg symbols: FirPropertySymbol,
            updater: (EventOccurrencesRange?) -> EventOccurrencesRange?,
        ): PathAwarePropertyUsageInfo {
            var resultMap = persistentMapOf<EdgeLabel, PropertyUsageInfo>()
            var changed = false
            for ((label, dataPerLabel) in this) {
                for (symbol in symbols) {
                    val v = updater.invoke(dataPerLabel[symbol])
                    if (v != null) {
                        resultMap = resultMap.put(label, dataPerLabel.put(label, v))
                        changed = true
                    } else {
                        resultMap = resultMap.put(label, dataPerLabel)
                    }
                }
            }
            return if (changed) PathAwarePropertyUsageInfo(resultMap) else this
        }

        private operator fun EventOccurrencesRange?.plus(other: EventOccurrencesRange?): EventOccurrencesRange {
            return (this ?: EventOccurrencesRange.ZERO) + (other ?: EventOccurrencesRange.ZERO)
        }
    }

    private class GraphReporterVisitor(
        val data: Map<CFGNode<*>, PathAwarePropertyUsageInfo>,
        val globalProperties: Collection<FirPropertySymbol>,
        val reporter: DiagnosticReporter
    ) : InterproceduralVisitorVoid() {
        fun reportForGraph(graph: ControlFlowGraph) {
            graph.traverseInterprocedural(TraverseDirection.Forward, this)
        }

        override fun visitNode(node: CFGNode<*>) {}

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            if (node.fir.source?.kind is FirFakeSourceElementKind) return

            val variableSymbol = node.fir.calleeReference.resolvedSymbol as? FirPropertySymbol ?: return
            val dataForNode = data[node]?.get(NormalPath)
            val variableFir = variableSymbol.fir

            if (!variableFir.isMustBeInitialized) return
            if (variableSymbol !in globalProperties) return

            if (dataForNode?.isAlwaysInitialized != true && shouldToReport(variableSymbol, node)) {
                val reportTo = node.fir.source
                reporter.report(reportTo, LEAKING_THIS)
            }
        }

        private fun shouldToReport(symbol: FirPropertySymbol, node: QualifiedAccessNode): Boolean {
            if (node.owner.declaration is FirAnonymousInitializer) return false
            if ((node.owner.declaration as? FirProperty)?.isLocal == false) return false
            if (!symbol.itWillBeInitializedLater) return false

            return true
        }

        private val FirPropertySymbol.itWillBeInitializedLater: Boolean
            get() = data.any { (k, _) ->
                k is VariableAssignmentNode && k.fir.lValue.resolvedSymbol == this
            }

        private val FirProperty.isMustBeInitialized
            get() = (getter == null || getter is FirDefaultPropertyGetter) && delegate == null

        private val PropertyUsageInfo.isAlwaysInitialized
            get() = this.all { (_, v) -> v.isAlwaysInitialized } && this.isNotEmpty()
    }

    class PropertyUsageInfo(
        map: PersistentMap<EdgeLabel, EventOccurrencesRange> = persistentMapOf()
    ) : ControlFlowInfo<PropertyUsageInfo, EdgeLabel, EventOccurrencesRange>(map) {
        override val constructor: (PersistentMap<EdgeLabel, EventOccurrencesRange>) -> PropertyUsageInfo = ::PropertyUsageInfo

        companion object {
            val empty = PropertyUsageInfo()
        }

        override fun merge(other: PropertyUsageInfo): PropertyUsageInfo {
            var result = this
            for (symbol in keys.union(other.keys)) {
                val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
                val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
                val new = kind1 or kind2
                result = result.put(symbol, new)
            }
            return result
        }

        override val empty: () -> PropertyUsageInfo = ::empty
    }

    class PathAwarePropertyUsageInfo(
        map: PersistentMap<EdgeLabel, PropertyUsageInfo> = persistentMapOf()
    ) : PathAwareControlFlowInfo<PathAwarePropertyUsageInfo, PropertyUsageInfo>(map) {
        companion object {
            val EMPTY = PathAwarePropertyUsageInfo(persistentMapOf(NormalPath to PropertyUsageInfo.empty))
        }

        override val constructor: (PersistentMap<EdgeLabel, PropertyUsageInfo>) -> PathAwarePropertyUsageInfo =
            ::PathAwarePropertyUsageInfo

        override val empty: () -> PathAwarePropertyUsageInfo =
            ::EMPTY
    }

    private val EventOccurrencesRange.isAlwaysInitialized
        get() = this == EventOccurrencesRange.AT_LEAST_ONCE
                || this == EventOccurrencesRange.EXACTLY_ONCE
                || this == EventOccurrencesRange.MORE_THAN_ONCE
}
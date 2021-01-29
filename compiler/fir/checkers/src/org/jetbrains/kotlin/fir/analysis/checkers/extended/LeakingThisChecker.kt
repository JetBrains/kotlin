/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.cfa.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.resolvedPropertySymbol
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.LEAKING_THIS
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors.MAY_BE_NOT_INITIALIZED
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

object LeakingThisChecker : FirClassChecker() {
    override fun check(declaration: FirClass<*>, context: CheckerContext, reporter: DiagnosticReporter) {
        val properties = declaration.declarations
            .filterIsInstance<FirProperty>()
            .filter { it.initializer == null && !it.isLateInit }
            .map { it.symbol }
            .toHashSet()

        if (properties.isEmpty()) return

        val functions = declaration.declarations
            .filterIsInstance<FirSimpleFunction>()
            .map { it.symbol }
            .toHashSet()
        val originalGraph = (declaration as FirControlFlowGraphOwner).controlFlowGraphReference?.controlFlowGraph ?: return
        val copiedGraph = originalGraph.copy()

        val data = InterproceduralCollector(properties, functions).collect(copiedGraph)
        GraphReporterVisitor(data, properties, reporter, functions).reportForGraph(copiedGraph)
    }

    private class InterproceduralCollector(
        private val globalProperties: HashSet<FirPropertySymbol>,
        private val functionsWhitelist: HashSet<FirNamedFunctionSymbol>
    ) : InterproceduralVisitor<PathAwarePropertyUsageInfo, PropertyUsageInfo>() {
        fun collect(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwarePropertyUsageInfo> {
            return graph.collectDataForNodeInterprocedural(
                TraverseDirection.Forward,
                PathAwarePropertyUsageInfo.EMPTY,
                this,
                functionsWhitelist
            )
        }

        override fun visitNode(node: CFGNode<*>, data: List<Pair<EdgeLabel, PathAwarePropertyUsageInfo>>): PathAwarePropertyUsageInfo {
            if (data.isEmpty()) return PathAwarePropertyUsageInfo.EMPTY
            return data.map { (label, info) -> info.applyLabel(node, label) }
                .reduce(PathAwarePropertyUsageInfo::merge)
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: List<Pair<EdgeLabel, PathAwarePropertyUsageInfo>>
        ): PathAwarePropertyUsageInfo {
            val dataForNode = visitNode(node, data)
            val symbol = node.fir.lValue.resolvedPropertySymbol ?: return dataForNode
            if (symbol !in globalProperties) return dataForNode
            if (node.fir.source?.kind is FirFakeSourceElementKind) return dataForNode

            return dataForNode.update(symbol) {
                it + EventOccurrencesRange.EXACTLY_ONCE
            }
        }

        override fun visitQualifiedAccessNode(
            node: QualifiedAccessNode,
            data: List<Pair<EdgeLabel, PathAwarePropertyUsageInfo>>
        ): PathAwarePropertyUsageInfo {
            val dataForNode = visitNode(node, data)
            val symbol = node.fir.calleeReference.resolvedPropertySymbol ?: return dataForNode
            if (!isAccessNodeMayBeProcessed(symbol, node)) return dataForNode

            return dataForNode.update(symbol) {
                it ?: EventOccurrencesRange.ZERO
            }
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
                        resultMap = resultMap.put(label, dataPerLabel.put(symbol, v))
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
        val globalProperties: HashSet<FirPropertySymbol>,
        val reporter: DiagnosticReporter,
        val functionsWhitelist: HashSet<FirNamedFunctionSymbol>
    ) : InterproceduralVisitorVoid() {
        private val reported = mutableSetOf<FirSourceElement>()

        fun reportForGraph(graph: ControlFlowGraph) {
            graph.traverseInterprocedural(TraverseDirection.Forward, this, functionsWhitelist)
        }

        override fun visitNode(node: CFGNode<*>) {}

        override fun visitFunctionCallNode(node: FunctionCallNode) {
            if (node.owner.declaration !is FirAnonymousInitializer) return
            if ((node.fir.calleeReference as FirResolvedNamedReference).resolvedSymbol.fir.symbol !in functionsWhitelist) return

            val source = node.fir.source ?: return
            if (source in reported) return
            val functionCallableSymbol = node.fir.toResolvedCallableSymbol() ?: return

            if (isReportNeeds(node)) {
                reporter.report(source, LEAKING_THIS, functionCallableSymbol)
                reported.add(source)
            }
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode) {
            if (node.fir.source?.kind is FirFakeSourceElementKind) return

            val source = node.fir.source ?: return
            if (source in reported) return

            val variableSymbol = node.fir.calleeReference.resolvedPropertySymbol ?: return
            val dataForNode = data[node]?.get(NormalPath)
            val variableFir = variableSymbol.fir

            // getting the getter enter node for process it like function call
            val getterEnterNode = node.followingNodes.find { (it.fir as? FirPropertyAccessor)?.isGetter == true }
            if (getterEnterNode != null && node.owner.declaration is FirAnonymousInitializer) {
                if (isReportNeeds(getterEnterNode)) {
                    reporter.report(source, LEAKING_THIS, variableFir.symbol)
                    reported.add(source)
                }
            }

            if (!variableFir.isMustBeInitialized) return
            if (variableSymbol !in globalProperties) return

            if (dataForNode?.isAlwaysInitialized != true && shouldToReport(variableSymbol, node)) {
                reporter.report(source, MAY_BE_NOT_INITIALIZED, variableSymbol)
                reported.add(source)
            }
        }

        private fun isReportNeeds(node: CFGNode<*>): Boolean {
            var inInlinedFunction = false

            for ((nodeKey, value) in data) {
                if (nodeKey == node) {
                    inInlinedFunction = true
                } else if (inInlinedFunction && nodeKey is QualifiedAccessNode) {
                    val property = nodeKey.fir.toResolvedCallableSymbol()
                    if (property !in globalProperties) continue
                    if (value[NormalPath]?.isAlwaysInitialized != true) {
                        return true
                    }
                } else if (nodeKey is FunctionExitNode && nodeKey.owner == node.owner) {
                    break
                }
            }

            return false
        }

        private fun shouldToReport(symbol: FirPropertySymbol, node: QualifiedAccessNode): Boolean {
            if (node.owner.declaration is FirAnonymousInitializer) return false
            if ((node.owner.declaration as? FirProperty)?.isLocal == false) return false
            if (!symbol.itWillBeInitializedLater) return false

            return true
        }

        private val FirPropertySymbol.itWillBeInitializedLater: Boolean
            get() = data.any { (k, _) ->
                k is VariableAssignmentNode && k.fir.lValue.resolvedPropertySymbol == this
            }

        private val FirProperty.isMustBeInitialized
            get() = (getter == null || getter is FirDefaultPropertyGetter) && delegate == null

        private val PropertyUsageInfo.isAlwaysInitialized
            get() = this.all { (_, v) -> v.isAlwaysInitialized } && this.isNotEmpty()
    }

    class PropertyUsageInfo(
        map: PersistentMap<FirPropertySymbol, EventOccurrencesRange> = persistentMapOf()
    ) : ControlFlowInfo<PropertyUsageInfo, FirPropertySymbol, EventOccurrencesRange>(map) {
        override val constructor: (PersistentMap<FirPropertySymbol, EventOccurrencesRange>) -> PropertyUsageInfo = ::PropertyUsageInfo

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
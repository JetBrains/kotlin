/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.fir.FirFakeSourceElementKind
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.FirSymbolOwner
import org.jetbrains.kotlin.fir.analysis.cfa.*
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClass
import org.jetbrains.kotlin.fir.analysis.checkers.isIterator
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.getChild
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.lexer.KtTokens

object UnusedChecker : FirControlFlowChecker() {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, checkerContext: CheckerContext) {
        if ((graph.declaration as? FirSymbolOwner<*>)?.getContainingClass(checkerContext)?.takeIf { !it.symbol.classId.isLocal }!= null) return
        val properties = LocalPropertyCollector.collect(graph)
        if (properties.isEmpty()) return

        val data = ValueWritesWithoutReading(properties).getData(graph)
        graph.traverse(TraverseDirection.Backward, CfaVisitor(data, reporter))
    }

    class CfaVisitor(
        val data: Map<CFGNode<*>, VariableStatusInfo>,
        val reporter: DiagnosticReporter
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val variableSymbol = (node.fir.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol ?: return
            val data = data[node]?.get(variableSymbol) ?: return
            if (data == VariableStatus.ONLY_WRITTEN_NEVER_READ) {
                // todo: report case like "a += 1" where `a` `doesn't writes` different way (special for Idea)
                val source = node.fir.lValue.source
                reporter.report(source, FirErrors.ASSIGNED_VALUE_IS_NEVER_READ)
            }
        }

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            val variableSymbol = node.fir.symbol
            if (variableSymbol.isLoopIterator) return
            val data = data[node]?.get(variableSymbol) ?: return

            when {
                data == VariableStatus.UNUSED -> {
                    if ((node.fir.initializer as? FirFunctionCall)?.isIterator != true) {
                        val source = variableSymbol.identifierSource
                        reporter.report(source, FirErrors.UNUSED_VARIABLE)
                    }
                }
                data.isRedundantInit -> {
                    val source = variableSymbol.fir.initializer?.source
                    reporter.report(source, FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT)
                }
                data == VariableStatus.ONLY_WRITTEN_NEVER_READ -> {
                    val source = variableSymbol.identifierSource
                    reporter.report(source, FirErrors.VARIABLE_NEVER_READ)
                }
                else -> {
                }
            }
        }
    }

    enum class VariableStatus(private val priority: Int) {
        READ(3),
        WRITTEN_AFTER_READ(2),
        ONLY_WRITTEN_NEVER_READ(1),
        UNUSED(0);

        var isRead = false
        var isRedundantInit = false

        fun merge(variableUseState: VariableStatus?): VariableStatus {
            val base = if (variableUseState == null || priority > variableUseState.priority) this
            else variableUseState

            return base.also {
                it.isRead = this.isRead || variableUseState?.isRead == true
                it.isRedundantInit = this.isRedundantInit && variableUseState?.isRedundantInit == true
            }
        }
    }

    class VariableStatusInfo(
        map: PersistentMap<FirPropertySymbol, VariableStatus> = persistentMapOf()
    ) : ControlFlowInfo<VariableStatusInfo, FirPropertySymbol, VariableStatus>(map) {
        companion object {
            val EMPTY = VariableStatusInfo()
        }

        override val constructor: (PersistentMap<FirPropertySymbol, VariableStatus>) -> VariableStatusInfo =
            ::VariableStatusInfo

        fun merge(other: VariableStatusInfo): VariableStatusInfo {
            var result = this
            for (symbol in keys.union(other.keys)) {
                val kind1 = this[symbol] ?: VariableStatus.UNUSED
                val kind2 = other[symbol] ?: VariableStatus.UNUSED
                val new = kind1.merge(kind2)
                result = result.put(symbol, new)
            }
            return result
        }

    }

    private class ValueWritesWithoutReading(
        private val localProperties: Set<FirPropertySymbol>
    ) : ControlFlowGraphVisitor<VariableStatusInfo, Collection<VariableStatusInfo>>() {
        fun getData(graph: ControlFlowGraph): Map<CFGNode<*>, VariableStatusInfo> {
            return graph.collectDataForNode(TraverseDirection.Backward, VariableStatusInfo.EMPTY, this)
        }

        override fun visitNode(node: CFGNode<*>, data: Collection<VariableStatusInfo>): VariableStatusInfo {
            if (data.isEmpty()) return VariableStatusInfo.EMPTY
            return data.reduce(VariableStatusInfo::merge)
        }

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: Collection<VariableStatusInfo>): VariableStatusInfo {
            val dataForNode = visitNode(node, data)
            if (node.fir.source?.kind is FirFakeSourceElementKind) return dataForNode
            val symbol = node.fir.symbol
            return when (dataForNode[symbol]) {
                null -> {
                    dataForNode.put(symbol, VariableStatus.UNUSED)
                }
                VariableStatus.ONLY_WRITTEN_NEVER_READ, VariableStatus.WRITTEN_AFTER_READ -> {
                    if (node.fir.initializer != null && dataForNode[symbol]?.isRead == true) {
                        val newData = dataForNode[symbol] ?: VariableStatus.UNUSED
                        newData.isRedundantInit = true
                        dataForNode.put(symbol, newData)
                    } else if (node.fir.initializer != null) {
                        dataForNode.put(symbol, VariableStatus.ONLY_WRITTEN_NEVER_READ)
                    } else {
                        dataForNode
                    }
                }
                VariableStatus.READ -> {
                    dataForNode.put(symbol, VariableStatus.READ)
                }
                else -> {
                    dataForNode
                }
            }
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: Collection<VariableStatusInfo>): VariableStatusInfo {
            val dataForNode = visitNode(node, data)
            val reference = node.fir.lValue as? FirResolvedNamedReference ?: return dataForNode
            val symbol = reference.resolvedSymbol as? FirPropertySymbol ?: return dataForNode
            val toPut = when {
                symbol !in localProperties -> {
                    null
                }
                dataForNode[symbol] == VariableStatus.READ -> {
                    VariableStatus.WRITTEN_AFTER_READ
                }
                dataForNode[symbol] == VariableStatus.WRITTEN_AFTER_READ -> {
                    VariableStatus.ONLY_WRITTEN_NEVER_READ
                }
                else -> {
                    VariableStatus.ONLY_WRITTEN_NEVER_READ.merge(dataForNode[symbol] ?: VariableStatus.UNUSED)
                }
            }

            toPut ?: return dataForNode

            toPut.isRead = dataForNode[symbol]?.isRead ?: false
            return dataForNode.put(symbol, toPut)
        }

        override fun visitQualifiedAccessNode(node: QualifiedAccessNode, data: Collection<VariableStatusInfo>): VariableStatusInfo {
            val dataForNode = visitNode(node, data)
            if (node.fir.source?.kind is FirFakeSourceElementKind) return dataForNode
            val reference = node.fir.calleeReference as? FirResolvedNamedReference ?: return dataForNode
            val symbol = reference.resolvedSymbol as? FirPropertySymbol ?: return dataForNode

            if (symbol !in localProperties) return dataForNode

            val status = VariableStatus.READ
            status.isRead = true
            return dataForNode.put(symbol, status)
        }
    }

    private val FirPropertySymbol.isLoopIterator
        get() = fir.initializer?.source?.kind == FirFakeSourceElementKind.DesugaredForLoop

    private val FirPropertySymbol.identifierSource: FirSourceElement?
        get() = fir.source?.getChild(KtTokens.IDENTIFIER, 0, 1)
}

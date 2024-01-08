/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.cfa.AbstractFirPropertyInitializationChecker
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isIterator
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.name.SpecialNames

object UnusedChecker : AbstractFirPropertyInitializationChecker(MppCheckerKind.Common) {
    override fun analyze(data: PropertyInitializationInfoData, reporter: DiagnosticReporter, context: CheckerContext) {
        val ownData = ValueWritesWithoutReading(data.properties).getData(data.graph)
        data.graph.traverse(CfaVisitor(ownData, reporter, context))
    }

    class CfaVisitor(
        val data: Map<CFGNode<*>, PathAwareVariableStatusInfo>,
        val reporter: DiagnosticReporter,
        val context: CheckerContext
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val variableSymbol = node.fir.calleeReference?.toResolvedPropertySymbol() ?: return
            val dataPerNode = data[node] ?: return
            for (dataPerLabel in dataPerNode.values) {
                val data = dataPerLabel[variableSymbol] ?: continue
                if (data == VariableStatus.ONLY_WRITTEN_NEVER_READ) {
                    // TODO, KT-59831: report case like "a += 1" where `a` `doesn't writes` different way (special for Idea)
                    val source = node.fir.lValue.source
                    reporter.reportOn(source, FirErrors.ASSIGNED_VALUE_IS_NEVER_READ, context)
                    // To avoid duplicate reports, stop investigating remaining paths once reported.
                    break
                }
            }
        }

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
            val variableSymbol = node.fir.symbol
            if (node.fir.source == null) return
            if (variableSymbol.isLoopIterator) return
            if (variableSymbol.name == SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) return
            val dataPerNode = data[node] ?: return

            val data = dataPerNode.values.mapNotNull { it[variableSymbol] }.reduceOrNull { acc, it -> acc.merge(it) }
            if (data != null) {
                variableSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                val variableSource = variableSymbol.source

                if (variableSource?.elementType == KtNodeTypes.DESTRUCTURING_DECLARATION) {
                    return
                }

                when {
                    data == VariableStatus.UNUSED -> {
                        when {
                            (node.fir.initializer as? FirFunctionCall)?.isIterator == true -> {}
                            node.fir.isCatchParameter == true -> {}
                            else -> {
                                reporter.reportOn(variableSource, FirErrors.UNUSED_VARIABLE, context)
                            }
                        }
                    }
                    data.isRedundantInit -> {
                        val source = variableSymbol.initializerSource
                        reporter.reportOn(source, FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT, context)
                    }
                    data == VariableStatus.ONLY_WRITTEN_NEVER_READ -> {
                        reporter.reportOn(variableSource, FirErrors.VARIABLE_NEVER_READ, context)
                    }
                    else -> {
                    }
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
                // TODO, KT-59833: is this modifying constant enum values???
                it.isRead = this.isRead || variableUseState?.isRead == true
                it.isRedundantInit = this.isRedundantInit && variableUseState?.isRedundantInit == true
            }
        }
    }

    @Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE") // K2 warning suppression, TODO: KT-62472
    class VariableStatusInfo(
        map: PersistentMap<FirPropertySymbol, VariableStatus> = persistentMapOf()
    ) : ControlFlowInfo<VariableStatusInfo, FirPropertySymbol, VariableStatus>(map) {
        companion object {
            val EMPTY = VariableStatusInfo()
        }

        override val constructor: (PersistentMap<FirPropertySymbol, VariableStatus>) -> VariableStatusInfo =
            ::VariableStatusInfo

        override fun merge(other: VariableStatusInfo): VariableStatusInfo {
            var result = this
            for (symbol in keys.union(other.keys)) {
                val kind1 = this[symbol] ?: VariableStatus.UNUSED
                val kind2 = other[symbol] ?: VariableStatus.UNUSED
                val new = kind1.merge(kind2)
                result = result.put(symbol, new)
            }
            return result
        }

        override fun plus(other: VariableStatusInfo): VariableStatusInfo =
            merge(other) // TODO, KT-59834: not sure
    }

    private class ValueWritesWithoutReading(
        private val localProperties: Set<FirPropertySymbol>
    ) : PathAwareControlFlowGraphVisitor<VariableStatusInfo>() {
        companion object {
            private val EMPTY_INFO: PathAwareVariableStatusInfo = persistentMapOf(NormalPath to VariableStatusInfo.EMPTY)
        }

        override val emptyInfo: PathAwareVariableStatusInfo
            get() = EMPTY_INFO

        fun getData(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwareVariableStatusInfo> {
            return graph.collectDataForNode(TraverseDirection.Backward, this)
        }

        private fun PathAwareVariableStatusInfo.withAnnotationsFrom(node: CFGNode<*>): PathAwareVariableStatusInfo =
            (node.fir as? FirAnnotationContainer)?.annotations?.fold(this, ::visitAnnotation) ?: this

        override fun visitNode(
            node: CFGNode<*>,
            data: PathAwareVariableStatusInfo
        ): PathAwareVariableStatusInfo =
            super.visitNode(node, data).withAnnotationsFrom(node)

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: PathAwareVariableStatusInfo
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)
            if (node.fir.source?.kind is KtFakeSourceElementKind) return dataForNode
            val symbol = node.fir.symbol
            return update(dataForNode, symbol) { prev ->
                when (prev) {
                    null -> {
                        VariableStatus.UNUSED
                    }
                    VariableStatus.ONLY_WRITTEN_NEVER_READ, VariableStatus.WRITTEN_AFTER_READ -> {
                        if (node.fir.initializer != null && prev.isRead) {
                            prev.isRedundantInit = true
                            prev
                        } else if (node.fir.initializer != null) {
                            VariableStatus.ONLY_WRITTEN_NEVER_READ
                        } else {
                            null
                        }
                    }
                    VariableStatus.READ -> {
                        VariableStatus.READ
                    }
                    else -> {
                        null
                    }
                }
            }
        }

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: PathAwareVariableStatusInfo
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)
            val symbol = node.fir.calleeReference?.toResolvedPropertySymbol() ?: return dataForNode
            return update(dataForNode, symbol) update@{ prev ->
                val toPut = when {
                    symbol !in localProperties -> {
                        null
                    }
                    prev == VariableStatus.READ -> {
                        VariableStatus.WRITTEN_AFTER_READ
                    }
                    prev == VariableStatus.WRITTEN_AFTER_READ -> {
                        VariableStatus.ONLY_WRITTEN_NEVER_READ
                    }
                    else -> {
                        VariableStatus.ONLY_WRITTEN_NEVER_READ.merge(prev ?: VariableStatus.UNUSED)
                    }
                }

                toPut ?: return@update null

                toPut.isRead = prev?.isRead ?: false
                toPut
            }
        }

        override fun visitQualifiedAccessNode(
            node: QualifiedAccessNode,
            data: PathAwareVariableStatusInfo
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)
            return visitQualifiedAccesses(dataForNode, node.fir)
        }

        private fun visitAnnotation(
            dataForNode: PathAwareVariableStatusInfo,
            annotation: FirAnnotation,
        ): PathAwareVariableStatusInfo {
            return if (annotation is FirAnnotationCall) {
                val qualifiedAccesses = annotation.argumentList.arguments.mapNotNull { it as? FirQualifiedAccessExpression }.toTypedArray()
                visitQualifiedAccesses(dataForNode, *qualifiedAccesses)
            } else {
                dataForNode
            }
        }

        private fun visitQualifiedAccesses(
            dataForNode: PathAwareVariableStatusInfo,
            vararg qualifiedAccesses: FirQualifiedAccessExpression,
        ): PathAwareVariableStatusInfo {
            fun retrieveSymbol(qualifiedAccess: FirQualifiedAccessExpression): FirPropertySymbol? {
                return qualifiedAccess.calleeReference.toResolvedPropertySymbol()?.takeIf { it in localProperties }
            }

            val symbols = qualifiedAccesses.mapNotNull { retrieveSymbol(it) }.toTypedArray()

            val status = VariableStatus.READ
            status.isRead = true

            return update(dataForNode, *symbols) { status }
        }

        override fun visitFunctionCallNode(
            node: FunctionCallNode,
            data: PathAwareVariableStatusInfo
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)

            val functionCall = node.fir
            if (functionCall is FirImplicitInvokeCall) {
                val invokeReceiver = functionCall.explicitReceiver as? FirQualifiedAccessExpression
                if (invokeReceiver != null) {
                    return visitQualifiedAccesses(dataForNode, invokeReceiver)
                }
            }

            return dataForNode
        }

        private fun update(
            pathAwareInfo: PathAwareVariableStatusInfo,
            vararg symbols: FirPropertySymbol,
            updater: (VariableStatus?) -> VariableStatus?,
        ): PathAwareVariableStatusInfo = pathAwareInfo.mutate {
            for ((label, dataPerLabel) in pathAwareInfo) {
                for (symbol in symbols) {
                    val v = updater.invoke(dataPerLabel[symbol]) ?: continue
                    it[label] = dataPerLabel.put(symbol, v)
                }
            }
        }
    }

    private val FirPropertySymbol.isLoopIterator: Boolean
        get() {
            return initializerSource?.kind == KtFakeSourceElementKind.DesugaredForLoop
        }
}

private typealias PathAwareVariableStatusInfo = PathAwareControlFlowInfo<UnusedChecker.VariableStatusInfo>

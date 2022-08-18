/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.extended

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.analysis.checkers.isIterator
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolved
import org.jetbrains.kotlin.fir.resolvedSymbol
import org.jetbrains.kotlin.fir.types.isFunctionalType
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneType

object UnusedChecker : FirControlFlowChecker() {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        if (graph.declaration?.getContainingClassSymbol(context.session)?.takeIf { !it.isLocal } != null) return
        val (properties, _) = LocalPropertyAndCapturedWriteCollector.collect(graph)
        if (properties.isEmpty()) return

        val data = ValueWritesWithoutReading(context.session, properties).getData(graph)
        graph.traverse(TraverseDirection.Backward, CfaVisitor(data, reporter, context))
    }

    class CfaVisitor(
        val data: Map<CFGNode<*>, PathAwareVariableStatusInfo>,
        val reporter: DiagnosticReporter,
        val context: CheckerContext
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
            val variableSymbol = node.fir.calleeReference.resolvedSymbol ?: return
            val dataPerNode = data[node] ?: return
            for (dataPerLabel in dataPerNode.values) {
                val data = dataPerLabel[variableSymbol] ?: continue
                if (data == VariableStatus.ONLY_WRITTEN_NEVER_READ) {
                    // todo: report case like "a += 1" where `a` `doesn't writes` different way (special for Idea)
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
            val dataPerNode = data[node] ?: return
            for (dataPerLabel in dataPerNode.values) {
                val data = dataPerLabel[variableSymbol] ?: continue

                variableSymbol.lazyResolveToPhase(FirResolvePhase.BODY_RESOLVE)
                @OptIn(SymbolInternals::class)
                val variable = variableSymbol.fir
                val variableSource = variable.source.takeIf { it?.elementType != KtNodeTypes.DESTRUCTURING_DECLARATION }
                when {
                    data == VariableStatus.UNUSED -> {
                        if ((node.fir.initializer as? FirFunctionCall)?.isIterator != true) {
                            reporter.reportOn(variableSource, FirErrors.UNUSED_VARIABLE, context)
                            break
                        }
                    }
                    data.isRedundantInit -> {
                        val source = variable.initializer?.source
                        reporter.reportOn(source, FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT, context)
                        break
                    }
                    data == VariableStatus.ONLY_WRITTEN_NEVER_READ -> {
                        reporter.reportOn(variableSource, FirErrors.VARIABLE_NEVER_READ, context)
                        break
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

        override val empty: () -> VariableStatusInfo =
            ::EMPTY

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

    }

    class PathAwareVariableStatusInfo(
        map: PersistentMap<EdgeLabel, VariableStatusInfo> = persistentMapOf()
    ) : PathAwareControlFlowInfo<PathAwareVariableStatusInfo, VariableStatusInfo>(map) {
        companion object {
            val EMPTY = PathAwareVariableStatusInfo(persistentMapOf(NormalPath to VariableStatusInfo.EMPTY))
        }

        override val constructor: (PersistentMap<EdgeLabel, VariableStatusInfo>) -> PathAwareVariableStatusInfo =
            ::PathAwareVariableStatusInfo

        override val empty: () -> PathAwareVariableStatusInfo =
            ::EMPTY
    }

    private class ValueWritesWithoutReading(
        private val session: FirSession,
        private val localProperties: Set<FirPropertySymbol>
    ) : ControlFlowGraphVisitor<PathAwareVariableStatusInfo, Collection<Pair<EdgeLabel, PathAwareVariableStatusInfo>>>() {
        fun getData(graph: ControlFlowGraph): Map<CFGNode<*>, PathAwareVariableStatusInfo> {
            return graph.collectDataForNode(TraverseDirection.Backward, PathAwareVariableStatusInfo.EMPTY, this)
        }

        override fun visitNode(
            node: CFGNode<*>,
            data: Collection<Pair<EdgeLabel, PathAwareVariableStatusInfo>>
        ): PathAwareVariableStatusInfo {
            if (data.isEmpty()) return PathAwareVariableStatusInfo.EMPTY
            val result = data.map { (label, info) -> info.applyLabel(node, label) }
                .reduce(PathAwareVariableStatusInfo::merge)
            return (node.fir as? FirAnnotationContainer)?.annotations?.fold(result, ::visitAnnotation)
                ?: result
        }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: Collection<Pair<EdgeLabel, PathAwareVariableStatusInfo>>
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
            data: Collection<Pair<EdgeLabel, PathAwareVariableStatusInfo>>
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)
            val symbol = node.fir.lValue.resolvedSymbol as? FirPropertySymbol ?: return dataForNode
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
            data: Collection<Pair<EdgeLabel, PathAwareVariableStatusInfo>>
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)
            return visitQualifiedAccesses(dataForNode, node.fir)
        }

        private fun visitAnnotation(
            dataForNode: PathAwareVariableStatusInfo,
            annotation: FirAnnotation,
        ): PathAwareVariableStatusInfo {
            return if (annotation is FirAnnotationCall) {
                val qualifiedAccesses = annotation.argumentList.arguments.mapNotNull { it as? FirQualifiedAccess }.toTypedArray()
                visitQualifiedAccesses(dataForNode, *qualifiedAccesses)
            } else {
                dataForNode
            }
        }

        private fun visitQualifiedAccesses(
            dataForNode: PathAwareVariableStatusInfo,
            vararg qualifiedAccesses: FirQualifiedAccess,
        ): PathAwareVariableStatusInfo {
            fun retrieveSymbol(qualifiedAccess: FirQualifiedAccess): FirPropertySymbol? {
                val symbol = qualifiedAccess.calleeReference.resolvedSymbol as? FirPropertySymbol ?: return null
                return if (symbol !in localProperties) null else symbol
            }

            val symbols = qualifiedAccesses.mapNotNull { retrieveSymbol(it) }.toTypedArray()

            val status = VariableStatus.READ
            status.isRead = true

            return update(dataForNode, *symbols) { status }
        }

        override fun visitFunctionCallNode(
            node: FunctionCallNode,
            data: Collection<Pair<EdgeLabel, PathAwareVariableStatusInfo>>
        ): PathAwareVariableStatusInfo {
            val dataForNode = visitNode(node, data)
            val reference = node.fir.calleeReference.resolved ?: return dataForNode
            val functionSymbol = reference.resolvedSymbol as? FirFunctionSymbol<*> ?: return dataForNode
            val symbol = if (functionSymbol.callableId.callableName.identifier == "invoke") {
                localProperties.find { it.name == reference.name && it.resolvedReturnTypeRef.coneType.isFunctionalType(session) }
            } else null
            symbol ?: return dataForNode

            val status = VariableStatus.READ
            status.isRead = true
            return update(dataForNode, symbol) { status }
        }

        private fun update(
            pathAwareInfo: PathAwareVariableStatusInfo,
            vararg symbols: FirPropertySymbol,
            updater: (VariableStatus?) -> VariableStatus?,
        ): PathAwareVariableStatusInfo {
            var resultMap = persistentMapOf<EdgeLabel, VariableStatusInfo>()
            var changed = false
            for ((label, dataPerLabel) in pathAwareInfo) {
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
            return if (changed) PathAwareVariableStatusInfo(resultMap) else pathAwareInfo
        }
    }

    private val FirPropertySymbol.isLoopIterator: Boolean
        get() {
            @OptIn(SymbolInternals::class)
            return fir.initializer?.source?.kind == KtFakeSourceElementKind.DesugaredForLoop
        }
}

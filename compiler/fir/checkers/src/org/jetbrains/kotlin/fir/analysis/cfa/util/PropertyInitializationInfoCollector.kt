/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

class PropertyInitializationInfoData(properties: Set<FirPropertySymbol>, graph: ControlFlowGraph) {
    private val data by lazy(LazyThreadSafetyMode.NONE) {
        PropertyInitializationInfoCollector(properties).getData(graph)
    }

    fun getValue(node: CFGNode<*>): PathAwarePropertyInitializationInfo {
        return data.getValue(node)
    }
}

class PropertyInitializationInfoCollector(
    private val localProperties: Set<FirPropertySymbol>,
    private val declaredVariableCollector: DeclaredVariableCollector = DeclaredVariableCollector(),
) : PathAwareControlFlowGraphVisitor<PropertyInitializationInfo>() {
    companion object {
        private val EMPTY_INFO: PathAwarePropertyInitializationInfo = persistentMapOf(NormalPath to PropertyInitializationInfo.EMPTY)
    }

    override val emptyInfo: PathAwarePropertyInitializationInfo
        get() = EMPTY_INFO

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        val symbol = node.fir.calleeReference.toResolvedPropertySymbol() ?: return dataForNode
        return if (symbol !in localProperties) {
            dataForNode
        } else {
            processVariableWithAssignment(dataForNode, symbol)
        }
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        return processVariableWithAssignment(
            dataForNode,
            node.fir.symbol,
            overwriteRange = node.fir.initializer == null && node.fir.delegate == null
        )
    }

    fun getData(graph: ControlFlowGraph) =
        graph.collectDataForNode(TraverseDirection.Forward, this)

    private fun processVariableWithAssignment(
        dataForNode: PathAwarePropertyInitializationInfo,
        symbol: FirPropertySymbol,
        overwriteRange: Boolean = false,
    ): PathAwarePropertyInitializationInfo {
        assert(dataForNode.keys.isNotEmpty())
        return if (overwriteRange)
            overwriteRange(dataForNode, symbol, EventOccurrencesRange.ZERO)
        else
            addRange(dataForNode, symbol, EventOccurrencesRange.EXACTLY_ONCE)
    }

    // --------------------------------------------------
    // Data flows of declared/assigned variables in loops
    // --------------------------------------------------

    override fun visitEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        metadata: Edge,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val result = super.visitEdge(from, to, metadata, data)
        if (!metadata.kind.isBack) return result
        val declaredVariableSymbolsInCapturedScope = when (to) {
            is LoopEnterNode -> declaredVariableCollector.declaredVariablesPerElement[to.fir]
            is LoopBlockEnterNode -> declaredVariableCollector.declaredVariablesPerElement[to.fir]
            is LoopConditionEnterNode -> declaredVariableCollector.declaredVariablesPerElement[to.loop]
            else -> return result
        }
        return declaredVariableSymbolsInCapturedScope.fold(data) { filteredData, variableSymbol ->
            removeRange(filteredData, variableSymbol)
        }
    }

    override fun visitLoopEnterNode(node: LoopEnterNode, data: PathAwarePropertyInitializationInfo): PathAwarePropertyInitializationInfo {
        declaredVariableCollector.enterCapturingStatement(node.fir)
        return visitNode(node, data)
    }

    override fun visitLoopExitNode(
        node: LoopExitNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        declaredVariableCollector.exitCapturingStatement(node.fir)
        return visitNode(node, data)
    }
}

internal fun <S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> addRange(
    pathAwareInfo: PathAwareControlFlowInfo<S>,
    key: K,
    range: EventOccurrencesRange,
): PathAwareControlFlowInfo<S> {
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    // after (if key is p1):
    //   { |-> { p1 |-> PI1 + r }, l1 |-> { p1 |-> r, p2 |-> PI2 } }
    return updateRange(pathAwareInfo, key) { existingKind -> existingKind + range }
}

private fun <S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> overwriteRange(
    pathAwareInfo: PathAwareControlFlowInfo<S>,
    key: K,
    range: EventOccurrencesRange,
): PathAwareControlFlowInfo<S> {
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    // after (if key is p1):
    //   { |-> { p1 |-> r }, l1 |-> { p1 |-> r, p2 |-> PI2 } }
    return updateRange(pathAwareInfo, key) { range }
}

private inline fun <S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> updateRange(
    pathAwareInfo: PathAwareControlFlowInfo<S>,
    key: K,
    computeNewRange: (EventOccurrencesRange) -> EventOccurrencesRange,
): PathAwareControlFlowInfo<S> {
    var resultMap = persistentMapOf<EdgeLabel, S>()
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    for ((label, dataPerLabel) in pathAwareInfo) {
        val existingKind = dataPerLabel[key] ?: EventOccurrencesRange.ZERO
        val kind = computeNewRange.invoke(existingKind)
        resultMap = resultMap.put(label, dataPerLabel.put(key, kind))
    }
    // after (if key is p1):
    //   { |-> { p1 |-> computeNewRange(PI1) }, l1 |-> { p1 |-> r, p2 |-> PI2 } }
    return resultMap
}

private fun <S : ControlFlowInfo<S, K, EventOccurrencesRange>, K : Any> removeRange(
    pathAwareInfo: PathAwareControlFlowInfo<S>,
    key: K,
): PathAwareControlFlowInfo<S> {
    var resultMap = persistentMapOf<EdgeLabel, S>()
    // before: { |-> { p1 |-> PI1 }, l1 |-> { p2 |-> PI2 } }
    for ((label, dataPerLabel) in pathAwareInfo) {
        resultMap = resultMap.put(label, dataPerLabel.remove(key))
    }
    // after (if key is p1):
    //   { |-> { }, l1 |-> { p2 |-> PI2 } }
    return resultMap
}

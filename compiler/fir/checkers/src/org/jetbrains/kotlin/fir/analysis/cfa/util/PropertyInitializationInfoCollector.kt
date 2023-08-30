/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.dispatchReceiver
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol

class PropertyInitializationInfoData(
    val properties: Set<FirPropertySymbol>,
    val receiver: FirBasedSymbol<*>?,
    val graph: ControlFlowGraph,
) {
    private val data by lazy(LazyThreadSafetyMode.NONE) {
        graph.collectDataForNode(TraverseDirection.Forward, PropertyInitializationInfoCollector(properties, receiver))
    }

    fun getValue(node: CFGNode<*>): PathAwarePropertyInitializationInfo {
        return data.getValue(node)
    }
}

class PropertyInitializationInfoCollector(
    private val localProperties: Set<FirPropertySymbol>,
    private val expectedReceiver: FirBasedSymbol<*>? = null,
    private val declaredVariableCollector: DeclaredVariableCollector = DeclaredVariableCollector(),
) : PathAwareControlFlowGraphVisitor<PropertyInitializationInfo>() {
    companion object {
        private val EMPTY_INFO: PathAwarePropertyInitializationInfo = persistentMapOf(NormalPath to PropertyInitializationInfo.EMPTY)
    }

    override val emptyInfo: PathAwarePropertyInitializationInfo
        get() = EMPTY_INFO

    // When looking for initializations of member properties, skip subgraphs of member functions;
    // all properties are assumed to be initialized there.
    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean =
        expectedReceiver == null || node !is ClassExitNode || node !== node.owner.exitNode

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        val receiver = (node.fir.dispatchReceiver?.unwrapSmartcastExpression() as? FirThisReceiverExpression)?.calleeReference?.boundSymbol
        if (receiver != expectedReceiver) return dataForNode
        val symbol = node.fir.calleeReference?.toResolvedPropertySymbol() ?: return dataForNode
        if (symbol !in localProperties) return dataForNode
        return addRange(dataForNode, symbol, EventOccurrencesRange.EXACTLY_ONCE)
    }

    override fun visitVariableDeclarationNode(
        node: VariableDeclarationNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        return when {
            expectedReceiver != null ->
                dataForNode
            node.fir.initializer == null && node.fir.delegate == null ->
                overwriteRange(dataForNode, node.fir.symbol, EventOccurrencesRange.ZERO)
            else ->
                overwriteRange(dataForNode, node.fir.symbol, EventOccurrencesRange.EXACTLY_ONCE)
        }
    }

    override fun visitPropertyInitializerExitNode(
        node: PropertyInitializerExitNode,
        data: PathAwareControlFlowInfo<PropertyInitializationInfo>
    ): PathAwareControlFlowInfo<PropertyInitializationInfo> {
        // If member property initializer is empty (there are no nodes between enter and exit node)
        //   then property is not initialized in its declaration
        // Otherwise it is
        val dataForNode = visitNode(node, data)
        if (node.firstPreviousNode is PropertyInitializerEnterNode) return dataForNode
        return overwriteRange(dataForNode, node.fir.symbol, EventOccurrencesRange.EXACTLY_ONCE)
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

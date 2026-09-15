/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.contracts.description.MarkedEventOccurrencesRange
import org.jetbrains.kotlin.contracts.description.canBeRevisited
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.calleeReference
import org.jetbrains.kotlin.fir.expressions.dispatchReceiver
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirLocalPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirVisitor

class PropertyInitializationInfoData(
    override val properties: Set<FirVariableSymbol<*>>,
    override val conditionallyInitializedProperties: Set<FirVariableSymbol<*>>,
    override val receiver: FirBasedSymbol<*>?,
    override val graph: ControlFlowGraph,
) : VariableInitializationInfoData() {
    private val data by lazy(LazyThreadSafetyMode.NONE) {
        val declaredVariablesInLoop = setMultimapOf<FirStatement, FirVariableSymbol<*>>().apply {
            // First element we visit is the graph declaration itself, and it is definitely an allowed (sub)graph.
            val collectorData = PropertyDeclarationCollector.Data(repeatable = null, allowedSubgraphs = setOf(graph))
            graph.declaration?.accept(PropertyDeclarationCollector(this), collectorData)
        }
        graph.traverseToFixedPoint(PropertyInitializationInfoCollector(properties, receiver, declaredVariablesInLoop))
    }

    override fun getValue(node: CFGNode<*>): PathAwarePropertyInitializationInfo {
        return data.getValue(node)
    }
}

class PropertyInitializationInfoCollector(
    private val localProperties: Set<FirVariableSymbol<*>>,
    private val expectedReceiver: FirBasedSymbol<*>? = null,
    private val declaredVariablesInLoop: SetMultimap<FirStatement, FirVariableSymbol<*>>,
) : EventCollectingControlFlowGraphVisitor<VariableInitializationEvent>() {
    // When looking for initializations of member properties, skip subgraphs of member functions;
    // all properties are assumed to be initialized there.
    override fun visitSubGraph(node: CFGNodeWithSubgraphs<*>, graph: ControlFlowGraph): Boolean =
        expectedReceiver == null || node !is ClassExitNode || node !== node.owner.exitNode

    override fun visitVariableAssignmentNode(
        node: VariableAssignmentNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        val symbol = toSymbolIfOurs(node.fir.dispatchReceiver, node.fir.calleeReference) ?: return dataForNode
        val range = EventOccurrencesRangeAtNode(MarkedEventOccurrencesRange.ExactlyOnce(node), mustBeLateinit = false)
        return dataForNode.addRange(symbol, range)
    }

    override fun visitQualifiedAccessNode(
        node: QualifiedAccessNode,
        data: PathAwareControlFlowInfo<VariableInitializationEvent, EventOccurrencesRangeAtNode>
    ): PathAwareControlFlowInfo<VariableInitializationEvent, EventOccurrencesRangeAtNode> {
        val dataForNode = visitNode(node, data)
        val symbol = toSymbolIfOurs(node.fir.dispatchReceiver, node.fir.calleeReference) ?: return dataForNode
        val range = EventOccurrencesRangeAtNode(MarkedEventOccurrencesRange.Zero, mustBeLateinit = true)
        return dataForNode.addRangeIfEmpty(symbol, range)
    }

    private fun toSymbolIfOurs(
        dispatchReceiver: FirExpression?,
        calleeReference: FirReference?,
    ): FirPropertySymbol? {
        val receiver = (dispatchReceiver?.unwrapSmartcastExpression() as? FirThisReceiverExpression)?.calleeReference?.boundSymbol
        val symbol = calleeReference?.toResolvedPropertySymbol() ?: return null
        return symbol.takeIf { receiver == expectedReceiver && symbol in localProperties }
    }

    override fun visitVariableDeclarationExitNode(
        node: VariableDeclarationExitNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val dataForNode = visitNode(node, data)
        return when {
            expectedReceiver != null ->
                dataForNode
            node.fir.initializer == null && node.fir.delegate == null ->
                dataForNode.removeRange(node.fir.symbol)
            else ->
                dataForNode.overwriteRange(
                    node.fir.symbol,
                    EventOccurrencesRangeAtNode(MarkedEventOccurrencesRange.ExactlyOnce(node), mustBeLateinit = false),
                )
        }
    }

    override fun visitPropertyInitializerExitNode(
        node: PropertyInitializerExitNode,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        // If member property initializer is empty (there are no nodes between enter and exit node)
        //   then property is not initialized in its declaration
        // Otherwise it is
        val dataForNode = visitNode(node, data)
        if (node.firstPreviousNode is PropertyInitializerEnterNode) return dataForNode
        return dataForNode.overwriteRange(
            node.fir.symbol,
            EventOccurrencesRangeAtNode(MarkedEventOccurrencesRange.ExactlyOnce(node), mustBeLateinit = false),
        )
    }

    override fun visitEnumEntryExitNode(
        node: EnumEntryExitNode,
        data: PathAwareControlFlowInfo<VariableInitializationEvent, EventOccurrencesRangeAtNode>,
    ): PathAwareControlFlowInfo<VariableInitializationEvent, EventOccurrencesRangeAtNode> {
        val dataForNode = visitNode(node, data)
        return dataForNode.overwriteRange(
            node.fir.symbol,
            EventOccurrencesRangeAtNode(MarkedEventOccurrencesRange.ExactlyOnce(node), mustBeLateinit = false),
        )
    }

    override fun visitEdge(
        from: CFGNode<*>,
        to: CFGNode<*>,
        metadata: Edge,
        data: PathAwarePropertyInitializationInfo
    ): PathAwarePropertyInitializationInfo {
        val result = super.visitEdge(from, to, metadata, data)
        if (!metadata.kind.isBack) return result
        val declaredVariableSymbolsInCapturedScope = when {
            from is PostponedLambdaExitNode -> declaredVariablesInLoop[from.fir.anonymousFunction]
            to is LoopEnterNode -> declaredVariablesInLoop[to.fir]
            to is LoopBlockEnterNode -> declaredVariablesInLoop[to.fir]
            to is LoopConditionEnterNode -> declaredVariablesInLoop[to.loop]
            else -> return result // the above should handle all possible back edges
        }
        return declaredVariableSymbolsInCapturedScope.fold(data) { filteredData, variableSymbol ->
            filteredData.removeRange(variableSymbol)
        }
    }
}

private class PropertyDeclarationCollector(
    val declaredVariablesInLoop: SetMultimap<FirStatement, FirVariableSymbol<*>>,
) : FirVisitor<Unit, PropertyDeclarationCollector.Data>() {
    data class Data(val repeatable: FirStatement? = null, val allowedSubgraphs: Set<ControlFlowGraph>)

    override fun visitElement(element: FirElement, data: Data) {
        when (element) {
            is FirControlFlowGraphOwner -> {
                // Only traverse elements that can have a graph when...
                // 1. They do not have a graph and never will,
                // 2. Or their graph is in the allowed set of sub-graphs.
                val elementGraph = element.controlFlowGraphReference?.controlFlowGraph
                when {
                    elementGraph != null -> if (elementGraph in data.allowedSubgraphs) {
                        element.acceptChildren(this, data.copy(allowedSubgraphs = elementGraph.subGraphs.toSet()))
                    }
                    !element.isContainerWithOwnGraph -> element.acceptChildren(this, data)
                }
            }
            else -> element.acceptChildren(this, data)
        }
    }

    override fun visitProperty(property: FirProperty, data: Data) {
        if (property.symbol is FirLocalPropertySymbol && data.repeatable != null) {
            declaredVariablesInLoop.put(data.repeatable, property.symbol)
        }
        visitElement(property, data)
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: Data) {
        visitRepeatable(whileLoop, data)
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: Data) {
        visitRepeatable(doWhileLoop, data)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: Data) {
        if (anonymousFunction.invocationKind?.canBeRevisited() == true) {
            visitRepeatable(anonymousFunction, data)
        } else {
            visitElement(anonymousFunction, data)
        }
    }

    private fun visitRepeatable(repeatable: FirStatement, data: Data) {
        visitElement(repeatable, data.copy(repeatable = repeatable))
        if (data.repeatable != null) {
            declaredVariablesInLoop.putAll(data.repeatable, declaredVariablesInLoop[repeatable])
        }
    }
}

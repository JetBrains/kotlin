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
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf
import org.jetbrains.kotlin.fir.visitors.FirVisitor

typealias PropertyInitializationEvent = FirPropertySymbol
typealias PropertyInitializationInfo = EventOccurrencesRangeInfo<PropertyInitializationEvent>
typealias PathAwarePropertyInitializationInfo = PathAwareEventOccurrencesRangeInfo<PropertyInitializationEvent>

class PropertyInitializationInfoData(
    val properties: Set<FirPropertySymbol>,
    val conditionallyInitializedProperties: Set<FirPropertySymbol>,
    val receiver: FirBasedSymbol<*>?,
    val graph: ControlFlowGraph,
) {
    private val data by lazy(LazyThreadSafetyMode.NONE) {
        val declaredVariablesInLoop = setMultimapOf<FirStatement, FirPropertySymbol>().apply {
            graph.declaration?.accept(PropertyDeclarationCollector(this), null)
        }
        graph.traverseToFixedPoint(PropertyInitializationInfoCollector(properties, receiver, declaredVariablesInLoop))
    }

    fun getValue(node: CFGNode<*>): PathAwarePropertyInitializationInfo {
        return data.getValue(node)
    }
}

class PropertyInitializationInfoCollector(
    private val localProperties: Set<FirPropertySymbol>,
    private val expectedReceiver: FirBasedSymbol<*>? = null,
    private val declaredVariablesInLoop: SetMultimap<FirStatement, FirPropertySymbol>,
) : EventCollectingControlFlowGraphVisitor<PropertyInitializationEvent>() {
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
        return dataForNode.addRange(symbol, MarkedEventOccurrencesRange.ExactlyOnce(node))
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
                dataForNode.removeRange(node.fir.symbol)
            else ->
                dataForNode.overwriteRange(node.fir.symbol, MarkedEventOccurrencesRange.ExactlyOnce(node))
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
        return dataForNode.overwriteRange(node.fir.symbol, MarkedEventOccurrencesRange.ExactlyOnce(node))
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
    val declaredVariablesInLoop: SetMultimap<FirStatement, FirPropertySymbol>
) : FirVisitor<Unit, FirStatement?>() {
    override fun visitElement(element: FirElement, data: FirStatement?) {
        element.acceptChildren(this, data)
    }

    override fun visitProperty(property: FirProperty, data: FirStatement?) {
        if (property.isLocal && data != null) {
            declaredVariablesInLoop.put(data, property.symbol)
        }
        visitElement(property, data)
    }

    override fun visitWhileLoop(whileLoop: FirWhileLoop, data: FirStatement?) {
        visitRepeatable(whileLoop, whileLoop)
    }

    override fun visitDoWhileLoop(doWhileLoop: FirDoWhileLoop, data: FirStatement?) {
        visitRepeatable(doWhileLoop, doWhileLoop)
    }

    override fun visitAnonymousFunction(anonymousFunction: FirAnonymousFunction, data: FirStatement?) {
        if (anonymousFunction.invocationKind?.canBeRevisited() == true) {
            visitRepeatable(anonymousFunction, data)
        } else {
            visitElement(anonymousFunction, data)
        }
    }

    private fun visitRepeatable(loop: FirStatement, data: FirStatement?) {
        visitElement(loop, loop)
        if (data != null) {
            declaredVariablesInLoop.putAll(data, declaredVariablesInLoop[loop])
        }
    }
}

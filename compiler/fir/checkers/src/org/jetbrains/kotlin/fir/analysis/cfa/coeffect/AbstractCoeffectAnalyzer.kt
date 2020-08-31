/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.coeffect

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.TraverseDirection
import org.jetbrains.kotlin.fir.analysis.cfa.collectDataForNode
import org.jetbrains.kotlin.fir.analysis.cfa.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.cfa.traverse
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.contracts.contextual.diagnostics.CoeffectContextVerificationError
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirWrappedArgumentExpression
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isLambda
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

abstract class CoeffectAnalyzer : FirControlFlowChecker() {

    protected fun FirFunction<*>.collectLambdaOwnerFunctions(): Map<FirAnonymousFunction, Pair<FirFunction<*>, AbstractFirBasedSymbol<*>>> {
        val lambdaToOwnerFunction = mutableMapOf<FirAnonymousFunction, Pair<FirFunction<*>, AbstractFirBasedSymbol<*>>>()
        body?.accept(LambdaToOwnerFunctionCollector(lambdaToOwnerFunction))
        return lambdaToOwnerFunction
    }

    protected fun ControlFlowGraph.collectActionsOnNodes(collector: CoeffectActionsCollector): CoeffectActionsOnNodes {
        val data = CoeffectActionsOnNodes()
        traverse(TraverseDirection.Forward, collector, data)
        return data
    }

    protected fun ControlFlowGraph.collectContextOnNodes(actionsOnNodes: CoeffectActionsOnNodes): Map<CFGNode<*>, CoeffectContextOnNodes> =
        collectDataForNode(TraverseDirection.Forward, CoeffectContextOnNodes.EMPTY, CoeffectContextResolver(actionsOnNodes))

    protected inline fun verifyCoeffectContext(
        actionsOnNodes: CoeffectActionsOnNodes,
        contextOnNodes: Map<CFGNode<*>, CoeffectContextOnNodes>,
        session: FirSession,
        reporter: (node: CFGNode<*>, error: CoeffectContextVerificationError) -> Unit
    ) {
        for ((node, actionsList) in actionsOnNodes) {
            val prevNode = node.previousCfgNodes.firstOrNull() ?: node
            val data = contextOnNodes[node] ?: continue
            val prevData = contextOnNodes[prevNode] ?: continue

            for (actions in actionsList) {
                for (verifier in actions.verifiers) {
                    val context = (if (verifier.needVerifyOnCurrentNode) data else prevData)[verifier.family]
                    val errors = verifier.verifyContext(context, session)
                    errors.forEach { reporter(node, it) }
                }
            }
        }
    }

    private class LambdaToOwnerFunctionCollector(
        val lambdaToOwnerFunction: MutableMap<FirAnonymousFunction, Pair<FirFunction<*>, AbstractFirBasedSymbol<*>>>
    ) : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            element.acceptChildren(this)
        }

        override fun visitFunctionCall(functionCall: FirFunctionCall) {
            val functionSymbol = functionCall.toResolvedCallableSymbol() ?: return
            val argumentMapping by lazy { createArgumentsMapping(functionCall)?.map { it.value to it.key }?.toMap() }
            val function = functionSymbol.fir as? FirFunction ?: return

            for (argument in functionCall.argumentList.arguments) {
                val expression = if (argument is FirWrappedArgumentExpression) argument.expression else argument
                if (expression.isLambda()) {
                    val lambdaParameterIndex = argumentMapping?.get(expression) ?: continue
                    val lambdaSymbol = function.valueParameters[lambdaParameterIndex].symbol

                    lambdaToOwnerFunction[expression] = function to lambdaSymbol
                }
            }
            super.visitFunctionCall(functionCall)
        }
    }
}


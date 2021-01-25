/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.declarations.FirControlFlowGraphOwner
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

abstract class InterproceduralVisitor<I : PathAwareControlFlowInfo<I, V>, V : ControlFlowInfo<V, *, *>> :
    ControlFlowGraphVisitor<I, MutableMap<CFGNode<*>, I>>() {
    internal fun onFunctionCall(
        direction: TraverseDirection,
        initialInfo: I,
        nodeMap: MutableMap<CFGNode<*>, I>,
        changed: MutableMap<CFGNode<*>, Boolean>,
        functionsWhitelist: Collection<FirNamedFunctionSymbol>,
        visitedSymbols: Collection<FirBasedSymbol<*>>,
        callNode: CFGNode<*>,
        visitedNodes: MutableSet<CFGNode<*>>
    ) {
        if (callNode is FunctionCallNode) {
            val functionSymbol = callNode.fir.toResolvedCallableSymbol() as? FirNamedFunctionSymbol
            val symbol = functionSymbol as? FirBasedSymbol<*> ?: return
            val function = functionSymbol.fir as? FirControlFlowGraphOwner
            val functionCfg = function?.controlFlowGraphReference?.controlFlowGraph
            if (functionSymbol in functionsWhitelist && symbol !in visitedSymbols && function != null && functionCfg != null) {
                processCfg(
                    functionCfg,
                    direction,
                    initialInfo,
                    nodeMap,
                    changed,
                    functionsWhitelist,
                    visitedSymbols + symbol,
                    callNode,
                    visitedNodes
                )
            }
        } else if (callNode is QualifiedAccessNode) {
            val property = callNode.property ?: return
            val setter = property.setter ?: return
            val getter = property.getter ?: return
            val propertySymbol = property.symbol

            if (propertySymbol !in visitedSymbols) {
                if (setter.symbol !in visitedSymbols) {
                    val setterCfg = setter.controlFlowGraph ?: return
                    processCfg(
                        setterCfg,
                        direction,
                        initialInfo,
                        nodeMap,
                        changed,
                        functionsWhitelist,
                        visitedSymbols + setter.symbol + propertySymbol,
                        callNode,
                        visitedNodes
                    )
                }
                if (getter.symbol !in visitedSymbols) {
                    val getterCfg = getter.controlFlowGraph ?: return
                    processCfg(
                        getterCfg,
                        direction,
                        initialInfo,
                        nodeMap,
                        changed,
                        functionsWhitelist,
                        visitedSymbols + getter.symbol + propertySymbol,
                        callNode,
                        visitedNodes
                    )
                }
            }
        }
    }

    private fun processCfg(
        cfg: ControlFlowGraph,
        direction: TraverseDirection,
        initialInfo: I,
        nodeMap: MutableMap<CFGNode<*>, I>,
        changed: MutableMap<CFGNode<*>, Boolean>,
        functionsWhitelist: Collection<FirNamedFunctionSymbol>,
        visitedSymbols: Collection<FirBasedSymbol<*>>,
        node: CFGNode<*>,
        visitedNodes: MutableSet<CFGNode<*>>
    ) {
        val functionEnterNode = cfg.nodes.firstOrNull() ?: return
        val functionExitNode = cfg.nodes.lastOrNull() ?: return
        val nodeAfterCall = node.followingNodes.firstOrNull() ?: return

        CFGNode.addEdgeIfNotExist(node, functionEnterNode, EdgeKind.Interprocedural, false)
        CFGNode.addEdgeIfNotExist(functionExitNode, nodeAfterCall, EdgeKind.Interprocedural, false)
        cfg.collectInterDataForNode(
            direction,
            initialInfo,
            this,
            nodeMap,
            changed,
            functionsWhitelist,
            visitedSymbols,
            visitedNodes
        )
    }

    private val FirPropertyAccessor.controlFlowGraph
        get() = symbol.fir.controlFlowGraphReference?.controlFlowGraph

    private val QualifiedAccessNode.property
        get() = (fir.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol?.fir as? FirProperty
}


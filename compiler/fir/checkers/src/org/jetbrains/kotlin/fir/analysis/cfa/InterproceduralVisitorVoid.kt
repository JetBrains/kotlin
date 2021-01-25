/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.analysis.checkers.resolvedSymbol
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.dfa.controlFlowGraph
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol

abstract class InterproceduralVisitorVoid : ControlFlowGraphVisitorVoid() {
    fun onNestedCall(
        direction: TraverseDirection,
        visitedSymbols: List<AbstractFirBasedSymbol<*>>,
        node: CFGNode<*>
    ) {
        if (node is FunctionCallNode) {
            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirNamedFunctionSymbol ?: return
            if (functionSymbol in visitedSymbols) return
            val functionCfg = functionSymbol.fir.controlFlowGraphReference?.controlFlowGraph ?: return

            functionCfg.traverseInterprocedural(direction, this, visitedSymbols + functionSymbol)
        } else if (node is QualifiedAccessNode) {
            val property = node.fir.calleeReference.resolvedSymbol?.fir as? FirProperty
            val propertySymbol = property?.symbol as? AbstractFirBasedSymbol<*>
            if (propertySymbol in visitedSymbols) return

            property?.setter?.let {
                val setterCfg = it.symbol.fir.controlFlowGraphReference?.controlFlowGraph ?: return@let
                propertySymbol ?: return@let
                setterCfg.traverseInterprocedural(direction, this, visitedSymbols + propertySymbol)
            }
            property?.getter?.let {
                val getterCfg = it.symbol.fir.controlFlowGraphReference?.controlFlowGraph ?: return@let
                propertySymbol ?: return@let
                getterCfg.traverseInterprocedural(direction, this, visitedSymbols + propertySymbol)
            }
        }
    }
}

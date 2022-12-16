/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.util

import org.jetbrains.kotlin.contracts.description.isInPlace
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.toResolvedPropertySymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirSyntheticPropertySymbol

// TODO: this should be a FIR tree visitor, not a control flow graph visitor.
class LocalPropertyAndCapturedWriteCollector private constructor() : ControlFlowGraphVisitorVoid() {
    companion object {
        fun collect(graph: ControlFlowGraph): Pair<Set<FirPropertySymbol>, Set<FirVariableAssignment>> {
            val collector = LocalPropertyAndCapturedWriteCollector()
            graph.traverse(collector)
            return collector.symbols.keys to collector.capturedWrites
        }
    }

    // Mapping from a property symbol to its declaration context
    // `true` if the (local) property is declared in the currently visited function.
    // `false` if it is declared in a lambda or a local function (inside the currently visited function).
    private val symbols: MutableMap<FirPropertySymbol, Boolean> = mutableMapOf()

    private val lambdaOrLocalFunctionStack: MutableList<FirFunction> = mutableListOf()
    private val capturedWrites: MutableSet<FirVariableAssignment> = mutableSetOf()

    override fun visitNode(node: CFGNode<*>) {}

    override fun <T> visitUnionNode(node: T) where T : CFGNode<*>, T : UnionNodeMarker {}

    override fun visitVariableDeclarationNode(node: VariableDeclarationNode) {
        symbols[node.fir.symbol] = lambdaOrLocalFunctionStack.lastOrNull() == null
    }

    override fun visitSplitPostponedLambdasNode(node: SplitPostponedLambdasNode) {
        lambdaOrLocalFunctionStack.addAll(node.lambdas)
    }

    override fun visitAnonymousFunctionExpressionNode(node: AnonymousFunctionExpressionNode) {
        lambdaOrLocalFunctionStack.add(node.fir.anonymousFunction)
    }

    override fun visitLocalFunctionDeclarationNode(node: LocalFunctionDeclarationNode) {
        lambdaOrLocalFunctionStack.add(node.fir)
    }

    override fun visitFunctionExitNode(node: FunctionExitNode) {
        lambdaOrLocalFunctionStack.remove(node.fir)
    }

    override fun visitVariableAssignmentNode(node: VariableAssignmentNode) {
        val symbol = node.fir.calleeReference.toResolvedPropertySymbol() ?: return

        // Check if this variable assignment is inside a lambda or a local function.
        if (lambdaOrLocalFunctionStack.isEmpty()) return

        // Check if the assigned variable doesn't belong to any lambda or local function.
        if (symbol !in symbols || symbols[symbol] == false) return

        // If all nested declarations are lambdas that are invoked in-place (according to the contract),
        // this variable assignment is not a captured write.
        if (lambdaOrLocalFunctionStack.all { it is FirAnonymousFunction && it.invocationKind.isInPlace }) return

        capturedWrites.add(node.fir)
    }
}

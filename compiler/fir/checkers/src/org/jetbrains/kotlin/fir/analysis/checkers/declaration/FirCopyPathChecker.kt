/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isInstanceExtension
import org.jetbrains.kotlin.fir.expressions.FirCopyFunCallExpression
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirSmartCastExpression
import org.jetbrains.kotlin.fir.expressions.FirThisReceiverExpression
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.resolvedArgumentMapping
import org.jetbrains.kotlin.fir.references.symbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CopyFunCallExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.VariableAssignmentNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol

@OptIn(SymbolInternals::class)
object FirCopyPathChecker : FirControlFlowChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(graph: ControlFlowGraph) {
        graph.nodes.forEach { node ->
            when (node) {
                is VariableAssignmentNode -> checkVariableAssignment(node.fir)
                is CopyFunCallExitNode -> checkCopyFunCall(node.fir)
                else -> {}
            }
        }
        graph.subGraphs.forEach { analyze(it) }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    fun checkVariableAssignment(expression: FirVariableAssignment) {
        val lValue = expression.lValue

        val receivers = mutableListOf<Pair<FirBasedSymbol<*>?, KtSourceElement?>>()
        val unsupported = mutableListOf<FirExpression>()
        lValue.linearizeReceivers(receivers, unsupported)

        // if any of the receivers is a copy var, everything must be a copy var
        if (receivers.any { [symbol, _] -> symbol is FirCallableSymbol<*> && symbol.fir.isCopy }) {
            receivers.forEach { [symbol, source] -> checkIsCopy(symbol, source) }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    fun checkCopyFunCall(expression: FirCopyFunCallExpression) {
        val inner = expression.originalExpression
        val symbol = inner.calleeReference.symbol
        if (symbol !is FirFunctionSymbol<*>) return
        val copyExpression = when {
            symbol.fir.isCopy && symbol.isInstanceExtension -> inner.extensionReceiver
            symbol.fir.isCopy && !symbol.isExtension -> inner.dispatchReceiver
            symbol.valueParameterSymbols.any { it.fir.isCopy } -> {
                inner.resolvedArgumentMapping
                    ?.firstNotNullOf { [e, p] -> e.takeIf { p.isCopy } }
            }
            else -> null
        }

        val receivers = mutableListOf<Pair<FirBasedSymbol<*>?, KtSourceElement?>>()
        val unsupported = mutableListOf<FirExpression>()
        copyExpression?.linearizeReceivers(receivers, unsupported)

        receivers.forEach { [symbol, source] -> checkIsCopy(symbol, source) }
        unsupported.forEach {
            reporter.reportOn(it.source, FirErrors.COPY_PATH_UNSUPPORTED_EXPRESSION)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun FirExpression.linearizeReceivers(
        receivers: MutableList<Pair<FirBasedSymbol<*>?, KtSourceElement?>>,
        unsupported: MutableList<FirExpression>,
    ) {
        when (this) {
            is FirSmartCastExpression -> originalExpression.linearizeReceivers(receivers, unsupported)
            is FirThisReceiverExpression -> {
                when (val reference = calleeReference.boundSymbol) {
                    is FirReceiverParameterSymbol -> {
                        receivers.add(0, reference.containingDeclarationSymbol to source)
                    }
                    is FirClassifierSymbol<*> -> {
                        val index = context.containingDeclarations.indexOfFirst { it == reference }
                        val current = context.containingDeclarations.getOrNull(index + 1)
                        receivers.add(0, current to source)
                    }
                    else -> {}
                }
            }
            is FirQualifiedAccessExpression -> {
                val symbol = calleeReference.symbol
                when {
                    symbol !is FirCallableSymbol<*> -> {}
                    symbol.isInstanceExtension -> extensionReceiver?.linearizeReceivers(receivers, unsupported)
                    !symbol.isExtension -> dispatchReceiver?.linearizeReceivers(receivers, unsupported)
                    else -> {}
                }
                when (this) {
                    is FirPropertyAccessExpression -> receivers.add(symbol to calleeReference.source)
                    else -> unsupported.add(this)
                }
            }
            else -> {
                unsupported.add(this)
            }
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    fun checkIsCopy(symbol: FirBasedSymbol<*>?, source: KtSourceElement?) {
        if (symbol !is FirCallableSymbol<*>) return
        if (!symbol.fir.isCopy) {
            reporter.reportOn(source, FirErrors.COPY_PATH_WRONG_STEP)
        }
    }
}

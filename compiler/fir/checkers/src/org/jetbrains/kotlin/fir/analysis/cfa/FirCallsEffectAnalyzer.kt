/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.isFunctionOrSuspendFunctionInvoke
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.util.SetMultimap
import org.jetbrains.kotlin.fir.util.setMultimapOf

object FirCallsEffectAnalyzer : FirControlFlowChecker(MppCheckerKind.Common) {
    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        // TODO, KT-59816: this is quadratic due to `graph.traverse`, surely there is a better way?
        for (subGraph in graph.subGraphs) {
            analyze(subGraph, reporter, context)
        }

        val function = graph.declaration as? FirFunction ?: return
        val contract = (function as? FirContractDescriptionOwner)?.contractDescription ?: return

        val argumentsCalledInPlace = buildMap {
            contract.effects?.forEach { firEffect ->
                val effect = firEffect.effect as? ConeCallsEffectDeclaration ?: return@forEach
                val index = effect.valueParameterReference.parameterIndex
                val typeRef = if (index < 0) function.receiverParameter?.typeRef else function.valueParameters[index].returnTypeRef
                if (typeRef?.coneType?.isSomeFunctionType(context.session) != true) return@forEach
                put(if (index < 0) function.symbol else function.valueParameters[index].symbol, firEffect)
            }
        }
        if (argumentsCalledInPlace.isEmpty()) return

        val leakedSymbols = graph.findNonInPlaceUsesOf(argumentsCalledInPlace.keys)
        val invocationData = graph.traverseToFixedPoint(
            InvocationDataCollector(argumentsCalledInPlace.keys - leakedSymbols.keys)
        )

        for ((symbol, uses) in leakedSymbols) {
            reporter.reportOn(argumentsCalledInPlace[symbol]?.source, FirErrors.LEAKED_IN_PLACE_LAMBDA, symbol, context)
            for (use in uses) {
                reporter.reportOn(use.source, FirErrors.LEAKED_IN_PLACE_LAMBDA, symbol, context)
            }
        }

        for ((symbol, firEffect) in argumentsCalledInPlace) {
            val requiredRange = (firEffect.effect as ConeCallsEffectDeclaration).kind
            val foundRange = invocationData.getValue(graph.exitNode)[NormalPath]?.get(symbol)?.withoutMarker ?: EventOccurrencesRange.ZERO
            val coercedFoundRange = foundRange.coerceToInvocationKind()
            if (foundRange !in requiredRange) {
                reporter.reportOn(firEffect.source, FirErrors.WRONG_INVOCATION_KIND, symbol, requiredRange, coercedFoundRange, context)
            }
        }
    }

    // This maps `EventOccurrencesRange` to `InvocationKind`, as the latter has fewer and different kinds.
    private fun EventOccurrencesRange.coerceToInvocationKind(): EventOccurrencesRange = when (this) {
        EventOccurrencesRange.ZERO -> EventOccurrencesRange.AT_MOST_ONCE
        EventOccurrencesRange.MORE_THAN_ONCE -> EventOccurrencesRange.AT_LEAST_ONCE
        else -> this
    }

    private fun ControlFlowGraph.findNonInPlaceUsesOf(lambdaSymbols: Set<FirBasedSymbol<*>>): SetMultimap<FirBasedSymbol<*>, FirExpression> {
        val result = setMultimapOf<FirBasedSymbol<*>, FirExpression>()

        fun FirExpression.mark() {
            val symbol = qualifiedAccessSymbol() ?: return
            if (symbol in lambdaSymbols) result.put(symbol, this)
        }

        fun ControlFlowGraph.scan(isValidScope: Boolean) {
            for (node in nodes) {
                when (node) {
                    is PropertyInitializerEnterNode -> {
                        node.fir.initializer?.mark()
                        node.fir.delegate?.mark()
                    }
                    is VariableDeclarationNode -> {
                        node.fir.initializer?.mark()
                        node.fir.delegate?.mark()
                    }
                    is VariableAssignmentNode -> {
                        node.fir.rValue.mark()
                    }
                    is FunctionCallExitNode -> {
                        node.fir.forEachArgument { arg, range ->
                            if (!isValidScope || range == null) {
                                arg.mark()
                            }
                        }
                    }
                    else -> {}
                }

                if (node is CFGNodeWithSubgraphs<*>) {
                    node.subGraphs.forEach { it.scan(isValidScope && it.declaration?.evaluatedInPlace == true) }
                }
            }
        }

        scan(isValidScope = true)
        return result
    }

    private class InvocationDataCollector(
        val lambdaSymbols: Set<FirBasedSymbol<*>>
    ) : EventCollectingControlFlowGraphVisitor<LambdaInvocationEvent>() {
        override fun visitFunctionCallExitNode(
            node: FunctionCallExitNode,
            data: PathAwareLambdaInvocationInfo
        ): PathAwareLambdaInvocationInfo {
            var dataForNode = visitNode(node, data)
            node.fir.forEachArgument { arg, range ->
                if (range != null) {
                    val symbol = arg.qualifiedAccessSymbol()?.takeIf { it in lambdaSymbols } ?: return@forEachArgument
                    dataForNode = dataForNode.addRange(symbol, range.at(node))
                }
            }
            return dataForNode
        }
    }

    private fun FirFunctionCall.forEachArgument(block: (FirExpression, EventOccurrencesRange?) -> Unit) {
        val functionSymbol = toResolvedCallableSymbol() as? FirFunctionSymbol<*>
        val effects = functionSymbol?.resolvedContractDescription?.effects?.mapNotNull { it.effect as? ConeCallsEffectDeclaration }
        explicitReceiver?.let { arg ->
            // Special hardcoded contract for `Function<N>.invoke`:
            //   callsInPlace(this, InvocationKind.EXACTLY_ONCE)
            val range = if (functionSymbol?.callableId?.isFunctionOrSuspendFunctionInvoke() == true)
                EventOccurrencesRange.EXACTLY_ONCE
            else
                effects?.find { it.valueParameterReference.parameterIndex == -1 }?.kind
            block(arg, range)
        }
        (argumentList as? FirResolvedArgumentList)?.mapping?.forEach { (value, parameter) ->
            val index = functionSymbol?.valueParameterSymbols?.indexOf(parameter.symbol) ?: -1
            block(value, if (index >= 0) effects?.find { it.valueParameterReference.parameterIndex == index }?.kind else null)
        }
    }

    private fun FirExpression.qualifiedAccessSymbol(): FirBasedSymbol<*>? =
        when (val callee = (unwrapArgument() as? FirQualifiedAccessExpression)?.calleeReference) {
            is FirResolvedNamedReference -> callee.resolvedSymbol
            is FirThisReference -> callee.boundSymbol
            else -> null
        }
}

private typealias LambdaInvocationEvent = FirBasedSymbol<*>
private typealias PathAwareLambdaInvocationInfo = PathAwareEventOccurrencesRangeInfo<LambdaInvocationEvent>

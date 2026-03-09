/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.*
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isArrayLambdaConstructor
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.locality
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.isFunctionOrSuspendFunctionInvoke
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReceiverParameterSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNonReflectFunctionType

object FirCallsEffectAnalyzer : FirControlFlowChecker(MppCheckerKind.Common) {
    context(reporter: DiagnosticReporter, context: CheckerContext)
    override fun analyze(graph: ControlFlowGraph) {
        val function = graph.declaration as? FirFunction ?: return
        val effects = (function as? FirContractDescriptionOwner)?.contractDescription?.effects.orEmpty()

        val receiverInPace = when (val receiverLocality = function.receiverParameter?.locality) {
            null -> emptyMap()
            else if receiverLocality.hasLocalContract -> mapOf(function.symbol to (receiverLocality.invocationKind ?: EventOccurrencesRange.UNKNOWN))
            else -> emptyMap()
        }
        val restInPlace =
            (function.valueParameters + function.contextParameters)
                .map { it to it.locality }
                .filter { [_, locality] -> locality.hasLocalContract }
                .associate { [param, locality] -> param.symbol to (locality.invocationKind ?: EventOccurrencesRange.UNKNOWN) }
        val argumentsCalledInPlace = receiverInPace + restInPlace

        // if (argumentsCalledInPlace.isEmpty()) return

        val leakedSymbols = FirLocalsChecker.analyze(graph)
        val callsEffects = argumentsCalledInPlace.mapValues { [symbol, _] ->
            val index = function.parameterIndex(symbol)
            effects.filter { (it.effect as? ConeCallsEffectDeclaration)?.valueParameterReference?.parameterIndex == index }
        }
        for ([symbol, uses] in leakedSymbols) {
            for (contract in callsEffects[symbol].orEmpty()) {
                reporter.reportOn(contract.source, FirErrors.LEAKED_IN_PLACE_LAMBDA, symbol)
            }
            for ([use, error] in uses) {
                reporter.reportOn(use.source, error, symbol)
            }
        }

        val invocationData = graph.traverseToFixedPoint(
            InvocationDataCollector(argumentsCalledInPlace.keys - leakedSymbols.keys, context.session)
        )
        for ([symbol, requiredRange] in argumentsCalledInPlace) {
            val foundRange = invocationData.getValue(graph.exitNode)[NormalPath]?.get(symbol)?.range?.withoutMarker ?: EventOccurrencesRange.ZERO
            val coercedFoundRange = foundRange.coerceToInvocationKind()
            if (foundRange !in requiredRange) {
                for (contract in callsEffects[symbol].orEmpty()) {
                    reporter.reportOn(contract.source, FirErrors.WRONG_INVOCATION_KIND, symbol, requiredRange, coercedFoundRange)
                }
            }
        }
    }

    private fun FirFunction.parameterIndex(symbol: FirCallableSymbol<*>): Int =
        (valueParameters + contextParameters).indexOfFirst { it.symbol == symbol }

    // This maps `EventOccurrencesRange` to `InvocationKind`, as the latter has fewer and different kinds.
    private fun EventOccurrencesRange.coerceToInvocationKind(): EventOccurrencesRange = when (this) {
        EventOccurrencesRange.ZERO -> EventOccurrencesRange.AT_MOST_ONCE
        EventOccurrencesRange.MORE_THAN_ONCE -> EventOccurrencesRange.AT_LEAST_ONCE
        else -> this
    }

    private class InvocationDataCollector(
        val lambdaSymbols: Set<FirBasedSymbol<*>>,
        val session: FirSession,
    ) : EventCollectingControlFlowGraphVisitor<LambdaInvocationEvent>() {
        override fun visitFunctionCallExitNode(
            node: FunctionCallExitNode,
            data: PathAwareLambdaInvocationInfo
        ): PathAwareLambdaInvocationInfo {
            var dataForNode = visitNode(node, data)
            node.firAsFunctionCallOrNull?.forEachArgument(session) { arg, range ->
                if (range != null) {
                    val symbol = arg.qualifiedAccessSymbol()?.takeIf { it in lambdaSymbols } ?: return@forEachArgument
                    dataForNode = dataForNode.addRange(symbol, EventOccurrencesRangeAtNode(range.at(node), mustBeLateinit = false))
                }
            }
            return dataForNode
        }
    }

    private fun FirFunctionCall.forEachArgument(session: FirSession, block: (FirExpression, EventOccurrencesRange?) -> Unit) {
        val functionSymbol = toResolvedCallableSymbol() as? FirFunctionSymbol<*>
        val effects = functionSymbol?.resolvedContractDescription?.effects?.mapNotNull { it.effect as? ConeCallsEffectDeclaration }
        val isInline = functionSymbol?.isInline == true || functionSymbol?.isArrayLambdaConstructor() == true
        explicitReceiver?.let { arg ->
            // Special hardcoded contract for `Function<N>.invoke`:
            //   callsInPlace(this, InvocationKind.EXACTLY_ONCE)
            val range = if (functionSymbol?.callableId?.isFunctionOrSuspendFunctionInvoke() == true)
                EventOccurrencesRange.EXACTLY_ONCE
            else
                effects?.find { it.valueParameterReference.parameterIndex == -1 }?.kind
            block(arg, range)
        }
        (argumentList as? FirResolvedArgumentList)?.mapping?.forEach { [value, parameter] ->
            val index = functionSymbol?.valueParameterSymbols?.indexOf(parameter.symbol) ?: -1
            val range = if (index >= 0) {
                effects?.find { it.valueParameterReference.parameterIndex == index }?.kind
                    ?: EventOccurrencesRange.UNKNOWN.takeIf {
                        parameter.isEffectivelyInline(isInline, session)
                    }
            } else {
                null
            }
            block(value, range)
        }
        contextArguments.forEachIndexed { i, expression ->
            val range = effects?.find { it.valueParameterReference.parameterIndex == i + functionSymbol.valueParameterSymbols.size }?.kind
            block(expression, range)
        }
    }

    private fun FirValueParameter.isEffectivelyInline(isInline: Boolean, session: FirSession) : Boolean {
        return isInline && !isNoinline && !isCrossinline && returnTypeRef.coneType.isNonReflectFunctionType(session)
    }

    private fun FirExpression.qualifiedAccessSymbol(): FirCallableSymbol<*>? =
        when (val callee = (unwrapArgument() as? FirQualifiedAccessExpression)?.calleeReference) {
            is FirResolvedNamedReference -> callee.resolvedSymbol as? FirCallableSymbol
            is FirThisReference -> (callee.boundSymbol as? FirReceiverParameterSymbol)?.containingDeclarationSymbol as? FirCallableSymbol
            else -> null
        }
}

private typealias LambdaInvocationEvent = FirBasedSymbol<*>
private typealias PathAwareLambdaInvocationInfo = PathAwareEventOccurrencesRangeInfo<LambdaInvocationEvent>

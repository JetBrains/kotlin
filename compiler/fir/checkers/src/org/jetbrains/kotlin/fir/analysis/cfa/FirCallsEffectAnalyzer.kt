/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class FirCallsEffectAnalyzer {

    fun analyze(function: FirFunction<*>, graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        if(function !is FirContractDescriptionOwner) return
        val functionalTypeEffects = mutableMapOf<FirBasedSymbol<*>, ConeCallsEffectDeclaration>()

        function.valueParameters.forEachIndexed { index, parameter ->
            if (isFunctionalTypeRef(function.session, parameter.returnTypeRef)) {
                val effectDeclaration = function.getParameterCallsEffectDeclaration(index)
                if (effectDeclaration != null) functionalTypeEffects[parameter.symbol] = effectDeclaration
            }
        }

        if (isFunctionalTypeRef(function.session, function.receiverTypeRef)) {
            val effectDeclaration = function.getParameterCallsEffectDeclaration(-1)
            if (effectDeclaration != null) functionalTypeEffects[function.symbol] = effectDeclaration
        }

        if (functionalTypeEffects.isEmpty()) return

        val data = graph.collectDataForNode(
            TraverseDirection.Forward,
            FunctionalTypeInvocationInfo.EMPTY,
            DataCollector(function, functionalTypeEffects.keys)
        )

        for ((symbol, effectDeclaration) in functionalTypeEffects) {
            graph.exitNode.previousCfgNodes.forEach { node ->
                val range = data.getValue(node)[symbol] ?: EventOccurrencesRange.ZERO
                if (range !in effectDeclaration.kind) {
                    symbol.fir.source?.let {
                        reporter.report(FirErrors.WRONG_INVOCATION_KIND.on(it, symbol, range))
                    }
                }
            }
        }
    }

    private class FunctionalTypeInvocationInfo(
        map: PersistentMap<FirBasedSymbol<*>, EventOccurrencesRange> = persistentMapOf(),
    ) : ControlFlowInfo<FunctionalTypeInvocationInfo, FirBasedSymbol<*>, EventOccurrencesRange>(map) {

        companion object {
            val EMPTY = FunctionalTypeInvocationInfo()
        }

//        var insideIllegalScope = false

        override val constructor: (PersistentMap<FirBasedSymbol<*>, EventOccurrencesRange>) -> FunctionalTypeInvocationInfo =
            ::FunctionalTypeInvocationInfo

        fun merge(other: FunctionalTypeInvocationInfo): FunctionalTypeInvocationInfo {
            var result = this
            for (symbol in keys.union(other.keys)) {
                val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
                val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
                result = result.put(symbol, kind1 or kind2)
            }
            return result
        }
    }

    private inner class DataCollector(
        val rootFunction: FirFunction<*>,
        val functionalTypeSymbols: Set<FirBasedSymbol<*>>
    ) : ControlFlowGraphVisitor<FunctionalTypeInvocationInfo, Collection<FunctionalTypeInvocationInfo>>() {

//        override fun visitFunctionEnterNode(
//            node: FunctionEnterNode,
//            data: Collection<FunctionalTypeInvocationInfo>
//        ): FunctionalTypeInvocationInfo {
//            return visitNode(node, data).apply {
//                val function = node.fir
//                if (!(function === rootFunction || function.isInPlaceLambda())) {
//                    insideIllegalScope = true
//                }
//            }
//        }

        override fun visitNode(node: CFGNode<*>, data: Collection<FunctionalTypeInvocationInfo>): FunctionalTypeInvocationInfo {
            if (data.isEmpty()) return FunctionalTypeInvocationInfo.EMPTY
            return data.reduce(FunctionalTypeInvocationInfo::merge)
        }

        override fun visitFunctionCallNode(
            node: FunctionCallNode,
            data: Collection<FunctionalTypeInvocationInfo>
        ): FunctionalTypeInvocationInfo {
            var dataForNode = visitNode(node, data)

            val extensionReceiver = (node.fir.extensionReceiver as? FirQualifiedAccess)?.calleeReference
            val receiverReference = extensionReceiver ?: (node.fir.explicitReceiver as? FirQualifiedAccess)?.calleeReference

            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?
            val function = functionSymbol?.fir
            val resolvedArguments = node.fir.argumentList as? FirResolvedArgumentList

            // collect data for receiver
            dataForNode = dataForNode.addDataForReference(receiverReference) {
                when {
//                    dataForNode.insideIllegalScope -> EventOccurrencesRange.UNKNOWN
                    extensionReceiver != null -> function.getParameterCallsEffect(-1)
                    functionSymbol?.callableId?.isInvoke() == true -> EventOccurrencesRange.EXACTLY_ONCE
                    else -> EventOccurrencesRange.UNKNOWN
                }
            }

            // collect data for arguments
            for (arg in node.fir.argumentList.arguments) {
                val argReference = (arg as? FirQualifiedAccess)?.calleeReference

                dataForNode = dataForNode.addDataForReference(argReference) {
                    if (function != null && resolvedArguments != null) {
                        val parameter = resolvedArguments.mapping[arg]
                        function.getParameterCallsEffect(function.valueParameters.indexOf(parameter))
                    } else EventOccurrencesRange.UNKNOWN
                }
            }

            return dataForNode
        }

        override fun visitVariableDeclarationNode(
            node: VariableDeclarationNode,
            data: Collection<FunctionalTypeInvocationInfo>
        ): FunctionalTypeInvocationInfo = handleExpressionUsage(node, data, node.fir.initializer)

        override fun visitVariableAssignmentNode(
            node: VariableAssignmentNode,
            data: Collection<FunctionalTypeInvocationInfo>
        ): FunctionalTypeInvocationInfo = handleExpressionUsage(node, data, node.fir.rValue)

        private fun handleExpressionUsage(
            node: CFGNode<*>,
            data: Collection<FunctionalTypeInvocationInfo>,
            value: FirExpression?
        ): FunctionalTypeInvocationInfo {
            return visitNode(node, data)
                .addDataForReference((value as? FirQualifiedAccess)?.calleeReference) { EventOccurrencesRange.UNKNOWN }
        }

        @OptIn(ExperimentalContracts::class)
        private fun collectDataForReference(reference: FirReference?): Boolean {
            contract {
                returns(true) implies (reference != null)
            }
            return reference != null && referenceToSymbol(reference) in functionalTypeSymbols
        }

        private fun referenceToSymbol(reference: FirReference?): FirBasedSymbol<*>? = when (reference) {
            is FirResolvedNamedReference -> reference.resolvedSymbol
            is FirThisReference -> reference.boundSymbol
            else -> null
        }

        private inline fun FunctionalTypeInvocationInfo.addDataForReference(
            reference: FirReference?,
            rangeGetter: () -> EventOccurrencesRange
        ): FunctionalTypeInvocationInfo {
            return if (collectDataForReference(reference)) addInvocationInfo(reference, rangeGetter()) else this
        }

        private fun FunctionalTypeInvocationInfo.addInvocationInfo(
            reference: FirReference,
            range: EventOccurrencesRange
        ): FunctionalTypeInvocationInfo {
            val symbol = referenceToSymbol(reference)
            return if (symbol != null) {
                val existingKind = this[symbol] ?: EventOccurrencesRange.ZERO
                val kind = existingKind + range
                this.put(symbol, kind)
            } else this
        }
    }

    private fun isFunctionalTypeRef(session: FirSession, typeRef: FirTypeRef?): Boolean {
        return typeRef?.coneType?.isBuiltinFunctionalType(session) == true
    }

    private fun FirFunction<*>?.getParameterCallsEffectDeclaration(index: Int): ConeCallsEffectDeclaration? {
        val effects = (this as? FirContractDescriptionOwner?)?.contractDescription?.effects
        val callsEffect = effects?.find { it is ConeCallsEffectDeclaration && it.valueParameterReference.parameterIndex == index }
        return callsEffect as? ConeCallsEffectDeclaration?
    }

    private fun FirFunction<*>?.getParameterCallsEffect(index: Int): EventOccurrencesRange {
        return getParameterCallsEffectDeclaration(index)?.kind ?: EventOccurrencesRange.UNKNOWN
    }

    private fun FirFunction<*>.isInPlaceLambda(): Boolean {
        return this is FirAnonymousFunction && this.isLambda && this.invocationKind != null
    }
}

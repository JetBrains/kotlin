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
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitor
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallNode
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.utils.addToStdlib.constant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class FirCallsEffectAnalyzer {

    fun analyze(function: FirFunction<*>, graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        println("${function.symbol.callableId.callableName} {")

        val functionalTypeParameters = function.valueParameters.filter { isFunctionalTypeRef(function.session, it.returnTypeRef) }
        val functionalTypeSymbols = functionalTypeParameters.map { it.symbol }
        val extensionToFunctionalType = isFunctionalTypeRef(function.session, function.receiverTypeRef)

        if (functionalTypeSymbols.isEmpty() && !extensionToFunctionalType) return

        val data =
            graph.collectDataForNode(
                TraverseDirection.Forward,
                FunctionalTypeInvocationInfo.EMPTY,
                DataCollector(functionalTypeSymbols, extensionToFunctionalType)
            )



//        if (data.values.isEmpty()) return
//        for ((key, value) in data.values.last()) {
//            println("  ${(key as? FirVariableSymbol)?.callableId?.callableName ?: "this"} : $value")
//        }

//        println("NODE")
//        graph.exitNode.incomingEdges.entries.forEach {
//            println("IN: $it")
//        }
//
//        graph.exitNode.followingNodes.forEach {
//            println("OUT: $it")
//        }

        println("}")
    }

    private class FunctionalTypeInvocationInfo(
        map: PersistentMap<FirBasedSymbol<*>, EventOccurrencesRange> = persistentMapOf()
    ) : ControlFlowInfo<FunctionalTypeInvocationInfo, FirBasedSymbol<*>, EventOccurrencesRange>(map) {

        companion object {
            val EMPTY = FunctionalTypeInvocationInfo()
        }

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
        val functionalTypeSymbols: List<FirVariableSymbol<*>>,
        val collectForThis: Boolean
    ) : ControlFlowGraphVisitor<FunctionalTypeInvocationInfo, Collection<FunctionalTypeInvocationInfo>>() {

        override fun visitNode(node: CFGNode<*>, data: Collection<FunctionalTypeInvocationInfo>): FunctionalTypeInvocationInfo {
            if (data.isEmpty()) return FunctionalTypeInvocationInfo.EMPTY
            return data.reduce(FunctionalTypeInvocationInfo::merge)
        }

        private fun referenceToSymbol(reference: FirReference?): FirBasedSymbol<*>? = when (reference) {
            is FirResolvedNamedReference -> reference.resolvedSymbol
            is FirThisReference -> reference.boundSymbol
            else -> null
        }

        @OptIn(ExperimentalContracts::class)
        private fun collectDataForReference(reference: FirReference?): Boolean {
            contract {
                returns(true) implies (reference != null)
            }
            return reference != null && (referenceToSymbol(reference) in functionalTypeSymbols || collectForThis && reference is FirThisReference)
        }

        override fun visitFunctionCallNode(
            node: FunctionCallNode,
            data: Collection<FunctionalTypeInvocationInfo>
        ): FunctionalTypeInvocationInfo {
            var dataForNode = visitNode(node, data)
            val extensionReceiver = (node.fir.extensionReceiver as? FirQualifiedAccess)?.calleeReference
            val receiverReference = (node.fir.dispatchReceiver as? FirQualifiedAccess)?.calleeReference ?: extensionReceiver

            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>
            val function = functionSymbol?.fir

            if (collectDataForReference(receiverReference)) {
                val range = when {
                    extensionReceiver != null -> getParameterCallsEffect(function, -1)
                    functionSymbol?.callableId?.isInvoke() == true -> EventOccurrencesRange.EXACTLY_ONCE
                    else -> EventOccurrencesRange.UNKNOWN
                }
                dataForNode = dataForNode.addInvocationInfo(receiverReference, range)
            }

            for ((i, arg) in node.fir.argumentList.arguments.withIndex()) {
                val argReference = (arg as? FirQualifiedAccess)?.calleeReference
                if (collectDataForReference(argReference)) {
                    val range = getParameterCallsEffect(function, i)
                    dataForNode = dataForNode.addInvocationInfo(argReference, range)
                }
            }

            return dataForNode
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

    private fun getParameterCallsEffect(function: FirFunction<*>?, index: Int): EventOccurrencesRange {
        val effects = (function as? FirSimpleFunction?)?.contractDescription?.effects
        val callsEffect = effects?.find { it is ConeCallsEffectDeclaration && it.valueParameterReference.parameterIndex == index }
        return (callsEffect as? ConeCallsEffectDeclaration?)?.kind ?: EventOccurrencesRange.UNKNOWN
    }
}

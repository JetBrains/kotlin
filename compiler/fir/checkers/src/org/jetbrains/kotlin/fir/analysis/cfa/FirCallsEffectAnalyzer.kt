/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.coneEffects
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccess
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.inference.isBuiltinFunctionalType
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.utils.addIfNotNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

object FirCallsEffectAnalyzer : FirControlFlowChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val function = (graph.declaration as? FirFunction<*>) ?: return
        if (function !is FirContractDescriptionOwner) return
        if (function.contractDescription.coneEffects?.any { it is ConeCallsEffectDeclaration } != true) return

        val functionalTypeEffects = mutableMapOf<AbstractFirBasedSymbol<*>, ConeCallsEffectDeclaration>()

        function.valueParameters.forEachIndexed { index, parameter ->
            if (parameter.returnTypeRef.isFunctionalTypeRef(function.session)) {
                val effectDeclaration = function.contractDescription.getParameterCallsEffectDeclaration(index)
                if (effectDeclaration != null) functionalTypeEffects[parameter.symbol] = effectDeclaration
            }
        }

        if (function.receiverTypeRef.isFunctionalTypeRef(function.session)) {
            val effectDeclaration = function.contractDescription.getParameterCallsEffectDeclaration(-1)
            if (effectDeclaration != null) functionalTypeEffects[function.symbol] = effectDeclaration
        }

        if (functionalTypeEffects.isEmpty()) return

        val leakedSymbols = mutableMapOf<AbstractFirBasedSymbol<*>, MutableList<FirSourceElement>>()
        graph.traverse(
            TraverseDirection.Forward,
            CapturedLambdaFinder(function),
            IllegalScopeContext(functionalTypeEffects.keys, leakedSymbols)
        )

        for ((symbol, leakedPlaces) in leakedSymbols) {
            function.contractDescription.source?.let {
                reporter.report(FirErrors.LEAKED_IN_PLACE_LAMBDA.on(it, symbol))
            }
            leakedPlaces.forEach {
                reporter.report(FirErrors.LEAKED_IN_PLACE_LAMBDA.on(it, symbol))
            }
        }

        val invocationData = graph.collectDataForNode(
            TraverseDirection.Forward,
            LambdaInvocationInfo.EMPTY,
            InvocationDataCollector(functionalTypeEffects.keys.filterTo(mutableSetOf()) { it !in leakedSymbols })
        )

        for ((symbol, effectDeclaration) in functionalTypeEffects) {
            graph.exitNode.previousCfgNodes.forEach { node ->
                val requiredRange = effectDeclaration.kind
                val foundRange = invocationData.getValue(node)[symbol] ?: EventOccurrencesRange.ZERO

                if (foundRange !in requiredRange) {
                    function.contractDescription.source?.let {
                        reporter.report(FirErrors.WRONG_INVOCATION_KIND.on(it, symbol, requiredRange, foundRange))
                    }
                }
            }
        }
    }

    private class IllegalScopeContext(
        private val functionalTypeSymbols: Set<AbstractFirBasedSymbol<*>>,
        private val leakedSymbols: MutableMap<AbstractFirBasedSymbol<*>, MutableList<FirSourceElement>>,
    ) {
        private var scopeDepth: Int = 0
        private var illegalScopeDepth: Int? = null

        val inIllegalScope: Boolean get() = illegalScopeDepth != null

        fun enterScope(legal: Boolean) {
            scopeDepth++
            if (illegalScopeDepth == null && !legal) illegalScopeDepth = scopeDepth
        }

        fun exitScope() {
            if (scopeDepth == illegalScopeDepth) illegalScopeDepth = null
            scopeDepth--
        }

        inline fun checkExpressionForLeakedSymbols(
            fir: FirExpression?,
            source: FirSourceElement? = fir?.source,
            illegalUsage: () -> Boolean = { false }
        ) {
            val symbol = referenceToSymbol(fir.toQualifiedReference())
            if (symbol != null && symbol in functionalTypeSymbols && (inIllegalScope || illegalUsage())) {
                leakedSymbols.getOrPut(symbol, ::mutableListOf).addIfNotNull(source)
            }
        }
    }

    private class CapturedLambdaFinder(val rootFunction: FirFunction<*>) : ControlFlowGraphVisitor<Unit, IllegalScopeContext>() {

        override fun visitNode(node: CFGNode<*>, data: IllegalScopeContext) {}

        override fun visitFunctionEnterNode(node: FunctionEnterNode, data: IllegalScopeContext) {
            data.enterScope(node.fir === rootFunction || node.fir.isInPlaceLambda())
        }

        override fun visitFunctionExitNode(node: FunctionExitNode, data: IllegalScopeContext) {
            data.exitScope()
        }

        override fun visitPropertyInitializerEnterNode(node: PropertyInitializerEnterNode, data: IllegalScopeContext) {
            data.enterScope(false)
            data.checkExpressionForLeakedSymbols(node.fir.initializer)
        }

        override fun visitPropertyInitializerExitNode(node: PropertyInitializerExitNode, data: IllegalScopeContext) {
            data.exitScope()
        }

        override fun visitInitBlockEnterNode(node: InitBlockEnterNode, data: IllegalScopeContext) {
            data.enterScope(false)
        }

        override fun visitInitBlockExitNode(node: InitBlockExitNode, data: IllegalScopeContext) {
            data.exitScope()
        }

        override fun visitVariableAssignmentNode(node: VariableAssignmentNode, data: IllegalScopeContext) {
            data.checkExpressionForLeakedSymbols(node.fir.rValue) { true }
        }

        override fun visitVariableDeclarationNode(node: VariableDeclarationNode, data: IllegalScopeContext) {
            data.checkExpressionForLeakedSymbols(node.fir.initializer) { true }
        }

        override fun visitFunctionCallNode(node: FunctionCallNode, data: IllegalScopeContext) {
            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?
            val contractDescription = functionSymbol?.fir?.contractDescription

            val callSource = node.fir.explicitReceiver?.source ?: node.fir.source
            data.checkExpressionForLeakedSymbols(node.fir.explicitReceiver, callSource) {
                functionSymbol?.callableId?.isInvoke() != true && contractDescription?.getParameterCallsEffect(-1) == null
            }

            for (arg in node.fir.argumentList.arguments) {
                data.checkExpressionForLeakedSymbols(arg) {
                    node.fir.getArgumentCallsEffect(arg) == null
                }
            }
        }
    }

    private class LambdaInvocationInfo(
        map: PersistentMap<FirBasedSymbol<*>, EventOccurrencesRange> = persistentMapOf(),
    ) : ControlFlowInfo<LambdaInvocationInfo, FirBasedSymbol<*>, EventOccurrencesRange>(map) {

        companion object {
            val EMPTY = LambdaInvocationInfo()
        }

        override val constructor: (PersistentMap<FirBasedSymbol<*>, EventOccurrencesRange>) -> LambdaInvocationInfo =
            ::LambdaInvocationInfo

        fun merge(other: LambdaInvocationInfo): LambdaInvocationInfo {
            var result = this
            for (symbol in keys.union(other.keys)) {
                val kind1 = this[symbol] ?: EventOccurrencesRange.ZERO
                val kind2 = other[symbol] ?: EventOccurrencesRange.ZERO
                result = result.put(symbol, kind1 or kind2)
            }
            return result
        }
    }

    private class InvocationDataCollector(
        val functionalTypeSymbols: Set<AbstractFirBasedSymbol<*>>
    ) : ControlFlowGraphVisitor<LambdaInvocationInfo, Collection<LambdaInvocationInfo>>() {

        override fun visitNode(node: CFGNode<*>, data: Collection<LambdaInvocationInfo>): LambdaInvocationInfo {
            if (data.isEmpty()) return LambdaInvocationInfo.EMPTY
            return data.reduce(LambdaInvocationInfo::merge)
        }

        override fun visitFunctionCallNode(
            node: FunctionCallNode,
            data: Collection<LambdaInvocationInfo>
        ): LambdaInvocationInfo {
            var dataForNode = visitNode(node, data)

            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?
            val contractDescription = functionSymbol?.fir?.contractDescription

            dataForNode = dataForNode.checkReference(node.fir.explicitReceiver.toQualifiedReference()) {
                when {
                    functionSymbol?.callableId?.isInvoke() == true -> EventOccurrencesRange.EXACTLY_ONCE
                    else -> contractDescription.getParameterCallsEffect(-1) ?: EventOccurrencesRange.UNKNOWN
                }
            }

            for (arg in node.fir.argumentList.arguments) {
                dataForNode = dataForNode.checkReference(arg.toQualifiedReference()) {
                    node.fir.getArgumentCallsEffect(arg) ?: EventOccurrencesRange.ZERO
                }
            }

            return dataForNode
        }

        @OptIn(ExperimentalContracts::class)
        private fun collectDataForReference(reference: FirReference?): Boolean {
            contract {
                returns(true) implies (reference != null)
            }
            return reference != null && referenceToSymbol(reference) in functionalTypeSymbols
        }

        private inline fun LambdaInvocationInfo.checkReference(
            reference: FirReference?,
            rangeGetter: () -> EventOccurrencesRange
        ): LambdaInvocationInfo {
            return if (collectDataForReference(reference)) addInvocationInfo(reference, rangeGetter()) else this
        }

        private fun LambdaInvocationInfo.addInvocationInfo(
            reference: FirReference,
            range: EventOccurrencesRange
        ): LambdaInvocationInfo {
            val symbol = referenceToSymbol(reference)
            return if (symbol != null) {
                val existingKind = this[symbol] ?: EventOccurrencesRange.ZERO
                val kind = existingKind + range
                this.put(symbol, kind)
            } else this
        }
    }

    private fun FirTypeRef?.isFunctionalTypeRef(session: FirSession): Boolean {
        return this?.coneTypeSafe<ConeKotlinType>()?.isBuiltinFunctionalType(session) == true
    }

    private val FirFunction<*>.contractDescription: FirContractDescription?
        get() = (this as? FirContractDescriptionOwner)?.contractDescription

    private fun FirContractDescription?.getParameterCallsEffectDeclaration(index: Int): ConeCallsEffectDeclaration? {
        val effects = this?.coneEffects
        val callsEffect = effects?.find { it is ConeCallsEffectDeclaration && it.valueParameterReference.parameterIndex == index }
        return callsEffect as? ConeCallsEffectDeclaration?
    }

    private fun FirFunctionCall.getArgumentCallsEffect(arg: FirExpression): EventOccurrencesRange? {
        val function = (this.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?)?.fir
        val contractDescription = function?.contractDescription
        val resolvedArguments = argumentList as? FirResolvedArgumentList

        return if (function != null && resolvedArguments != null) {
            val parameter = resolvedArguments.mapping[arg]
            contractDescription.getParameterCallsEffect(function.valueParameters.indexOf(parameter))
        } else null
    }

    private fun FirContractDescription?.getParameterCallsEffect(index: Int): EventOccurrencesRange? {
        return getParameterCallsEffectDeclaration(index)?.kind
    }

    private fun FirFunction<*>.isInPlaceLambda(): Boolean {
        return this is FirAnonymousFunction && this.isLambda && this.invocationKind != null
    }

    private fun FirExpression?.toQualifiedReference(): FirReference? = (this as? FirQualifiedAccess)?.calleeReference

    private fun referenceToSymbol(reference: FirReference?): AbstractFirBasedSymbol<*>? = when (reference) {
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirThisReference -> reference.boundSymbol
        else -> null
    }
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.isDefinitelyVisited
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.description.ConeCalledOnEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.description.ConeCallsEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitorVoid
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallNode
import org.jetbrains.kotlin.fir.resolve.dfa.contracts.createArgumentsMapping
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.utils.addIfNotNull

object FirCalledOnAnalyzer : FirControlFlowChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val function = graph.declaration as? FirFunction<*> ?: return
        val contractDescription = function.contractDescription ?: return
        val effects = contractDescription.effects ?: return
        if (effects.none { it is ConeCalledOnEffectDeclaration }) return

        val definitelyInvokedLambdas = mutableSetOf<Int>()
        effects.forEach {
            if (it is ConeCallsEffectDeclaration && it.kind.isDefinitelyVisited()) {
                definitelyInvokedLambdas += it.valueParameterReference.parameterIndex
            }
        }

        val requiredValueSymbols = mutableMapOf<AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>()
        for (effect in effects) {
            if (effect !is ConeCalledOnEffectDeclaration) continue
            val lambdaSymbol = function.getParameterSymbol(effect.lambda.parameterIndex)

            if (effect.lambda.parameterIndex in definitelyInvokedLambdas) {
                requiredValueSymbols[lambdaSymbol] = function.getParameterSymbol(effect.value.parameterIndex)
            } else {
                contractDescription.source?.let { source ->
                    reporter.report(FirErrors.NOT_DEFINITELY_INVOKED_IN_PLACE_LAMBDA.on(source, lambdaSymbol))
                }
            }
        }
        if (requiredValueSymbols.isEmpty()) return

        val wrongInvocationSources = mutableMapOf<AbstractFirBasedSymbol<*>, MutableList<FirSourceElement>>()
        graph.traverse(TraverseDirection.Forward, LambdaInvocationFinder(requiredValueSymbols, wrongInvocationSources))

        for ((lambdaSymbol, invocationSources) in wrongInvocationSources) {
            contractDescription.source?.let {
                reporter.report(FirErrors.WRONG_CALLED_ON_VALUE.on(it))
            }

            val requiredSymbol = requiredValueSymbols.getValue(lambdaSymbol)
            invocationSources.forEach {
                reporter.report(FirErrors.WRONG_INVOCATION_VALUE.on(it, lambdaSymbol, requiredSymbol))
            }
        }
    }

    private class LambdaInvocationFinder(
        val requiredValueSymbols: Map<AbstractFirBasedSymbol<*>, AbstractFirBasedSymbol<*>>,
        val wrongInvocationSources: MutableMap<AbstractFirBasedSymbol<*>, MutableList<FirSourceElement>>
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitFunctionCallNode(node: FunctionCallNode) {
            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>? ?: return
            val function = functionSymbol.fir
            val receiver = node.fir.explicitReceiver
            val arguments = node.fir.argumentList.arguments
            val resolvedArguments = node.fir.argumentList as? FirResolvedArgumentList ?: return

            val parameterToIndex by lazy { function.valueParameters.withIndex().map { it.value to it.index }.toMap() }
            val argumentMapping by lazy { createArgumentsMapping(node.fir) }

            fun getInvocationValue(arg: FirExpression): AbstractFirBasedSymbol<*>? {
                val parameterIndex = parameterToIndex[resolvedArguments.mapping[arg]] ?: -1
                val calledOnParameter = function.contractDescription.getParameterCalledOnEffectDeclaration(parameterIndex)?.value

                return if (calledOnParameter != null) {
                    argumentMapping?.get(calledOnParameter.parameterIndex)?.toResolvedCallableSymbol()
                } else null
            }

            checkInvocationValue(receiver, node.fir.source) {
                if (functionSymbol.callableId.isInvoke()) {
                    arguments.firstOrNull()?.toResolvedCallableSymbol()
                } else getInvocationValue(it)
            }

            for (argument in arguments) {
                checkInvocationValue(argument, argument.source, ::getInvocationValue)
            }
        }

        fun checkInvocationValue(
            expression: FirExpression?,
            source: FirSourceElement?,
            calledOnGetter: (FirExpression) -> AbstractFirBasedSymbol<*>?
        ) {
            val symbol = expression?.toResolvedCallableSymbol() ?: return
            val requiredSymbol = requiredValueSymbols[symbol] ?: return

            if (calledOnGetter(expression) != requiredSymbol) {
                wrongInvocationSources.getOrPut(symbol) { mutableListOf() }.addIfNotNull(source)
            }
        }
    }

    private fun FirContractDescription?.getParameterCalledOnEffectDeclaration(index: Int): ConeCalledOnEffectDeclaration? {
        val callsEffect = this?.effects?.find { it is ConeCalledOnEffectDeclaration && it.lambda.parameterIndex == index }
        return callsEffect as? ConeCalledOnEffectDeclaration?
    }

    private val FirFunction<*>.contractDescription: FirContractDescription?
        get() = (this as? FirContractDescriptionOwner)?.contractDescription

    private fun FirFunction<*>.getParameterSymbol(index: Int): AbstractFirBasedSymbol<*> {
        return if (index == -1) this.symbol else this.valueParameters[index].symbol
    }
}
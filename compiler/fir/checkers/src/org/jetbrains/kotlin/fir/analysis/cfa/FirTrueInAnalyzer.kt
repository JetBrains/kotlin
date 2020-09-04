/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.FirEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.coneEffects
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraphVisitorVoid
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.FunctionCallNode
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addIfNotNull

object FirTrueInAnalyzer : AbstractOperationFlowBasedChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, checkerContext: CheckerContext) {
        val function = graph.declaration as? FirFunction<*> ?: return

        val firEffects = function.contractDescription?.effects ?: return
        if (firEffects.none { it.effect is ConeTrueInEffectDeclaration }) return

        val requiredEffects = mutableMapOf<AbstractFirBasedSymbol<*>, MutableList<FirEffectDeclaration>>()
        for (firEffect in firEffects) {
            val effect = firEffect.effect as? ConeTrueInEffectDeclaration ?: continue
            val symbol = function.getParameterSymbol(effect.target.parameterIndex)
            requiredEffects.getOrPut(symbol) { mutableListOf() } += firEffect
        }
        if (requiredEffects.isEmpty()) return

        val graphRef = function.controlFlowGraphReference as FirControlFlowGraphReferenceImpl
        val dataFlowInfo = graphRef.dataFlowInfo ?: return
        val operationFlowInfo = graph.collectOperationFlowInfo(function.session)

        val possibleFalseInvocationEffects = mutableSetOf<FirEffectDeclaration>()
        graph.traverse(
            TraverseDirection.Forward,
            LambdaInvocationFinder(function, requiredEffects, dataFlowInfo, operationFlowInfo, reporter, possibleFalseInvocationEffects)
        )

        possibleFalseInvocationEffects.forEach { effect ->
            effect.source?.let { reporter.report(FirErrors.POSSIBLE_FALSE_INVOCATION_CONDITION.on(it)) }
        }
    }

    private class LambdaInvocationFinder(
        val function: FirFunction<*>,
        val requiredEffects: Map<AbstractFirBasedSymbol<*>, List<FirEffectDeclaration>>,
        val dataFlowInfo: DataFlowInfo,
        val operationFlowInfo: DataFlowInfo,
        val reporter: DiagnosticReporter,
        val possibleFalseInvocationEffects: MutableSet<FirEffectDeclaration>
    ) : ControlFlowGraphVisitorVoid() {
        override fun visitNode(node: CFGNode<*>) {}

        override fun visitFunctionCallNode(node: FunctionCallNode) {
            val functionSymbol = node.fir.toResolvedCallableSymbol() as? FirFunctionSymbol<*>?
            val contractDescription = functionSymbol?.fir?.contractDescription

            checkReference(node.fir.explicitReceiver.toQualifiedReference(), node) {
                functionSymbol?.callableId?.isInvoke() == true || contractDescription.getParameterCallsEffect(-1) != null
            }

            for (arg in node.fir.argumentList.arguments) {
                checkReference(arg.toQualifiedReference(), node) {
                    node.fir.getArgumentCallsEffect(arg) != null
                }
            }
        }

        private inline fun checkReference(reference: FirReference?, node: CFGNode<*>, checkCondition: () -> Boolean = { true }) {
            val symbol = referenceToSymbol(reference) ?: return
            val firEffects = requiredEffects[symbol]
            if (firEffects == null || !checkCondition()) return

            for (firEffect in firEffects) {
                val effect = firEffect.effect as? ConeTrueInEffectDeclaration ?: continue
                if (!effect.condition.checkTruthOnNode(node, function)) {
                    possibleFalseInvocationEffects.addIfNotNull(firEffect)

                    node.fir.source?.let {
                        reporter.report(FirErrors.FALSE_LAMBDA_INVOCATION_CONDITION.on(it, symbol))
                    }
                }
            }
        }

        private fun ConeBooleanExpression.checkTruthOnNode(node: CFGNode<*>, function: FirFunction<*>): Boolean {
            return when (this) {
                is ConeIsInstancePredicate -> {
                    !isNegated && function.checkParameterType(arg.parameterIndex, type, node, dataFlowInfo)
                }
                is ConeIsNullPredicate -> {
                    val anyType = function.session.builtinTypes.anyType
                    isNegated && function.checkParameterType(arg.parameterIndex, anyType.type, node, dataFlowInfo)
                }
                is ConeBinaryLogicExpression -> {
                    kind == LogicOperationKind.AND && left.checkTruthOnNode(node, function) && right.checkTruthOnNode(node, function)
                }
                is ConeLogicalNot -> {
                    val parameterRef = arg as? ConeBooleanValueParameterReference ?: return false
                    function.checkParameterTruth(parameterRef, Operation.EqFalse, node)
                }
                is ConeBooleanValueParameterReference -> {
                    function.checkParameterTruth(this, Operation.EqTrue, node)
                }
                else -> false
            }
        }

        private fun FirFunction<*>.checkParameterTruth(
            parameterRef: ConeBooleanValueParameterReference,
            requiredOperation: Operation,
            node: CFGNode<*>
        ): Boolean {
            val symbol = getParameterSymbol(parameterRef.parameterIndex)
            val flow = operationFlowInfo.flowOnNodes[node] as? PersistentOperationFlow ?: return false
            val realVariable = operationFlowInfo.variableStorage.getOrCreateRealVariable(flow, symbol, symbol.fir) ?: return false
            return requiredOperation in flow.getApprovedOperations(realVariable)
        }

        private fun FirFunction<*>.checkParameterType(
            parameterIndex: Int,
            requiredType: ConeKotlinType,
            node: CFGNode<*>,
            dataFlowInfo: DataFlowInfo
        ): Boolean {
            val symbol = getParameterSymbol(parameterIndex)
            val flow = dataFlowInfo.flowOnNodes[node] as? PersistentFlow ?: return false
            val realVariable = dataFlowInfo.variableStorage.getOrCreateRealVariable(flow, symbol, symbol.fir) ?: return false

            val types = mutableListOf<ConeKotlinType>().apply {
                addAll(flow.getTypeStatement(realVariable)?.exactType ?: emptySet())
                addIfNotNull(getParameterType(symbol))
            }

            val typeContext = session.typeContext
            val parameterType = ConeTypeIntersector.intersectTypes(typeContext, types)
            return AbstractTypeChecker.isSubtypeOf(typeContext, parameterType, requiredType)
        }
    }

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

    private fun FirFunction<*>.getParameterType(symbol: AbstractFirBasedSymbol<*>): ConeKotlinType? {
        return (if (this.symbol == symbol) receiverTypeRef else valueParameters.find { it.symbol == symbol }?.returnTypeRef)?.coneType
    }

    private fun FirFunction<*>.getParameterSymbol(index: Int): AbstractFirBasedSymbol<*> {
        return if (index == -1) this.symbol else this.valueParameters[index].symbol
    }

    private val FirFunction<*>.contractDescription: FirContractDescription?
        get() = (this as? FirContractDescriptionOwner)?.contractDescription

    private fun FirExpression?.toQualifiedReference(): FirReference? = (this as? FirQualifiedAccess)?.calleeReference

    private fun referenceToSymbol(reference: FirReference?): AbstractFirBasedSymbol<*>? = when (reference) {
        is FirResolvedNamedReference -> reference.resolvedSymbol
        is FirThisReference -> reference.boundSymbol
        else -> null
    }
}
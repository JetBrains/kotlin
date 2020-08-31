/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa.coeffect

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectContextActions
import org.jetbrains.kotlin.fir.contracts.contextual.CoeffectFamily
import org.jetbrains.kotlin.fir.contracts.contextual.declaration.CoeffectActionExtractors
import org.jetbrains.kotlin.fir.contracts.FirContractDescription
import org.jetbrains.kotlin.fir.contracts.effects
import org.jetbrains.kotlin.fir.declarations.FirAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.isLambda
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.*
import org.jetbrains.kotlin.fir.resolve.isInvoke
import org.jetbrains.kotlin.fir.contracts.contextual.declaration.ConeCoeffectEffectDeclaration
import org.jetbrains.kotlin.fir.contracts.contextual.declaration.ConeLambdaCoeffectEffectDeclaration
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol

abstract class CoeffectActionsCollector(
    val lambdaToOwnerFunction: Map<FirAnonymousFunction, Pair<FirFunction<*>, AbstractFirBasedSymbol<*>>>
) : ControlFlowGraphVisitor<Unit, CoeffectActionsOnNodes>() {

    abstract fun collectFamilyActions(family: CoeffectFamily): Boolean

    override fun visitNode(node: CFGNode<*>, data: CoeffectActionsOnNodes) {}

    override fun visitFunctionCallNode(node: FunctionCallNode, data: CoeffectActionsOnNodes) {
        val functionSymbol = node.fir.toResolvedCallableSymbol() ?: return

        if (functionSymbol.callableId.isInvoke()) {
            val receiverSymbol = node.fir.explicitReceiver?.toResolvedCallableSymbol() ?: return
            val function = (node.owner.enterNode as? FunctionEnterNode)?.fir ?: return

            collectLambdaCoeffectActions(node, function, receiverSymbol, data) { onOwnerCall?.extractActions(node.fir) }
        } else collectCoeffectActions(node, data) { onOwnerCall?.extractActions(node.fir) }
    }

    override fun visitFunctionEnterNode(node: FunctionEnterNode, data: CoeffectActionsOnNodes) {
        visitFunctionBoundaryNode(node, data) { onOwnerEnter?.extractActions(node.fir) }
    }

    override fun visitFunctionExitNode(node: FunctionExitNode, data: CoeffectActionsOnNodes) {
        visitFunctionBoundaryNode(node, data) { onOwnerExit?.extractActions(node.fir) }
    }

    protected inline fun visitFunctionBoundaryNode(
        node: CFGNode<FirFunction<*>>,
        data: CoeffectActionsOnNodes,
        extractor: CoeffectActionExtractors.() -> CoeffectContextActions?
    ) {
        val function = node.fir
        if (function.isLambda()) {
            val (calledFunction, lambdaSymbol) = lambdaToOwnerFunction[function] ?: return
            collectLambdaCoeffectActions(node, calledFunction, lambdaSymbol, data, extractor)
        } else collectCoeffectActions(node, data, extractor)
    }

    protected inline fun collectCoeffectActions(
        node: CFGNode<*>,
        data: CoeffectActionsOnNodes,
        extractor: CoeffectActionExtractors.() -> CoeffectContextActions?
    ) {
        val effects = node.fir.contractDescription?.effects?.filterIsInstance<ConeCoeffectEffectDeclaration>()
        if (effects.isNullOrEmpty()) return

        for (effect in effects) {
            if (!collectFamilyActions(effect.family)) continue
            val actions = extractor(effect.actionExtractors) ?: continue
            data[node] = actions
        }
    }

    protected inline fun collectLambdaCoeffectActions(
        node: CFGNode<*>,
        function: FirFunction<*>,
        lambdaSymbol: AbstractFirBasedSymbol<*>,
        data: CoeffectActionsOnNodes,
        extractor: CoeffectActionExtractors.() -> CoeffectContextActions?
    ) {
        val effects = function.contractDescription?.effects?.filterIsInstance<ConeLambdaCoeffectEffectDeclaration>()
        if (effects.isNullOrEmpty()) return

        for (effect in effects) {
            if (!collectFamilyActions(effect.family)) continue
            if (function.valueParameters.getOrNull(effect.lambda.parameterIndex)?.symbol != lambdaSymbol) continue
            val actions = extractor(effect.actionExtractors) ?: continue
            data[node] = actions
        }
    }

    protected val FirElement.contractDescription: FirContractDescription?
        get() = when (this) {
            is FirFunction<*> -> (this as? FirContractDescriptionOwner)?.contractDescription
            is FirFunctionCall -> (this.toResolvedCallableSymbol()?.fir as? FirContractDescriptionOwner)?.contractDescription
            else -> null
        }
}

class CoeffectActionsOnNodes(private val data: MutableMap<CFGNode<*>, MutableList<CoeffectContextActions>>) {

    constructor() : this(mutableMapOf())

    operator fun get(node: CFGNode<*>): MutableList<CoeffectContextActions>? = data[node]

    operator fun set(node: CFGNode<*>, actions: CoeffectContextActions) {
        if (!actions.isEmpty) data.getOrPut(node, ::mutableListOf) += actions
    }

    operator fun iterator() = data.iterator()

    fun hasVerifiers() = data.any { entry -> entry.value.any { it.verifiers.isNotEmpty() } }
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.coneEffects
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.BlockExitNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker

object FirReturnsImpliesAnalyzer : FirControlFlowChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        val function = graph.declaration as? FirFunction ?: return
        val graphRef = function.controlFlowGraphReference as FirControlFlowGraphReferenceImpl
        val dataFlowInfo = graphRef.dataFlowInfo
        if (function !is FirContractDescriptionOwner || dataFlowInfo == null) return

        val effects = (function.contractDescription as? FirResolvedContractDescription)?.coneEffects
            ?.filter { it is ConeConditionalEffectDeclaration && it.effect is ConeReturnsEffectDeclaration }

        if (effects.isNullOrEmpty()) return

        val logicSystem = object : PersistentLogicSystem(context.session.typeContext) {
            override val variableStorage: VariableStorageImpl
                get() = throw IllegalStateException("shouldn't be called")

            override fun ConeKotlinType.isAcceptableForSmartcast(): Boolean =
                !isNullableNothing
        }

        effects.forEach { effect ->
            val wrongCondition = graph.exitNode.previousCfgNodes.any {
                isWrongConditionOnNode(it, effect as ConeConditionalEffectDeclaration, function, logicSystem, dataFlowInfo, context)
            }

            if (wrongCondition) {
                reporter.reportOn(function.contractDescription.source, FirErrors.WRONG_IMPLIES_CONDITION, context)
            }
        }
    }

    private fun isWrongConditionOnNode(
        node: CFGNode<*>,
        effectDeclaration: ConeConditionalEffectDeclaration,
        function: FirFunction,
        logicSystem: LogicSystem,
        dataFlowInfo: DataFlowInfo,
        context: CheckerContext
    ): Boolean {
        val effect = effectDeclaration.effect as ConeReturnsEffectDeclaration
        val builtinTypes = context.session.builtinTypes
        val typeContext = context.session.typeContext

        val isReturn = node is JumpNode && node.fir is FirReturnExpression
        val resultExpression = if (isReturn) (node.fir as FirReturnExpression).result else node.fir

        val expressionType = (resultExpression as? FirExpression)?.typeRef?.coneType
        if (expressionType == builtinTypes.nothingType.type) return false

        if (isReturn && resultExpression is FirWhenExpression) {
            return node.collectBranchExits().any {
                isWrongConditionOnNode(it, effectDeclaration, function, logicSystem, dataFlowInfo, context)
            }
        }

        var flow = dataFlowInfo.flowOnNodes.getValue(node)
        val operation = effect.value.toOperation()
        if (operation != null) {
            if (resultExpression is FirConstExpression<*>) {
                if (!operation.isTrueFor(resultExpression.value)) return false
            } else {
                if (expressionType != null && !operation.canBeTrueFor(context.session, expressionType)) return false
                // TODO: avoid modifying the storage
                val variableStorage = dataFlowInfo.variableStorage as VariableStorageImpl
                val resultVar = variableStorage.getOrCreateIfReal(flow, resultExpression)
                if (resultVar != null) {
                    val impliedByReturnValue = logicSystem.approveOperationStatement(flow, OperationStatement(resultVar, operation))
                    if (impliedByReturnValue.isNotEmpty()) {
                        flow = flow.fork().also { logicSystem.addTypeStatements(it, impliedByReturnValue) }.freeze()
                    }
                }
            }
        }

        // TODO: if this is not a top-level function, `FirDataFlowAnalyzer` has erased its value parameters
        //  from `dataFlowInfo.variableStorage` for some reason, so its `getLocalVariable` doesn't work.
        val knownVariables = flow.knownVariables.associateBy { it.identifier }
        val argumentVariables = Array(function.valueParameters.size + 1) { i ->
            val parameterSymbol = if (i > 0) {
                function.valueParameters[i - 1].symbol
            } else {
                if (function.symbol is FirPropertyAccessorSymbol) {
                    context.containingProperty?.symbol
                } else {
                    null
                } ?: function.symbol
            }
            val identifier = Identifier(parameterSymbol, null, null)
            // Might be unknown if there are no statements made about that parameter, but it's still possible that trivial
            // contracts are valid. E.g. `returns() implies (x is String)` when `x`'s *original type* is already `String`.
            knownVariables[identifier] ?: RealVariable(identifier, i == 0, null, i, PropertyStability.STABLE_VALUE)
        }

        val conditionStatements = logicSystem.approveContractStatement(
            effectDeclaration.condition, argumentVariables, substitutor = null
        ) { logicSystem.approveOperationStatement(flow, it) } ?: return true

        return !conditionStatements.values.all { requirement ->
            val originalType = requirement.variable.identifier.symbol.correspondingParameterType ?: return@all true
            val requiredType = requirement.smartCastedType(typeContext, originalType)
            val actualType = flow.getTypeStatement(requirement.variable).smartCastedType(typeContext, originalType)
            actualType.isSubtypeOf(typeContext, requiredType)
        }
    }

    private fun Operation.canBeTrueFor(session: FirSession, type: ConeKotlinType): Boolean = when (this) {
        Operation.EqTrue, Operation.EqFalse ->
            AbstractTypeChecker.isSubtypeOf(session.typeContext, session.builtinTypes.booleanType.type, type)
        Operation.EqNull -> type.canBeNull
        Operation.NotEqNull -> !type.isNullableNothing
    }

    private fun Operation.isTrueFor(value: Any?) = when (this) {
        Operation.EqTrue -> value == true
        Operation.EqFalse -> value == false
        Operation.EqNull -> value == null
        Operation.NotEqNull -> value != null
    }

    private fun CFGNode<*>.collectBranchExits(nodes: MutableList<CFGNode<*>> = mutableListOf()): List<CFGNode<*>> {
        if (this is BlockExitNode) {
            nodes += previousCfgNodes
        } else previousCfgNodes.forEach { it.collectBranchExits(nodes) }
        return nodes
    }

    private val CheckerContext.containingProperty: FirProperty?
        get() = (containingDeclarations.lastOrNull { it is FirProperty } as? FirProperty)

    private val FirBasedSymbol<*>.correspondingParameterType: ConeKotlinType?
        get() = when (this) {
            is FirValueParameterSymbol -> resolvedReturnType
            is FirCallableSymbol<*> -> resolvedReceiverTypeRef?.coneType
            else -> null
        }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.cfa.util.previousCfgNodes
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
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
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

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
                get() = dataFlowInfo.variableStorage as VariableStorageImpl

            override fun ConeKotlinType.isAcceptableForSmartcast(): Boolean {
                return !isNullableNothing
            }
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
        logicSystem: LogicSystem<PersistentFlow>,
        dataFlowInfo: DataFlowInfo,
        context: CheckerContext
    ): Boolean {
        val effect = effectDeclaration.effect as ConeReturnsEffectDeclaration
        val builtinTypes = context.session.builtinTypes
        val typeContext = context.session.typeContext
        val flow = dataFlowInfo.flowOnNodes.getValue(node) as PersistentFlow

        val isReturn = node is JumpNode && node.fir is FirReturnExpression
        val resultExpression = if (isReturn) (node.fir as FirReturnExpression).result else node.fir

        val expressionType = (resultExpression as? FirExpression)?.typeRef?.coneType
        if (expressionType == builtinTypes.nothingType.type) return false

        if (isReturn && resultExpression is FirWhenExpression) {
            return node.collectBranchExits().any {
                isWrongConditionOnNode(it, effectDeclaration, function, logicSystem, dataFlowInfo, context)
            }
        }

        // TODO: create separate variable storage and don't modify existing one
        val variableStorage = dataFlowInfo.variableStorage as VariableStorageImpl

        var typeStatements: TypeStatements = flow.approvedTypeStatements

        if (effect.value != ConeConstantReference.WILDCARD) {
            val operation = effect.value.toOperation()
            if (expressionType != null && expressionType.isInapplicableWith(operation, context.session)) return false

            if (resultExpression is FirConstExpression<*>) {
                if (!resultExpression.isApplicableWith(operation)) return false
            } else {
                val resultVar = variableStorage.getOrCreateVariable(flow, resultExpression)
                typeStatements = logicSystem.approveOperationStatement(flow, OperationStatement(resultVar, operation), builtinTypes)
            }
        }

        val conditionStatements = effectDeclaration.condition.buildTypeStatements(
            function, logicSystem, variableStorage, flow, context
        ) ?: return false

        for ((realVar, requiredTypeStatement) in conditionStatements) {
            val fixedRealVar = typeStatements.keys.find { it.identifier == realVar.identifier } ?: realVar
            val originalType = function.getParameterType(fixedRealVar.identifier.symbol, context) ?: continue
            val resultType = typeStatements[fixedRealVar]?.exactType.intersectWith(typeContext, originalType)
            val requiredType = typeContext.intersectTypesOrNull(requiredTypeStatement.exactType.toList())
            if (requiredType != null && !requiredType.isSupertypeOf(typeContext, resultType)) return true
        }
        return false
    }

    private fun LogicSystem<PersistentFlow>.approveOperationStatement(
        flow: PersistentFlow,
        statement: OperationStatement,
        builtinTypes: BuiltinTypes
    ): TypeStatements {
        val newTypeStatements = andForTypeStatements(flow.approvedTypeStatements, approveOperationStatement(flow, statement))
        val variable = statement.variable
        if (!variable.isReal()) return newTypeStatements
        val extraStatement = when (statement.operation) {
            Operation.NotEqNull -> variable.nullabilityStatement(builtinTypes, isNull = false)
            Operation.EqNull -> variable.nullabilityStatement(builtinTypes, isNull = true)
            else -> return newTypeStatements
        }
        return andForTypeStatements(newTypeStatements, mapOf(variable to extraStatement))
    }

    private fun ConeBooleanExpression.buildTypeStatements(
        function: FirFunction,
        logicSystem: LogicSystem<*>,
        variableStorage: VariableStorageImpl,
        flow: Flow,
        context: CheckerContext
    ): TypeStatements? {
        fun getOrCreateRealVariable(arg: ConeValueParameterReference): RealVariable? {
            val parameterSymbol = function.getParameterSymbol(arg.parameterIndex, context)

            @OptIn(SymbolInternals::class)
            val parameter = parameterSymbol.fir
            return variableStorage.getOrCreateRealVariable(flow, parameterSymbol, parameter)?.takeIf {
                it.stability == PropertyStability.STABLE_VALUE ||
                        // TODO: consider removing the part below
                        it.stability == PropertyStability.LOCAL_VAR
            }
        }

        fun ConeBooleanExpression.toTypeStatements(inverted: Boolean): TypeStatements? = when (this) {
            is ConeBinaryLogicExpression -> {
                val left = left.toTypeStatements(inverted)
                val right = right.toTypeStatements(inverted)
                when {
                    left == null -> right
                    right == null -> left
                    (kind == LogicOperationKind.AND) == !inverted -> logicSystem.andForTypeStatements(left, right)
                    else -> logicSystem.orForTypeStatements(left, right)
                }
            }
            is ConeIsInstancePredicate ->
                if (isNegated == inverted) getOrCreateRealVariable(arg)?.let { it typeEq type }?.singleton() else mapOf()
            is ConeIsNullPredicate ->
                getOrCreateRealVariable(arg)?.nullabilityStatement(context.session.builtinTypes, isNull = isNegated == inverted)
                    ?.singleton()
            is ConeLogicalNot -> arg.toTypeStatements(!inverted)
            else -> null
        }

        return toTypeStatements(inverted = false)
    }

    private fun RealVariable.nullabilityStatement(builtinTypes: BuiltinTypes, isNull: Boolean) =
        this typeEq (if (isNull) builtinTypes.nullableNothingType.type else builtinTypes.anyType.type)

    private fun TypeStatement.singleton(): TypeStatements =
        mapOf(variable to this)

    private fun ConeKotlinType.isInapplicableWith(operation: Operation, session: FirSession): Boolean {
        return (operation == Operation.EqFalse || operation == Operation.EqTrue)
                && !AbstractTypeChecker.isSubtypeOf(session.typeContext, session.builtinTypes.booleanType.type, this)
                || operation == Operation.EqNull && !isNullable
    }

    private fun FirConstExpression<*>.isApplicableWith(operation: Operation): Boolean = when {
        kind == ConstantValueKind.Null -> operation == Operation.EqNull
        kind == ConstantValueKind.Boolean && operation == Operation.EqTrue -> (value as Boolean)
        kind == ConstantValueKind.Boolean && operation == Operation.EqFalse -> !(value as Boolean)
        else -> true
    }

    private fun CFGNode<*>.collectBranchExits(nodes: MutableList<CFGNode<*>> = mutableListOf()): List<CFGNode<*>> {
        if (this is BlockExitNode) {
            nodes += previousCfgNodes
        } else previousCfgNodes.forEach { it.collectBranchExits(nodes) }
        return nodes
    }

    private val CheckerContext.containingProperty: FirProperty?
        get() = (containingDeclarations.lastOrNull { it is FirProperty } as? FirProperty)

    private fun FirFunction.getParameterType(symbol: FirBasedSymbol<*>, context: CheckerContext): ConeKotlinType? {
        val typeRef = if (this.symbol == symbol) {
            if (symbol is FirPropertyAccessorSymbol) {
                context.containingProperty?.receiverParameter?.typeRef
            } else {
                receiverParameter?.typeRef
            }
        } else {
            valueParameters.find { it.symbol == symbol }?.returnTypeRef
        }
        return typeRef?.coneType
    }

    private fun FirFunction.getParameterSymbol(index: Int, context: CheckerContext): FirBasedSymbol<*> {
        return if (index == -1) {
            if (symbol !is FirPropertyAccessorSymbol) {
                symbol
            } else {
                context.containingProperty?.symbol ?: symbol
            }
        } else {
            this.valueParameters[index].symbol
        }
    }
}

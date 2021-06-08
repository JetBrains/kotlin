/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.BuiltinTypes
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.cfa.FirControlFlowChecker
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isSupertypeOf
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
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
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertyAccessorSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addIfNotNull

object FirReturnsImpliesAnalyzer : FirControlFlowChecker() {

    override fun analyze(graph: ControlFlowGraph, reporter: DiagnosticReporter, context: CheckerContext) {
        val function = graph.declaration as? FirFunction<*> ?: return
        val graphRef = function.controlFlowGraphReference as FirControlFlowGraphReferenceImpl
        val dataFlowInfo = graphRef.dataFlowInfo
        if (function !is FirContractDescriptionOwner || dataFlowInfo == null) return

        val effects = (function.contractDescription as? FirResolvedContractDescription)?.coneEffects
            ?.filter { it is ConeConditionalEffectDeclaration && it.effect is ConeReturnsEffectDeclaration }

        if (effects.isNullOrEmpty()) return

        val logicSystem = object : PersistentLogicSystem(context.session.typeContext) {
            override fun processUpdatedReceiverVariable(flow: PersistentFlow, variable: RealVariable) =
                throw IllegalStateException("Receiver variable update is not possible for this logic system")

            override fun updateAllReceivers(flow: PersistentFlow) =
                throw IllegalStateException("Update of all receivers is not possible for this logic system")

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
        function: FirFunction<*>,
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

        var typeStatements: TypeStatements = flow.approvedTypeStatements

        if (effect.value != ConeConstantReference.WILDCARD) {
            val operation = effect.value.toOperation()
            if (expressionType != null && expressionType.isInapplicableWith(operation, context.session)) return false

            if (resultExpression is FirConstExpression<*>) {
                if (!resultExpression.isApplicableWith(operation)) return false
            } else {
                val resultVar = dataFlowInfo.variableStorage.getOrCreateVariable(flow, resultExpression)
                typeStatements = logicSystem.approveOperationStatement(flow, OperationStatement(resultVar, operation), builtinTypes)
            }
        }

        val conditionStatements = effectDeclaration.condition.buildTypeStatements(
            function, logicSystem, dataFlowInfo.variableStorage, flow, context
        ) ?: return false

        for ((realVar, requiredTypeStatement) in conditionStatements) {
            val fixedRealVar = typeStatements.keys.find { it.identifier == realVar.identifier } ?: realVar
            val resultTypeStatement = typeStatements[fixedRealVar]

            val resultType = mutableListOf<ConeKotlinType>().apply {
                addIfNotNull(function.getParameterType(fixedRealVar.identifier.symbol, context))
                if (resultTypeStatement != null) addAll(resultTypeStatement.exactType)
            }.let { typeContext.intersectTypesOrNull(it) }

            val requiredType = typeContext.intersectTypesOrNull(requiredTypeStatement.exactType.toList())
            if (requiredType != null && !requiredType.isSupertypeOf(typeContext, resultType)) return true
        }
        return false
    }

    private fun LogicSystem<PersistentFlow>.approveOperationStatement(
        flow: PersistentFlow,
        statement: OperationStatement,
        builtinTypes: BuiltinTypes
    ): MutableTypeStatements {
        val newTypeStatements: MutableTypeStatements = mutableMapOf()

        approveStatementsTo(newTypeStatements, flow, statement, flow.logicStatements.flatMap { it.value })
        newTypeStatements.mergeTypeStatements(flow.approvedTypeStatements)

        val variable = statement.variable
        if (variable.isReal()) {
            if (statement.operation == Operation.NotEqNull) {
                newTypeStatements.addStatement(variable, simpleTypeStatement(variable, true, builtinTypes.anyType.type))
            } else if (statement.operation == Operation.EqNull) {
                newTypeStatements.addStatement(variable, simpleTypeStatement(variable, false, builtinTypes.anyType.type))
            }
        }

        return newTypeStatements
    }

    private fun ConeBooleanExpression.buildTypeStatements(
        function: FirFunction<*>,
        logicSystem: LogicSystem<*>,
        variableStorage: VariableStorage,
        flow: Flow,
        context: CheckerContext
    ): MutableTypeStatements? {
        fun buildTypeStatements(arg: ConeValueParameterReference, exactType: Boolean, type: ConeKotlinType): MutableTypeStatements? {
            val fir = function.getParameterSymbol(arg.parameterIndex, context).fir
            val realVar = variableStorage.getOrCreateRealVariable(flow, fir.symbol, fir)
                ?.takeIf {
                    it.stability == PropertyStability.STABLE_VALUE ||
                            // TODO: consider removing the part below
                            it.stability == PropertyStability.LOCAL_VAR
                }
            return realVar?.to(simpleTypeStatement(realVar, exactType, type))?.let { mutableMapOf(it) }
        }
        return when (this) {
            is ConeBinaryLogicExpression -> {
                val left = left.buildTypeStatements(function, logicSystem, variableStorage, flow, context)
                val right = right.buildTypeStatements(function, logicSystem, variableStorage, flow, context)
                if (left != null && right != null) {
                    if (kind == LogicOperationKind.AND) {
                        left.apply { mergeTypeStatements(right) }
                    } else logicSystem.orForTypeStatements(left, right)
                } else (left ?: right)
            }
            is ConeIsInstancePredicate -> buildTypeStatements(arg, !isNegated, type)
            is ConeIsNullPredicate -> buildTypeStatements(arg, isNegated, context.session.builtinTypes.anyType.type)
            is ConeLogicalNot -> arg.buildTypeStatements(function, logicSystem, variableStorage, flow, context)
                ?.mapValuesTo(mutableMapOf()) { (_, value) -> value.invert() }

            else -> null
        }
    }

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

    private fun simpleTypeStatement(realVar: RealVariable, exactType: Boolean, type: ConeKotlinType): MutableTypeStatement {
        return MutableTypeStatement(
            realVar,
            if (exactType) linkedSetOf(type) else linkedSetOf(),
            if (!exactType) linkedSetOf(type) else linkedSetOf()
        )
    }

    private fun CFGNode<*>.collectBranchExits(nodes: MutableList<CFGNode<*>> = mutableListOf()): List<CFGNode<*>> {
        if (this is BlockExitNode) {
            nodes += previousCfgNodes
        } else previousCfgNodes.forEach { it.collectBranchExits(nodes) }
        return nodes
    }

    private val CheckerContext.containingProperty: FirProperty?
        get() = (containingDeclarations.asReversed().firstOrNull { it is FirProperty } as? FirProperty)

    private fun FirFunction<*>.getParameterType(symbol: AbstractFirBasedSymbol<*>, context: CheckerContext): ConeKotlinType? {
        val typeRef = if (this.symbol == symbol) {
            if (symbol is FirPropertyAccessorSymbol) {
                context.containingProperty?.receiverTypeRef
            } else {
                receiverTypeRef
            }
        } else {
            valueParameters.find { it.symbol == symbol }?.returnTypeRef
        }
        return typeRef?.coneType
    }

    private fun FirFunction<*>.getParameterSymbol(index: Int, context: CheckerContext): AbstractFirBasedSymbol<*> {
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

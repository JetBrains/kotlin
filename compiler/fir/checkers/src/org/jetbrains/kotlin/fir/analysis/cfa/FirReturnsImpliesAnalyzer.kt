/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.CFGNode
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullable
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.utils.addIfNotNull

class FirReturnsImpliesAnalyzer {

    fun analyze(function: FirFunction<*>, graph: ControlFlowGraph, reporter: DiagnosticReporter) {
        val cfgRef = function.controlFlowGraphReference as FirControlFlowGraphReferenceImpl
        val variableStorage = cfgRef.variableStorage
        if (function !is FirContractDescriptionOwner || variableStorage == null) return

        val effects = (function.contractDescription as? FirResolvedContractDescription)?.effects
            ?.filterIsInstance<ConeConditionalEffectDeclaration>()
            ?.filter { it.effect is ConeReturnsEffectDeclaration }

        if (effects.isNullOrEmpty()) return

        val logicSystem = object : PersistentLogicSystem(function.session.typeContext) {
            override fun processUpdatedReceiverVariable(flow: PersistentFlow, variable: RealVariable) =
                throw IllegalStateException("Receiver variable update is not possible for this logic system")

            override fun updateAllReceivers(flow: PersistentFlow) =
                throw IllegalStateException("Update of all receivers is not possible for this logic system")
        }

        fun ConeBooleanExpression.show(): String = when (this) {
            is ConeBinaryLogicExpression -> "(${left.show()}) ${kind.token} (${right.show()})"
            is ConeBooleanConstantReference -> name
            is ConeBooleanValueParameterReference -> "$name($parameterIndex)"
            is ConeIsInstancePredicate -> "${arg.name}(${arg.parameterIndex}) ${if (isNegated) "!" else ""}is $type"
            is ConeIsNullPredicate -> "${arg.name}(${arg.parameterIndex}) ${if (isNegated) "!=" else "=="} null"
            is ConeLogicalNot -> "!${arg.show()}"
            else -> "unknown"
        }

        fun AbstractFirBasedSymbol<*>.name(): String {
            return when (this) {
                is FirCallableSymbol<*> -> callableId.callableName.toString()
                else -> "unknown"
            }
        }

        fun DataFlowVariable.name(): String {
            return when (this) {
                is RealVariable -> identifier.symbol.name()
                is SyntheticVariable -> "syn${variableStorage.syntheticVariables.values.indexOf(this)}"
            }
        }

        fun debug(message: Any?) {
            println(message)
        }

        val builtinTypes = function.session.builtinTypes
        val typeContext = function.session.typeContext
        fun KotlinTypeMarker.isSupertypeOf(type: KotlinTypeMarker?) =
            type != null && AbstractTypeChecker.isSubtypeOf(typeContext, type, this)

        effects.forEach { effectDeclaration ->
            val returnsEffect = effectDeclaration.effect as ConeReturnsEffectDeclaration
            val condition = effectDeclaration.condition

            debug("returns ${returnsEffect.value.name} -> ${condition.show()}")

            fun checkNode(node: CFGNode<*>): Boolean {
                val flow = cfgRef.flowOnNodes.getValue(node) as PersistentFlow

                val resultExpr = if (node is JumpNode && node.fir is FirReturnExpression) {
                    (node.fir as FirReturnExpression).result
                } else node.fir

                val exprType = (resultExpr as? FirExpression)?.typeRef?.coneType
                if (exprType == builtinTypes.nothingType.type) return false

                if (resultExpr is FirWhenExpression) {
                    debug("    # when branch") // When exit -> When branch exit -> Block exit -> Last expression
                    return node.previousNodes(4).any { checkNode(it) }
                }

                var typeStatements: TypeStatements = flow.approvedTypeStatements
                var operationStatements: ApprovedOperations = flow.approvedOperations

                val implications = flow.logicStatements.flatMap { it.value }

                debug("  on exit: $node")
                debug("    expr: $resultExpr ($exprType)")
                debug("    vars:")
                variableStorage.realVariables.forEach {
                    debug("      val ${it.key.symbol.name()} = ${it.value}")
                }
                variableStorage.syntheticVariables.forEach { _, syntVar ->
                    debug("      synt $syntVar")
                }
                debug("    implications:")
                implications.forEach {
                    debug("      $it")
                }
                debug("    type statements:")
                typeStatements.forEach { (it, types) ->
                    debug("      <- $it(${it.name()}) : [${types.exactType.joinToString()}] ![${types.exactNotType.joinToString()}]")
                }

                if (returnsEffect.value != ConeConstantReference.WILDCARD) {
                    val operation = returnsEffect.value.toOperation()

                    if (exprType != null && exprType.isInapplicableWith(operation, function.session)) return false

                    if (resultExpr is FirConstExpression<*>) {
                        val applicableResult = when {
                            resultExpr.kind == FirConstKind.Null -> operation == Operation.EqNull
                            resultExpr.kind == FirConstKind.Boolean && operation == Operation.EqTrue -> (resultExpr.value as Boolean)
                            resultExpr.kind == FirConstKind.Boolean && operation == Operation.EqFalse -> !(resultExpr.value as Boolean)
                            else -> true
                        }

                        debug("    return const '${resultExpr.value}'")
                        if (!applicableResult) {
                            debug("    (inapplicable case)")
                            return false
                        }
                    } else {
                        val resultVar = variableStorage.getOrCreateVariable(flow, resultExpr)
                        val newTypeStatements: MutableTypeStatements = mutableMapOf()
                        val newApprovedOperations: MutableApprovedOperations = mutableMapOf()

                        logicSystem.approveStatementsTo(
                            newTypeStatements,
                            newApprovedOperations,
                            flow,
                            OperationStatement(resultVar, operation),
                            implications
                        )
                        newTypeStatements.mergeTypeStatements(flow.approvedTypeStatements)
                        newApprovedOperations.mergeApprovedOperations(flow.approvedOperations)

                        if (resultVar.isReal()) {
                            if (operation == Operation.NotEqNull) {
                                newTypeStatements.addStatement(resultVar, simpleTypeStatement(resultVar, true, builtinTypes.anyType.type))
                            } else if (operation == Operation.EqNull) {
                                newTypeStatements.addStatement(resultVar, simpleTypeStatement(resultVar, false, builtinTypes.anyType.type))
                            }
                        }

                        debug("    return $resultVar $operation")
                        typeStatements = newTypeStatements
                        operationStatements = newApprovedOperations
                    }
                }

                val conditionStatements = condition.buildTypeStatements(function, logicSystem, variableStorage, flow) ?: return false

                debug("    conditions:")
                conditionStatements.forEach { (it, types) ->
                    debug("      -> $it(${it.name()}) : [${types.exactType.joinToString()}] ![${types.exactNotType.joinToString()}]")
                }
                debug("    result statements:")
                typeStatements.forEach { (it, types) ->
                    debug("      <- $it(${it.name()}) : [${types.exactType.joinToString()}] ![${types.exactNotType.joinToString()}]")
                }
                debug("    operation statements:")
                operationStatements.forEach { v, info ->
                    debug("    # $v(${v.name()}) : ${info.operations.joinToString(", ", "[", "]")}")
                }

                for ((realVar, requiredTypeStatement) in conditionStatements) {
                    val fixedRealVar = typeStatements.keys.find { it.identifier == realVar.identifier } ?: realVar

                    println("    $realVar : ${flow.getTypeStatement(realVar)}")

                    val resultTypeStatement = typeStatements[fixedRealVar]
                    val resultType = if (resultTypeStatement != null) {
                        val resultTypes = mutableListOf<ConeKotlinType>()
                        resultTypes += resultTypeStatement.exactType
                        resultTypes.addIfNotNull(function.getParameterType(fixedRealVar.identifier.symbol))
                        if (resultTypes.isNotEmpty()) ConeTypeIntersector.intersectTypes(typeContext, resultTypes) else null
                    } else function.getParameterType(fixedRealVar.identifier.symbol)

                    if (requiredTypeStatement.exactType.isNotEmpty()) {
                        val requiredType = ConeTypeIntersector.intersectTypes(typeContext, requiredTypeStatement.exactType.toList())
                        if (!requiredType.isSupertypeOf(resultType)) {
                            debug("    result FALSE (required by '${fixedRealVar.identifier.symbol.name()}')")
                            return true
                        }
                    }

                    if (requiredTypeStatement.exactNotType.isNotEmpty()) {
                        val forbiddenType = ConeTypeIntersector.intersectTypes(typeContext, requiredTypeStatement.exactNotType.toList())
                        if (forbiddenType.isSupertypeOf(resultType)) {
                            debug("    result FALSE (forbidden by '${fixedRealVar.identifier.symbol.name()}')")
                            return true
                        }
                    }
                }
                return false
            }

            val wrongCondition = graph.exitNode.previousCfgNodes.any { checkNode(it) }
            if (wrongCondition) {
                function.contractDescription.source?.let {
                    reporter.report(FirErrors.WRONG_IMPLIES_CONDITION.on(it))
                }
            }
        }
    }

    private fun CFGNode<*>.previousNodes(depth: Int, nodes: MutableList<CFGNode<*>> = mutableListOf()): List<CFGNode<*>> {
        if (depth == 0) nodes.add(this) else previousCfgNodes.forEach { it.previousNodes(depth - 1, nodes) }
        return nodes
    }

    private fun FirFunction<*>.getParameterType(symbol: AbstractFirBasedSymbol<*>): ConeKotlinType? {
        return (if (this.symbol == symbol) receiverTypeRef else valueParameters.find { it.symbol == symbol }?.returnTypeRef)?.coneType
    }

    private fun FirFunction<*>.getParameterSymbol(index: Int): AbstractFirBasedSymbol<*> {
        return if (index == -1) this.symbol else this.valueParameters[index].symbol
    }

    private fun ConeKotlinType.isInapplicableWith(operation: Operation, session: FirSession): Boolean {
        return (operation == Operation.EqFalse || operation == Operation.EqTrue)
                && !AbstractTypeChecker.isSubtypeOf(session.typeContext, session.builtinTypes.booleanType.type, this)
                || operation == Operation.EqNull && !isNullable
    }

    private fun ConeBooleanExpression.buildTypeStatements(
        function: FirFunction<*>,
        logicSystem: LogicSystem<*>,
        variableStorage: VariableStorage,
        flow: Flow
    ): MutableTypeStatements? = when (this) {
        is ConeBinaryLogicExpression -> {
            val left = left.buildTypeStatements(function, logicSystem, variableStorage, flow)
            val right = right.buildTypeStatements(function, logicSystem, variableStorage, flow)
            if (left != null && right != null) {
                if (kind == LogicOperationKind.AND) {
                    left.apply { mergeTypeStatements(right) }
                } else logicSystem.orForTypeStatements(left, right)
            } else (left ?: right)
        }
        is ConeIsInstancePredicate -> {
            val fir = function.getParameterSymbol(arg.parameterIndex).fir
            mutableMapOf<RealVariable, MutableTypeStatement>().also {
                val realVar = variableStorage.getOrCreateRealVariable(flow, fir.symbol, fir)
                if (realVar != null) it[realVar] = simpleTypeStatement(realVar, !isNegated, type)
            }
        }
        is ConeIsNullPredicate -> {
            val fir = function.getParameterSymbol(arg.parameterIndex).fir
            val session = function.session
            mutableMapOf<RealVariable, MutableTypeStatement>().also {
                val realVar = variableStorage.getOrCreateRealVariable(flow, fir.symbol, fir)
                if (realVar != null) it[realVar] = simpleTypeStatement(realVar, isNegated, session.builtinTypes.anyType.type)
            }
        }
        is ConeLogicalNot -> arg.buildTypeStatements(function, logicSystem, variableStorage, flow)
            ?.mapValuesTo(mutableMapOf()) { (_, value) -> value.invert() }

        else -> null
    }

    private fun simpleTypeStatement(realVar: RealVariable, condition: Boolean, type: ConeKotlinType): MutableTypeStatement {
        return MutableTypeStatement(
            realVar,
            if (condition) linkedSetOf(type) else linkedSetOf(),
            if (!condition) linkedSetOf(type) else linkedSetOf()
        )
    }
}
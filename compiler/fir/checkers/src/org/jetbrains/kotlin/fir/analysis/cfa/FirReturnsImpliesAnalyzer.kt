/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.cfa

import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.LogicOperationKind
import org.jetbrains.kotlin.fir.resolve.dfa.*
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.ControlFlowGraph
import org.jetbrains.kotlin.fir.resolve.dfa.cfg.JumpNode
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.typeContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.types.AbstractTypeChecker
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
                is SyntheticVariable -> "synthetic"
            }
        }

        effects.forEach { effectDeclaration ->
            val returnsEffect = effectDeclaration.effect as ConeReturnsEffectDeclaration
            val returnsOperation = returnsEffect.getOperation()
            val condition = effectDeclaration.condition

//            println("returns ${returnsEffect.value.name} -> ${condition.show()}")

            var wrongCondition = false

            exitNodes@ for (node in graph.exitNode.previousCfgNodes) {
                val flow = cfgRef.flowOnNodes.getValue(node) as PersistentFlow
                val implications = flow.logicStatements.flatMap { it.value }
                val conditionTypeStatements = condition.buildTypeStatements(function, logicSystem, variableStorage, flow)

//                println("  $node")
//
//                variableStorage.realVariables.forEach {
//                    println("    val ${it.key.symbol.name()} = ${it.value}")
//                }
//
//                conditionTypeStatements.forEach { (it, types) ->
//                    println("    -> $it(${it.name()}) : [${types.exactType.joinToString()}] ![${types.exactNotType.joinToString()}]")
//                }
//
//                flow.logicStatements.forEach { (_, list) ->
//                    list.forEach {
//                        println("      $it")
//                    }
//                }

                var typeStatements: TypeStatements = flow.approvedTypeStatements

                if (returnsOperation != null && node is JumpNode && node.fir is FirReturnExpression) {
                    val result = (node.fir as FirReturnExpression).result

                    if (result is FirConstExpression<*>) {
                        val value = result.value
                        val possibleResult = when {
                            value == null -> returnsOperation == Operation.EqNull
                            value is Boolean && returnsOperation == Operation.EqTrue -> value
                            value is Boolean && returnsOperation == Operation.EqFalse -> !value
                            else -> returnsOperation == Operation.NotEqNull
                        }

//                        println("    return const '${result.value}'")

                        if (!possibleResult) {
//                            println("    (impossible case)")
                            continue
                        }
                    } else {
                        val resultVar = variableStorage.getVariable(result, flow)

                        if (resultVar != null) {
                            val newTypeStatements: MutableTypeStatements = mutableMapOf()

                            logicSystem.approveStatementsTo(
                                newTypeStatements,
                                flow,
                                OperationStatement(resultVar, returnsOperation),
                                implications
                            )
                            newTypeStatements.mergeTypeStatements(flow.approvedTypeStatements)

//                            println("    return $resultVar")
                            typeStatements = newTypeStatements
                        } else {
//                            println("    return unknown")
                        }
                    }
                }

                val typeContext = function.session.typeContext

                for ((realVar, requiredTypeStatement) in conditionTypeStatements) {

                    val resultTypeStatement = typeStatements[realVar]
                    val resultType = if (resultTypeStatement != null) {
                        val resultTypes = mutableListOf<ConeKotlinType>()
                        resultTypes += resultTypeStatement.exactType
                        resultTypes.addIfNotNull(function.getParameterType(realVar.identifier.symbol))
                        if (resultTypes.isNotEmpty()) ConeTypeIntersector.intersectTypes(typeContext, resultTypes) else null
                    } else null

                    if (requiredTypeStatement.exactType.isNotEmpty()) {
                        val requiredType = ConeTypeIntersector.intersectTypes(typeContext, requiredTypeStatement.exactType.toList())
                        if (resultType == null || !AbstractTypeChecker.isSubtypeOf(typeContext, resultType, requiredType)) {
                            wrongCondition = true
//                            println("    result FALSE (required by '${(realVar.identifier.symbol as? FirCallableSymbol<*>)?.callableId?.callableName ?: "this"}')")
                            break@exitNodes
                        }
                    }

                    if (requiredTypeStatement.exactNotType.isNotEmpty()) {
                        val forbiddenType = ConeTypeIntersector.intersectTypes(typeContext, requiredTypeStatement.exactNotType.toList())
                        if (resultType != null && AbstractTypeChecker.isSubtypeOf(typeContext, resultType, forbiddenType)) {
                            wrongCondition = true
//                            println("    result FALSE (forbidden by '${(realVar.identifier.symbol as? FirCallableSymbol<*>)?.callableId?.callableName ?: "this"}')")
                            break@exitNodes
                        }
                    }
                }
//                typeStatements.forEach { (it, types) ->
//                    println("    <- $it(${it.name()}) : [${types.exactType.joinToString()}] ![${types.exactNotType.joinToString()}]")
//                }
            }

            if (wrongCondition) {
                println("WRONG")
                function.contractDescription.source?.let {
                    reporter.report(FirErrors.WRING_IMPLIES_CONDITION.on(it))
                }
            }
        }
    }

    private fun FirFunction<*>.getParameterType(symbol: AbstractFirBasedSymbol<*>): ConeKotlinType? {
        return (if (this.symbol == symbol) receiverTypeRef else valueParameters.find { it.symbol == symbol }?.returnTypeRef)?.coneType
    }

    private fun FirFunction<*>.getParameterSymbol(index: Int): AbstractFirBasedSymbol<*> {
        return if (index == -1) this.symbol else this.valueParameters[index].symbol
    }

    private fun ConeReturnsEffectDeclaration.getOperation(): Operation? {
        return when {
            value === ConeConstantReference.NULL -> Operation.EqNull
            value === ConeConstantReference.NOT_NULL -> Operation.NotEqNull
            value === ConeBooleanConstantReference.TRUE -> Operation.EqTrue
            value === ConeBooleanConstantReference.FALSE -> Operation.EqFalse
            else -> null
        }
    }

    fun ConeBooleanExpression.buildTypeStatements(
        function: FirFunction<*>,
        logicSystem: LogicSystem<*>,
        variableStorage: VariableStorage,
        flow: Flow
    ): MutableTypeStatements = when (this) {
        is ConeBinaryLogicExpression -> {
            val left = left.buildTypeStatements(function, logicSystem, variableStorage, flow)
            val right = right.buildTypeStatements(function, logicSystem, variableStorage, flow)
            if (kind == LogicOperationKind.AND) {
                left.apply { mergeTypeStatements(right) }
            } else logicSystem.orForTypeStatements(left, right)
        }

        is ConeIsInstancePredicate -> {
            val fir = function.getParameterSymbol(arg.parameterIndex).fir
            mutableMapOf<RealVariable, MutableTypeStatement>().also {
                variableStorage.getRealVariable(fir.symbol, fir, flow)?.let { realVar ->
                    it[realVar] = MutableTypeStatement(
                        realVar,
                        if (isNegated) linkedSetOf() else linkedSetOf(type),
                        if (isNegated) linkedSetOf(type) else linkedSetOf()
                    )
                }
            }
        }

        is ConeIsNullPredicate -> {
            val fir = function.getParameterSymbol(arg.parameterIndex).fir
            val session = function.session
            mutableMapOf<RealVariable, MutableTypeStatement>().also {
                variableStorage.getRealVariable(fir.symbol, fir, flow)?.let { realVar ->
                    it[realVar] = MutableTypeStatement(
                        realVar,
                        linkedSetOf((if (isNegated) session.builtinTypes.anyType else session.builtinTypes.nullableAnyType).coneType),
                    )
                }
            }
        }

        is ConeLogicalNot -> arg.buildTypeStatements(function, logicSystem, variableStorage, flow)
            .mapValuesTo(mutableMapOf()) { (_, value) -> value.invert() }

        //is ConeBooleanConstantReference -> name
        // is ConeBooleanValueParameterReference -> "$name($parameterIndex)"
        else -> throw IllegalArgumentException("")
    }

}

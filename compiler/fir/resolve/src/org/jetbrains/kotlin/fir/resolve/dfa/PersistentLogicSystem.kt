/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.utils.addIfNotNull
import java.util.*

data class PersistentTypeStatement(
    override val variable: RealVariable,
    override val exactType: PersistentSet<ConeKotlinType>,
    override val exactNotType: PersistentSet<ConeKotlinType>
) : TypeStatement() {
    override operator fun plus(other: TypeStatement): PersistentTypeStatement {
        return PersistentTypeStatement(
            variable,
            exactType + other.exactType,
            exactNotType + other.exactNotType
        )
    }

    override val isEmpty: Boolean
        get() = exactType.isEmpty() && exactNotType.isEmpty()

    override fun invert(): PersistentTypeStatement {
        return PersistentTypeStatement(variable, exactNotType, exactType)
    }
}

typealias PersistentApprovedTypeStatements = PersistentMap<RealVariable, PersistentTypeStatement>
typealias PersistentImplications = PersistentMap<DataFlowVariable, PersistentList<Implication>>

class PersistentFlow : AbstractPersistentFlow<PersistentFlow> {
    var approvedTypeStatements: PersistentApprovedTypeStatements
    var approvedTypeStatementsDiff: PersistentApprovedTypeStatements = persistentHashMapOf()

    constructor(previousFlow: PersistentFlow) : super(previousFlow) {
        approvedTypeStatements = previousFlow.approvedTypeStatements
    }

    constructor() : super(null) {
        approvedTypeStatements = persistentHashMapOf()
    }

    override fun getTypeStatement(variable: RealVariable): TypeStatement? {
        return approvedTypeStatements[variable]
    }

    override fun getImplications(variable: DataFlowVariable): Collection<Implication> {
        return logicStatements[variable] ?: emptyList()
    }

    override fun getVariablesInTypeStatements(): Collection<RealVariable> {
        return approvedTypeStatements.keys
    }

    override fun removeOperations(variable: DataFlowVariable): Collection<Implication> {
        return getImplications(variable).also {
            if (it.isNotEmpty()) {
                logicStatements -= variable
            }
        }
    }

    override fun getVariablesWithNewInfo(): Collection<RealVariable> = approvedTypeStatementsDiff.keys
}

abstract class PersistentLogicSystem(context: ConeInferenceContext) : LogicSystem<PersistentFlow>(context) {
    override fun createEmptyFlow(): PersistentFlow {
        return PersistentFlow()
    }

    override fun forkFlow(flow: PersistentFlow): PersistentFlow {
        return PersistentFlow(flow)
    }

    override fun joinFlow(flows: Collection<PersistentFlow>): PersistentFlow {
        return foldFlow(
            flows,
            { variable, commonFlow ->
                getApprovedTypeStatementsDiff(variable, commonFlow)
            },
            { statements ->
                addApprovedStatements(or(statements).takeIf { it.isNotEmpty })
            },
            computeVariables = { computeVariablesDiffWithCommonFlow ->
                flows.map {
                    computeVariablesDiffWithCommonFlow(it).toList()
                }.intersectSets().takeIf { it.isNotEmpty() }
            }
        )
    }

    override fun unionFlow(flows: Collection<PersistentFlow>): PersistentFlow {
        return foldFlow(
            flows,
            { variable, commonFlow -> getApprovedTypeStatementsDiff(variable, commonFlow) },
            { addApprovedStatements(and(it)) },
            computeVariables = { computeVariablesDiffWithCommonFlow ->
                flows.flatMapTo(mutableSetOf()) {
                    computeVariablesDiffWithCommonFlow(it)
                }
            }
        )
    }

    private fun PersistentFlow.addApprovedStatements(
        info: MutableTypeStatement?
    ) {
        if (info == null) return
        approvedTypeStatements = approvedTypeStatements.addTypeStatement(info)
        if (previousFlow != null) {
            approvedTypeStatementsDiff = approvedTypeStatementsDiff.addTypeStatement(info)
        }
    }

    override fun addLocalVariableAlias(flow: PersistentFlow, alias: RealVariable, underlyingVariable: RealVariableAndType) {
        flow.addLocalVariableAlias(alias, underlyingVariable)
    }

    override fun removeLocalVariableAlias(flow: PersistentFlow, alias: RealVariable) {
        flow.removeLocalVariableAlias(alias)
    }

    @OptIn(DfaInternals::class)
    private fun PersistentFlow.getApprovedTypeStatementsDiff(variable: RealVariable, parentFlow: PersistentFlow): MutableTypeStatement {
        var flow = this
        val result = MutableTypeStatement(variable)
        val variableUnderAlias = directAliasMap[variable]
        if (variableUnderAlias == null) {
            while (flow != parentFlow) {
                flow.approvedTypeStatementsDiff[variable]?.let { result += it }
                flow = flow.previousFlow!!
            }
        } else {
            result.exactType.addIfNotNull(variableUnderAlias.originalType)
            flow.approvedTypeStatements[variableUnderAlias.variable]?.let { result += it }
        }
        return result
    }

    override fun addTypeStatement(flow: PersistentFlow, statement: TypeStatement) {
        if (statement.isEmpty) return
        with(flow) {
            approvedTypeStatements = approvedTypeStatements.addTypeStatement(statement)
            if (previousFlow != null) {
                approvedTypeStatementsDiff = approvedTypeStatementsDiff.addTypeStatement(statement)
            }
            if (statement.variable.isThisReference) {
                processUpdatedReceiverVariable(flow, statement.variable)
            }
        }
    }

    override fun addImplication(flow: PersistentFlow, implication: Implication) {
        flow.addImplication(implication)
    }

    override fun removeAllAboutVariable(flow: PersistentFlow, variable: RealVariable) {
        flow.logicStatements -= variable
        flow.approvedTypeStatements -= variable
        flow.approvedTypeStatementsDiff -= variable
        // TODO: should we search variable in all logic statements?
    }

    override fun translateVariableFromConditionInStatements(
        flow: PersistentFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        shouldRemoveOriginalStatements: Boolean,
        filter: (Implication) -> Boolean,
        transform: (Implication) -> Implication
    ) {
        flow.translateVariableFromConditionInStatements(originalVariable, newVariable, shouldRemoveOriginalStatements, filter, transform)
    }

    override fun approveStatementsInsideFlow(
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        shouldForkFlow: Boolean,
        shouldRemoveSynthetics: Boolean
    ): PersistentFlow {
        val approvedFacts = approveOperationStatementsInternal(
            flow,
            approvedStatement,
            initialStatements = null,
            shouldRemoveSynthetics
        )

        val resultFlow = if (shouldForkFlow) forkFlow(flow) else flow
        if (!approvedFacts.isEmpty) addStatementsToFlow(resultFlow, approvedFacts)
        return resultFlow
    }

    private fun addStatementsToFlow(flow: PersistentFlow, statements: ArrayListMultimap<RealVariable, TypeStatement>) {
        val updatedReceivers = mutableSetOf<RealVariable>()

        statements.asMap().forEach { (variable, infos) ->
            var resultInfo = PersistentTypeStatement(variable, persistentSetOf(), persistentSetOf())
            for (info in infos) {
                resultInfo += info
            }
            if (variable.isThisReference) updatedReceivers += variable
            addTypeStatement(flow, resultInfo)
        }

        updatedReceivers.forEach {
            processUpdatedReceiverVariable(flow, it)
        }
    }

    private fun approveOperationStatementsInternal(
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        initialStatements: Collection<Implication>?,
        shouldRemoveSynthetics: Boolean
    ): ArrayListMultimap<RealVariable, TypeStatement> {
        val approvedFacts: ArrayListMultimap<RealVariable, TypeStatement> = ArrayListMultimap.create()
        val approvedStatements = LinkedList<OperationStatement>().apply { this += approvedStatement }
        flow.approveOperationStatementsInternal(approvedStatements, initialStatements, shouldRemoveSynthetics, approvedFacts)
        return approvedFacts
    }

    override fun approveStatementsTo(
        destination: MutableTypeStatements,
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        statements: Collection<Implication>
    ) {
        val approveOperationStatements =
            approveOperationStatementsInternal(flow, approvedStatement, statements, shouldRemoveSynthetics = false)
        approveOperationStatements.asMap().forEach { (variable, infos) ->
            for (info in infos) {
                val mutableInfo = info.asMutableStatement()
                destination.put(variable, mutableInfo) {
                    it += mutableInfo
                    it
                }
            }
        }
    }

    override fun collectInfoForBooleanOperator(
        leftFlow: PersistentFlow,
        leftVariable: DataFlowVariable,
        rightFlow: PersistentFlow,
        rightVariable: DataFlowVariable
    ): InfoForBooleanOperator {
        return InfoForBooleanOperator(
            leftFlow.logicStatements[leftVariable] ?: emptyList(),
            rightFlow.logicStatements[rightVariable] ?: emptyList(),
            rightFlow.approvedTypeStatementsDiff
        )
    }

    override fun getImplicationsWithVariable(flow: PersistentFlow, variable: DataFlowVariable): Collection<Implication> {
        return flow.logicStatements[variable] ?: emptyList()
    }

    // --------------------------------------------------------------------\
}

private fun PersistentApprovedTypeStatements.addTypeStatement(info: TypeStatement): PersistentApprovedTypeStatements {
    val variable = info.variable
    val existingInfo = this[variable]
    return if (existingInfo == null) {
        val persistentInfo = if (info is PersistentTypeStatement) info else info.toPersistent()
        put(variable, persistentInfo)
    } else {
        put(variable, existingInfo + info)
    }
}

private fun TypeStatement.toPersistent(): PersistentTypeStatement = PersistentTypeStatement(
    variable,
    exactType.toPersistentSet(),
    exactNotType.toPersistentSet()
)

fun TypeStatement.asMutableStatement(): MutableTypeStatement = when (this) {
    is MutableTypeStatement -> this
    is PersistentTypeStatement -> MutableTypeStatement(variable, exactType.toMutableSet(), exactNotType.toMutableSet())
    else -> throw IllegalArgumentException("Unknown TypeStatement type: ${this::class}")
}
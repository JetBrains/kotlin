/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import java.util.*
import kotlin.collections.LinkedHashSet

abstract class ApprovedOperationStatement {

    abstract val variable: RealVariable
    abstract val operations: Set<Operation>

    val isEmpty: Boolean get() = operations.isEmpty()

    abstract operator fun plus(other: ApprovedOperationStatement): ApprovedOperationStatement
}

class MutableApprovedOperation(
    override val variable: RealVariable,
    override val operations: MutableSet<Operation> = mutableSetOf()
) : ApprovedOperationStatement() {

    override fun plus(other: ApprovedOperationStatement): MutableApprovedOperation {
        return MutableApprovedOperation(variable, LinkedHashSet(operations).apply { addAll(other.operations) })
    }
}

class PersistentApprovedOperation(
    override val variable: RealVariable,
    override val operations: PersistentSet<Operation>
) : ApprovedOperationStatement() {

    override fun plus(other: ApprovedOperationStatement): PersistentApprovedOperation {
        return PersistentApprovedOperation(variable, operations + other.operations)
    }
}

typealias ApprovedOperations = Map<RealVariable, ApprovedOperationStatement>
typealias MutableApprovedOperations = MutableMap<RealVariable, MutableApprovedOperation>
typealias PersistentApprovedOperations = PersistentMap<RealVariable, PersistentApprovedOperation>

fun MutableApprovedOperations.addOperations(operations: ApprovedOperationStatement) {
    put(operations.variable, operations.asMutableStatement()) { it + operations }
}

fun MutableApprovedOperations.mergeApprovedOperations(other: ApprovedOperations) {
    other.forEach { (_, info) ->
        addOperations(info)
    }
}

private fun ApprovedOperationStatement.toPersistent(): PersistentApprovedOperation = PersistentApprovedOperation(
    variable,
    operations.toPersistentSet()
)

fun ApprovedOperationStatement.asMutableStatement(): MutableApprovedOperation = when (this) {
    is MutableApprovedOperation -> this
    is PersistentApprovedOperation -> MutableApprovedOperation(variable, operations.toMutableSet())
    else -> throw IllegalArgumentException("Unknown ApprovedOperation type: ${this::class}")
}

class PersistentOperationFlow : AbstractPersistentFlow<PersistentOperationFlow> {
    var approvedOperations: PersistentApprovedOperations
    var approvedOperationsDiff: PersistentApprovedOperations = persistentHashMapOf()

    constructor(previousFlow: PersistentOperationFlow) : super(previousFlow) {
        approvedOperations = previousFlow.approvedOperations
    }

    constructor() : super(null) {
        approvedOperations = persistentHashMapOf()
    }

    override fun getImplications(variable: DataFlowVariable): Collection<Implication> = logicStatements[variable] ?: emptyList()
    fun getApprovedOperationStatement(variable: RealVariable): ApprovedOperationStatement? = approvedOperations[variable]
    fun getApprovedOperations(variable: RealVariable): Set<Operation> = getApprovedOperationStatement(variable)?.operations ?: emptySet()

    override fun getTypeStatement(variable: RealVariable): TypeStatement? = null
    override fun getVariablesInTypeStatements(): Collection<RealVariable> = emptyList()

    override fun removeOperations(variable: DataFlowVariable): Collection<Implication> {
        return getImplications(variable).also {
            if (it.isNotEmpty()) {
                logicStatements -= variable
            }
        }
    }

    override fun getVariablesWithNewInfo(): Collection<RealVariable> = approvedOperationsDiff.keys
}

abstract class PersistentOperationLogicSystem(context: ConeInferenceContext) : LogicSystem<PersistentOperationFlow>(context) {

    override fun createEmptyFlow(): PersistentOperationFlow = PersistentOperationFlow()

    override fun forkFlow(flow: PersistentOperationFlow): PersistentOperationFlow = PersistentOperationFlow(flow)

    override fun joinFlow(flows: Collection<PersistentOperationFlow>): PersistentOperationFlow {
        return foldFlow(
            flows,
            { variable, commonFlow ->
                getApprovedOperationStatementsDiff(variable, commonFlow)
            },
            { statements ->
                addApprovedStatements(or(statements).takeIf { !it.isEmpty })
            },
            computeVariables = { computeVariablesDiffWithCommonFlow ->
                flows.map {
                    computeVariablesDiffWithCommonFlow(it).toList()
                }.intersectSets().takeIf { it.isNotEmpty() }
            }
        )
    }

    private fun or(statements: Collection<ApprovedOperationStatement>): MutableApprovedOperation {
        require(statements.isNotEmpty())
        val first = statements.first()
        val operations = first.operations.filterTo(mutableSetOf()) {
            statements.all { statement -> statement === first || it in statement.operations }
        }
        return MutableApprovedOperation(first.variable, operations)
    }

    override fun unionFlow(flows: Collection<PersistentOperationFlow>): PersistentOperationFlow {
        return foldFlow(
            flows,
            { variable, commonFlow -> getApprovedOperationStatementsDiff(variable, commonFlow) },
            { addApprovedStatements(and(it)) },
            computeVariables = { computeVariablesDiffWithCommonFlow ->
                flows.flatMapTo(mutableSetOf()) {
                    computeVariablesDiffWithCommonFlow(it)
                }
            }
        )
    }

    private fun and(statements: Collection<ApprovedOperationStatement>): MutableApprovedOperation {
        require(statements.isNotEmpty())
        val first = statements.first()
        val operations = statements.flatMapTo(mutableSetOf()) { it.operations }
        return MutableApprovedOperation(first.variable, operations)
    }

    @OptIn(DfaInternals::class)
    private fun PersistentOperationFlow.getApprovedOperationStatementsDiff(
        variable: RealVariable, parentFlow: PersistentOperationFlow
    ): MutableApprovedOperation {
        var flow = this
        val result = MutableApprovedOperation(variable)
        val variableUnderAlias = directAliasMap[variable]
        if (variableUnderAlias == null) {
            while (flow != parentFlow) {
                flow.approvedOperationsDiff[variable]?.let { result.operations += it.operations }
                flow = flow.previousFlow!!
            }
        } else {
            flow.approvedOperationsDiff[variableUnderAlias.variable]?.let { result.operations += it.operations }
        }
        return result
    }

    private fun PersistentOperationFlow.addApprovedStatements(info: MutableApprovedOperation?) {
        if (info == null) return
        approvedOperations = approvedOperations.addOperationStatement(info)
        if (previousFlow != null) {
            approvedOperationsDiff = approvedOperationsDiff.addOperationStatement(info)
        }
    }

    override fun addTypeStatement(flow: PersistentOperationFlow, statement: TypeStatement) {}

    fun addApprovedOperationStatement(flow: PersistentOperationFlow, statement: ApprovedOperationStatement) {
        if (statement.isEmpty) return
        with(flow) {
            approvedOperations = approvedOperations.addOperationStatement(statement) 
            if (previousFlow != null) {
                approvedOperationsDiff = approvedOperationsDiff.addOperationStatement(statement)
            }
        }
    }

    override fun addImplication(flow: PersistentOperationFlow, implication: Implication) {
        flow.addImplication(implication)
    }

    override fun removeAllAboutVariable(flow: PersistentOperationFlow, variable: RealVariable) {
        flow.logicStatements -= variable
        flow.approvedOperations -= variable
        flow.approvedOperationsDiff -= variable
    }

    override fun translateVariableFromConditionInStatements(
        flow: PersistentOperationFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        shouldRemoveOriginalStatements: Boolean,
        filter: (Implication) -> Boolean,
        transform: (Implication) -> Implication
    ) {
        flow.translateVariableFromConditionInStatements(originalVariable, newVariable, shouldRemoveOriginalStatements, filter, transform)
    }

    override fun approveStatementsInsideFlow(
        flow: PersistentOperationFlow,
        approvedStatement: OperationStatement,
        shouldForkFlow: Boolean,
        shouldRemoveSynthetics: Boolean
    ): PersistentOperationFlow {
        val approvedOperations = approveOperationStatementsInternal(
            flow,
            approvedStatement,
            initialStatements = null,
            shouldRemoveSynthetics
        )

        val resultFlow = if (shouldForkFlow) forkFlow(flow) else flow
        if (approvedOperations.isNotEmpty()) addStatementsToFlow(resultFlow, approvedOperations)
        return resultFlow
    }

    private fun addStatementsToFlow(flow: PersistentOperationFlow, statements: Set<OperationStatement>) {
        for ((variable, operations) in statements.groupBy { it.variable }) {
            val realVariable = variable as? RealVariable ?: continue
            val info = PersistentApprovedOperation(
                realVariable,
                operations.mapTo(mutableSetOf(), { it.operation }).toPersistentSet()
            )
            addApprovedOperationStatement(flow, info)
        }
    }

    private fun approveOperationStatementsInternal(
        flow: PersistentOperationFlow,
        approvedStatement: OperationStatement,
        initialStatements: Collection<Implication>?,
        shouldRemoveSynthetics: Boolean
    ): Set<OperationStatement> {
        val approvedFacts: ArrayListMultimap<RealVariable, TypeStatement> = ArrayListMultimap.create()
        val approvedStatements = LinkedList<OperationStatement>().apply { this += approvedStatement }
        return flow.approveOperationStatementsInternal(approvedStatements, initialStatements, shouldRemoveSynthetics, approvedFacts)
    }

    override fun addLocalVariableAlias(flow: PersistentOperationFlow, alias: RealVariable, underlyingVariable: RealVariableAndType) {
        flow.addLocalVariableAlias(alias, underlyingVariable)
    }

    override fun removeLocalVariableAlias(flow: PersistentOperationFlow, alias: RealVariable) {
        flow.removeLocalVariableAlias(alias)
    }

    override fun getImplicationsWithVariable(flow: PersistentOperationFlow, variable: DataFlowVariable): Collection<Implication> {
        return flow.logicStatements[variable] ?: emptyList()
    }

    override fun collectInfoForBooleanOperator(
        leftFlow: PersistentOperationFlow,
        leftVariable: DataFlowVariable,
        rightFlow: PersistentOperationFlow,
        rightVariable: DataFlowVariable
    ): InfoForBooleanOperator {
        return InfoForBooleanOperator(
            leftFlow.logicStatements[leftVariable] ?: emptyList(),
            rightFlow.logicStatements[rightVariable] ?: emptyList(),
            emptyMap()
        )
    }

    override fun approveStatementsTo(
        destination: MutableTypeStatements,
        flow: PersistentOperationFlow,
        approvedStatement: OperationStatement,
        statements: Collection<Implication>
    ) {
        approveOperationStatementsInternal(flow, approvedStatement, statements, shouldRemoveSynthetics = false)
    }
}

private fun PersistentApprovedOperations.addOperationStatement(info: ApprovedOperationStatement): PersistentApprovedOperations {
    val variable = info.variable
    val existingInfo = this[variable]
    return if (existingInfo == null) {
        val persistentInfo = if (info is PersistentApprovedOperation) info else info.toPersistent()
        put(variable, persistentInfo)
    } else {
        put(variable, existingInfo + info)
    }
}



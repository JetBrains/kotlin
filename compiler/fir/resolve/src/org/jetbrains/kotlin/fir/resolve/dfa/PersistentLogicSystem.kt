/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.symbols.AbstractFirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import java.util.*
import kotlin.NoSuchElementException

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

class PersistentFlow : Flow {
    val previousFlow: PersistentFlow?
    var approvedTypeStatements: PersistentApprovedTypeStatements
    var logicStatements: PersistentImplications
    val level: Int
    var approvedTypeStatementsDiff: PersistentApprovedTypeStatements = persistentHashMapOf()
    var updatedAliasDiff: PersistentSet<RealVariable> = persistentSetOf()

    /*
     * val x = a
     * val y = a
     *
     * directAliasMap: { x -> a, y -> a}
     * backwardsAliasMap: { a -> [x, y] }
     */
    override var directAliasMap: PersistentMap<RealVariable, RealVariable>
    override var backwardsAliasMap: PersistentMap<RealVariable, PersistentList<RealVariable>>

    constructor(previousFlow: PersistentFlow) {
        this.previousFlow = previousFlow
        approvedTypeStatements = previousFlow.approvedTypeStatements
        logicStatements = previousFlow.logicStatements
        level = previousFlow.level + 1

        directAliasMap = previousFlow.directAliasMap
        backwardsAliasMap = previousFlow.backwardsAliasMap
    }

    constructor() {
        previousFlow = null
        approvedTypeStatements = persistentHashMapOf()
        logicStatements = persistentHashMapOf()
        level = 1

        directAliasMap = persistentMapOf()
        backwardsAliasMap = persistentMapOf()
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
}

abstract class PersistentLogicSystem(context: ConeInferenceContext) : LogicSystem<PersistentFlow>(context) {
    override fun createEmptyFlow(): PersistentFlow {
        return PersistentFlow()
    }

    override fun forkFlow(flow: PersistentFlow): PersistentFlow {
        return PersistentFlow(flow)
    }

    override fun joinFlow(flows: Collection<PersistentFlow>): PersistentFlow {
        if (flows.isEmpty()) return createEmptyFlow()
        flows.singleOrNull()?.let { return it }
        val flowsSize = flows.size

        val aliasedVariablesThatDontChangeAlias = mutableMapOf<RealVariable, RealVariable>()
        flows.flatMapTo(mutableSetOf()) { it.directAliasMap.keys }.forEach { aliasedVariable ->
            val originals = flows.map { it.directAliasMap[aliasedVariable] ?: return@forEach }
            if (originals.size != flowsSize) return@forEach
            val firstOriginal = originals.first()
            if (originals.all { it == firstOriginal }) {
                aliasedVariablesThatDontChangeAlias[aliasedVariable] = firstOriginal
            }
        }

        val commonFlow = flows.reduce(::lowestCommonFlow)
        val commonVariables = flows.map {
                it.diffVariablesIterable(commonFlow, aliasedVariablesThatDontChangeAlias.keys).toList()
            }
            .intersectSets()
            .takeIf { it.isNotEmpty() }
            ?: return commonFlow

        for (variable in commonVariables) {
            val info = or(flows.map { it.getApprovedTypeStatementsDiff(variable, commonFlow) })
            if (info.isEmpty) continue
            commonFlow.approvedTypeStatements = commonFlow.approvedTypeStatements.addTypeStatement(info)
            if (commonFlow.previousFlow != null) {
                commonFlow.approvedTypeStatementsDiff = commonFlow.approvedTypeStatementsDiff.addTypeStatement(info)
            }
        }

        for ((alias, underlyingVariable) in aliasedVariablesThatDontChangeAlias) {
            addLocalVariableAlias(commonFlow,alias, underlyingVariable)
        }

        updateAllReceivers(commonFlow)

        return commonFlow
    }

    override fun addLocalVariableAlias(flow: PersistentFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        removeLocalVariableAlias(flow, alias)
        flow.directAliasMap = flow.directAliasMap.put(alias, underlyingVariable)
        flow.backwardsAliasMap = flow.backwardsAliasMap.put(
            underlyingVariable,
            { persistentListOf(alias) },
            { variables -> variables + alias }
        )
    }

    override fun removeLocalVariableAlias(flow: PersistentFlow, alias: RealVariable) {
        flow.updatedAliasDiff += alias
        val original = flow.directAliasMap[alias] ?: return
        flow.directAliasMap = flow.directAliasMap.remove(alias)
        val variables = flow.backwardsAliasMap.getValue(original)
        flow.backwardsAliasMap = flow.backwardsAliasMap.put(original, variables - alias)
    }

    @UseExperimental(DfaInternals::class)
    private fun PersistentFlow.getApprovedTypeStatementsDiff(variable: RealVariable, parentFlow: PersistentFlow): MutableTypeStatement {
        var flow = this
        val result = MutableTypeStatement(variable)
        val variableUnderAlias = directAliasMap[variable]
        if (variableUnderAlias == null) {
            while (flow != parentFlow) {
                flow.approvedTypeStatementsDiff[variable]?.let {
                    result += it
                }
                flow = flow.previousFlow!!
            }
        } else {
            result.exactType += variableUnderAlias.originalType
            flow.approvedTypeStatements[variableUnderAlias]?.let { result += it }
        }
        return result
    }

    /**
     * This is an iterable over real variable that has known facts in flow range
     *   from [this] to [parentFlow]
     */
    private fun PersistentFlow.diffVariablesIterable(
        parentFlow: PersistentFlow,
        aliasedVariablesThatDontChangeAlias: Set<RealVariable>
    ): Iterable<RealVariable> =
        object : DiffIterable<RealVariable>(parentFlow, this) {
            override fun extractIterator(flow: PersistentFlow): Iterator<RealVariable> {
                val variablesWithNewInfo = flow.approvedTypeStatementsDiff.keys
                val updatedVariables = ArrayList(variablesWithNewInfo)
                updatedVariables += flow.updatedAliasDiff
                variablesWithNewInfo.flatMapTo(updatedVariables) { variableWithNewInfo ->
                    flow.backwardsAliasMap[variableWithNewInfo]?.filter { it !in aliasedVariablesThatDontChangeAlias } ?: emptyList()
                }
                return updatedVariables.iterator()
            }
        }

    private abstract class DiffIterable<T>(private val parentFlow: PersistentFlow, private var currentFlow: PersistentFlow) : Iterable<T> {
        private var currentIterator = extractIterator(currentFlow)

        abstract fun extractIterator(flow: PersistentFlow): Iterator<T>

        override fun iterator(): Iterator<T> {
            return object : Iterator<T> {
                override fun hasNext(): Boolean {
                    if (currentIterator.hasNext()) return true
                    while (currentFlow != parentFlow) {
                        currentFlow = currentFlow.previousFlow!!
                        currentIterator = extractIterator(currentFlow)
                        if (currentIterator.hasNext()) return true
                    }
                    return false
                }

                override fun next(): T {
                    if (!hasNext()) {
                        throw NoSuchElementException()
                    }
                    return currentIterator.next()
                }
            }
        }
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
        if ((implication.effect as? TypeStatement)?.isEmpty == true) return
        if (implication.condition == implication.effect) return
        with(flow) {
            val variable = implication.condition.variable
            val existingImplications = logicStatements[variable]
            logicStatements = if (existingImplications == null) {
                logicStatements.put(variable, persistentListOf(implication))
            } else {
                logicStatements.put(variable, existingImplications + implication)
            }
        }
    }

    override fun removeAllAboutVariable(flow: PersistentFlow, variable: RealVariable) {
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
        with(flow) {
            val statements = logicStatements[originalVariable]?.takeIf { it.isNotEmpty() } ?: return
            val newStatements = statements.filter(filter).map {
                val newStatement = OperationStatement(newVariable, it.condition.operation) implies it.effect
                transform(newStatement)
            }.toPersistentList()
            if (shouldRemoveOriginalStatements) {
                logicStatements -= originalVariable
            }
            logicStatements = logicStatements.put(newVariable, newStatements)
        }
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
        if (approvedFacts.isEmpty) return resultFlow

        val updatedReceivers = mutableSetOf<RealVariable>()
        approvedFacts.asMap().forEach { (variable, infos) ->
            var resultInfo = PersistentTypeStatement(variable, persistentSetOf(), persistentSetOf())
            for (info in infos) {
                resultInfo += info
            }
            if (variable.isThisReference) {
                updatedReceivers += variable
            }
            addTypeStatement(resultFlow, resultInfo)
        }

        updatedReceivers.forEach {
            processUpdatedReceiverVariable(resultFlow, it)
        }

        return resultFlow
    }

    private fun approveOperationStatementsInternal(
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        initialStatements: Collection<Implication>?,
        shouldRemoveSynthetics: Boolean
    ): ArrayListMultimap<RealVariable, TypeStatement> {
        val approvedFacts: ArrayListMultimap<RealVariable, TypeStatement> = ArrayListMultimap.create()
        val approvedStatements = LinkedList<OperationStatement>().apply { this += approvedStatement }
        approveOperationStatementsInternal(flow, approvedStatements, initialStatements, shouldRemoveSynthetics, approvedFacts)
        return approvedFacts
    }

    private fun approveOperationStatementsInternal(
        flow: PersistentFlow,
        approvedStatements: LinkedList<OperationStatement>,
        initialStatements: Collection<Implication>?,
        shouldRemoveSynthetics: Boolean,
        approvedTypeStatements: ArrayListMultimap<RealVariable, TypeStatement>
    ) {
        if (approvedStatements.isEmpty()) return
        val approvedOperationStatements = mutableSetOf<OperationStatement>()
        var firstIteration = true
        while (approvedStatements.isNotEmpty()) {
            @Suppress("NAME_SHADOWING")
            val approvedStatement: OperationStatement = approvedStatements.removeFirst()
            // Defense from cycles in facts
            if (!approvedOperationStatements.add(approvedStatement)) {
                continue
            }
            val statements = initialStatements?.takeIf { firstIteration }
                ?: flow.logicStatements[approvedStatement.variable]?.takeIf { it.isNotEmpty() }
                ?: continue
            if (shouldRemoveSynthetics && approvedStatement.variable.isSynthetic()) {
                flow.logicStatements -= approvedStatement.variable
            }
            for (statement in statements) {
                if (statement.condition == approvedStatement) {
                    when (val effect = statement.effect) {
                        is OperationStatement -> approvedStatements += effect
                        is TypeStatement -> approvedTypeStatements.put(effect.variable, effect)
                    }
                }
            }
            firstIteration = false
        }
    }

    override fun approveStatementsTo(
        destination: MutableTypeStatements,
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        statements: Collection<Implication>
    ) {
        val approveOperationStatements = approveOperationStatementsInternal(flow, approvedStatement, statements, shouldRemoveSynthetics = false)
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

private fun lowestCommonFlow(left: PersistentFlow, right: PersistentFlow): PersistentFlow {
    val level = minOf(left.level, right.level)

    @Suppress("NAME_SHADOWING")
    var left = left
    while (left.level > level) {
        left = left.previousFlow!!
    }
    @Suppress("NAME_SHADOWING")
    var right = right
    while (right.level > level) {
        right = right.previousFlow!!
    }
    while (left != right) {
        left = left.previousFlow!!
        right = right.previousFlow!!
    }
    return left
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

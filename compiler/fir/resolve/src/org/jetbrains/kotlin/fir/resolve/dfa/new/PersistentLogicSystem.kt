/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.new

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import java.util.*
import kotlin.NoSuchElementException
import kotlin.collections.HashSet

data class PersistentDataFlowInfo(
    override val variable: RealVariable,
    override val exactType: PersistentSet<ConeKotlinType>,
    override val exactNotType: PersistentSet<ConeKotlinType>
) : DataFlowInfo() {
    override operator fun plus(other: DataFlowInfo): PersistentDataFlowInfo {
        return PersistentDataFlowInfo(
            variable,
            exactType + other.exactType,
            exactNotType + other.exactNotType
        )
    }

    override val isEmpty: Boolean
        get() = exactType.isEmpty() && exactNotType.isEmpty()

    override fun invert(): PersistentDataFlowInfo {
        return PersistentDataFlowInfo(variable, exactNotType, exactType)
    }
}

typealias PersistentKnownFacts = PersistentMap<RealVariable, PersistentDataFlowInfo>
typealias PersistentLogicStatements = PersistentMap<DataFlowVariable, PersistentList<LogicStatement>>

class PersistentFlow : Flow {
    val previousFlow: PersistentFlow?
    var knownFacts: PersistentKnownFacts
    var logicStatements: PersistentLogicStatements
    val level: Int
    var knownFactsDiff: PersistentKnownFacts = persistentHashMapOf()

    constructor(previousFlow: PersistentFlow) {
        this.previousFlow = previousFlow
        knownFacts = previousFlow.knownFacts
        logicStatements = previousFlow.logicStatements
        level = previousFlow.level + 1
    }

    constructor() {
        previousFlow = null
        knownFacts = persistentHashMapOf()
        logicStatements = persistentHashMapOf()
        level = 1
    }

    override fun getKnownInfo(variable: RealVariable): DataFlowInfo? {
        return knownFacts[variable]
    }

    override fun getLogicStatements(variable: DataFlowVariable): Collection<LogicStatement> {
        return logicStatements[variable] ?: emptyList()
    }

    override fun getVariablesInKnownInfos(): Collection<RealVariable> {
        return knownFacts.keys
    }

    override fun removeConditions(variable: DataFlowVariable): Collection<LogicStatement> {
        return getLogicStatements(variable).also {
            if (it.isNotEmpty()) {
                logicStatements -= variable
            }
        }
    }
}

abstract class PersistentLogicSystem(private val anyType: ConeKotlinType, context: ConeInferenceContext) :
    LogicSystem<PersistentFlow>(context) {
    override fun createEmptyFlow(): PersistentFlow {
        return PersistentFlow()
    }

    override fun forkFlow(flow: PersistentFlow): PersistentFlow {
        return PersistentFlow(flow)
    }

    override fun joinFlow(flows: Collection<PersistentFlow>): PersistentFlow {
        if (flows.isEmpty()) return createEmptyFlow()
        flows.singleOrNull()?.let { return it }
        val commonFlow = flows.reduce(::lowestCommonFlow)
        val commonVariables = flows.map { it.diffVariablesIterable(commonFlow).toList() }
            .intersectSets()
            .takeIf { it.isNotEmpty() }
            ?: return commonFlow

        for (variable in commonVariables) {
            val info = or(flows.map { it.getKnownFactsDiff(variable, commonFlow) })
            if (info.isEmpty) continue
            commonFlow.knownFacts = commonFlow.knownFacts.addNewInfo(info)
            if (commonFlow.previousFlow != null) {
                commonFlow.knownFactsDiff = commonFlow.knownFactsDiff.addNewInfo(info)
            }
        }

        updateAllReceivers(commonFlow)

        return commonFlow
    }

    private fun PersistentFlow.getKnownFactsDiff(variable: RealVariable, parentFlow: PersistentFlow): MutableDataFlowInfo {
        var flow = this
        val result = MutableDataFlowInfo(variable)
        while (flow != parentFlow) {
            flow.knownFactsDiff[variable]?.let {
                result += it
            }
            flow = flow.previousFlow!!
        }
        return result
    }

    /**
     * This is an iterable over real variable that has known facts in flow range
     *   from [this] to [parentFlow]
     */
    private fun PersistentFlow.diffVariablesIterable(parentFlow: PersistentFlow): Iterable<RealVariable> =
        object : DiffIterable<RealVariable>(parentFlow, this) {
            override fun extractIterator(flow: PersistentFlow): Iterator<RealVariable> {
                return flow.knownFactsDiff.keys.iterator()
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

    override fun addKnownInfo(flow: PersistentFlow, info: DataFlowInfo) {
        with(flow) {
            knownFacts = knownFacts.addNewInfo(info)
            if (previousFlow != null) {
                knownFactsDiff = knownFactsDiff.addNewInfo(info)
            }
            if (info.variable.isThisReference) {
                processUpdatedReceiverVariable(flow, info.variable)
            }
        }
    }

    override fun addLogicStatement(flow: PersistentFlow, statement: LogicStatement) {
        if (statement.condition == statement.effect) return
        with(flow) {
            val variable = statement.condition.variable
            val existingFacts = logicStatements[variable]
            logicStatements = if (existingFacts == null) {
                logicStatements.put(variable, persistentListOf(statement))
            } else {
                logicStatements.put(variable, existingFacts + statement)
            }
        }
    }

    override fun removeAllAboutVariable(flow: PersistentFlow, variable: RealVariable) {
        flow.knownFacts -= variable
        flow.knownFactsDiff -= variable
        // TODO: should we search variable in all logic statements?
    }

    override fun translateConditionalVariableInStatements(
        flow: PersistentFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        shouldRemoveOriginalStatements: Boolean,
        filter: (LogicStatement) -> Boolean,
        transform: (LogicStatement) -> LogicStatement
    ) {
        with(flow) {
            val statements = logicStatements[originalVariable]?.takeIf { it.isNotEmpty() } ?: return
            val newStatements = statements.filter(filter).map {
                val newStatement = Predicate(newVariable, it.condition.condition) implies it.effect
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
        predicate: Predicate,
        shouldForkFlow: Boolean,
        shouldRemoveSynthetics: Boolean
    ): PersistentFlow {
        val approvedFacts = approvePredicatesInternal(
            flow,
            predicate,
            initialStatements = null,
            shouldRemoveSynthetics
        )

        val resultFlow = if (shouldForkFlow) forkFlow(flow) else flow
        if (approvedFacts.isEmpty) return resultFlow

        val updatedReceivers = mutableSetOf<RealVariable>()
        approvedFacts.asMap().forEach { (variable, infos) ->
            var resultInfo = PersistentDataFlowInfo(variable, persistentSetOf(), persistentSetOf())
            for (info in infos) {
                resultInfo += info
            }
            if (variable.isThisReference) {
                updatedReceivers += variable
            }
            addKnownInfo(resultFlow, resultInfo)
        }

        updatedReceivers.forEach {
            processUpdatedReceiverVariable(resultFlow, it)
        }

        return resultFlow
    }

    private fun approvePredicatesInternal(
        flow: PersistentFlow,
        predicate: Predicate,
        initialStatements: Collection<LogicStatement>?,
        shouldRemoveSynthetics: Boolean
    ): ArrayListMultimap<RealVariable, DataFlowInfo> {
        val approvedFacts: ArrayListMultimap<RealVariable, DataFlowInfo> = ArrayListMultimap.create()
        val predicatesToApprove = LinkedList<Predicate>().apply { this += predicate }
        approvePredicatesInternal(flow, predicatesToApprove, initialStatements, shouldRemoveSynthetics, approvedFacts)
        return approvedFacts
    }

    private fun approvePredicatesInternal(
        flow: PersistentFlow,
        predicatesToApprove: LinkedList<Predicate>,
        initialStatements: Collection<LogicStatement>?,
        shouldRemoveSynthetics: Boolean,
        approvedFacts: ArrayListMultimap<RealVariable, DataFlowInfo>
    ) {
        if (predicatesToApprove.isEmpty()) return
        val approvedVariables = mutableSetOf<RealVariable>()
        val approvedPredicates = mutableSetOf<Predicate>()
        var firstIteration = true
        while (predicatesToApprove.isNotEmpty()) {
            @Suppress("NAME_SHADOWING")
            val predicate: Predicate = predicatesToApprove.removeFirst()
            // Defense from cycles in facts
            if (!approvedPredicates.add(predicate)) {
                continue
            }
            val statements = initialStatements?.takeIf { firstIteration }
                ?: flow.logicStatements[predicate.variable]?.takeIf { it.isNotEmpty() }
                ?: continue
            if (shouldRemoveSynthetics && predicate.variable.isSynthetic()) {
                flow.logicStatements -= predicate.variable
            }
            for (statement in statements) {
                if (statement.condition == predicate) {
                    when (val effect = statement.effect) {
                        is Predicate -> predicatesToApprove += effect
                        is DataFlowInfo -> {
                            approvedFacts.put(effect.variable, effect)
                            approvedVariables += effect.variable
                        }
                    }
                }
            }
            firstIteration = false
        }

        val newPredicates = LinkedList<Predicate>()
        for (approvedVariable in approvedVariables) {
            var variable = approvedVariable
            foo@ while (variable.explicitReceiverVariable != null && variable.isSafeCall) {
                when (val receiver = variable.explicitReceiverVariable!!) {
                    is RealVariable -> {
                        approvedFacts.put(receiver, receiver has anyType)
                        variable = receiver
                    }
                    is SyntheticVariable -> {
                        newPredicates += receiver notEq null
                        break@foo
                    }
                    else -> throw IllegalStateException()
                }
            }
        }
        approvePredicatesInternal(flow, newPredicates, initialStatements = null, shouldRemoveSynthetics, approvedFacts)
    }

    override fun approvePredicateTo(
        destination: MutableKnownFacts,
        flow: PersistentFlow,
        predicate: Predicate,
        statements: Collection<LogicStatement>
    ) {
        val approvePredicates = approvePredicatesInternal(flow, predicate, statements, shouldRemoveSynthetics = false)
        approvePredicates.asMap().forEach { (variable, infos) ->
            for (info in infos) {
                val mutableInfo = info.asMutableInfo()
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
            rightFlow.knownFactsDiff
        )
    }

    override fun getLogicStatementsWithVariable(flow: PersistentFlow, variable: DataFlowVariable): Collection<LogicStatement> {
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

private fun <E> Collection<Collection<E>>.intersectSets(): Set<E> {
    if (isEmpty()) return emptySet()
    val iterator = iterator()
    val result = HashSet<E>(iterator.next())
    while (iterator.hasNext()) {
        result.retainAll(iterator.next())
    }
    return result
}

private fun PersistentKnownFacts.addNewInfo(info: DataFlowInfo): PersistentKnownFacts {
    val variable = info.variable
    val existingInfo = this[variable]
    return if (existingInfo == null) {
        val persistentInfo = if (info is PersistentDataFlowInfo) info else info.toPersistent()
        put(variable, persistentInfo)
    } else {
        put(variable, existingInfo + info)
    }
}

private fun DataFlowInfo.toPersistent(): PersistentDataFlowInfo = PersistentDataFlowInfo(
    variable,
    exactType.toPersistentSet(),
    exactNotType.toPersistentSet()
)

fun DataFlowInfo.asMutableInfo(): MutableDataFlowInfo = when (this) {
    is MutableDataFlowInfo -> this
    is PersistentDataFlowInfo -> MutableDataFlowInfo(variable, exactType.toMutableSet(), exactNotType.toMutableSet())
    else -> throw IllegalArgumentException("Unknown DataFlowInfo type: ${this::class}")
}

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList
import java.util.*
import kotlin.NoSuchElementException

fun computeAliasesThatDontChange(
    flows: Collection<Flow>
): MutableMap<RealVariable, RealVariableAndType> {
    val flowsSize = flows.size
    val aliasedVariablesThatDontChangeAlias = mutableMapOf<RealVariable, RealVariableAndType>()

    flows.flatMapTo(mutableSetOf()) { it.directAliasMap.keys }.forEach { aliasedVariable ->
        val originals = flows.map { it.directAliasMap[aliasedVariable] ?: return@forEach }
        if (originals.size != flowsSize) return@forEach
        val firstOriginal = originals.first()
        if (originals.all { it == firstOriginal }) {
            aliasedVariablesThatDontChangeAlias[aliasedVariable] = firstOriginal
        }
    }

    return aliasedVariablesThatDontChangeAlias
}

fun <FLOW : AbstractPersistentFlow<FLOW>> lowestCommonFlow(left: FLOW, right: FLOW): FLOW {
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

/**
 * This is an iterable over real variable that has known facts in flow range
 *   from [this] to [parentFlow]
 */
fun <FLOW : AbstractPersistentFlow<FLOW>> FLOW.diffVariablesIterable(
    parentFlow: FLOW,
    aliasedVariablesThatDontChangeAlias: Set<RealVariable>
): Iterable<RealVariable> =
    object : DiffIterable<RealVariable, FLOW>(parentFlow, this) {
        override fun extractIterator(flow: FLOW): Iterator<RealVariable> {
            val variablesWithNewInfo = flow.getVariablesWithNewInfo()
            val updatedVariables = ArrayList(variablesWithNewInfo)
            updatedVariables += flow.updatedAliasDiff
            variablesWithNewInfo.flatMapTo(updatedVariables) { variableWithNewInfo ->
                flow.backwardsAliasMap[variableWithNewInfo]?.filter { it !in aliasedVariablesThatDontChangeAlias } ?: emptyList()
            }
            return updatedVariables.iterator()
        }
    }

private abstract class DiffIterable<T, FLOW : AbstractPersistentFlow<FLOW>>(private val parentFlow: FLOW, private var currentFlow: FLOW) :
    Iterable<T> {
    @Suppress("LeakingThis")
    private var currentIterator = extractIterator(currentFlow)

    abstract fun extractIterator(flow: FLOW): Iterator<T>

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

inline fun <FLOW : AbstractPersistentFlow<FLOW>, DATA> LogicSystem<FLOW>.foldFlow(
    flows: Collection<FLOW>,
    diffExtractor: FLOW.(RealVariable, commonFlow: FLOW) -> DATA,
    updater: FLOW.(data: Collection<DATA>) -> Unit,
    computeVariables: (computeVariablesDiffWithCommonFlow: (FLOW) -> Iterable<RealVariable>) -> Collection<RealVariable>?
): FLOW {
    if (flows.isEmpty()) return createEmptyFlow()
    flows.singleOrNull()?.let { return it }

    val aliasedVariablesThatDontChangeAlias = computeAliasesThatDontChange(flows)

    val commonFlow = flows.reduce(::lowestCommonFlow)
    val variables =
        computeVariables { it.diffVariablesIterable(commonFlow, aliasedVariablesThatDontChangeAlias.keys) } ?: return commonFlow

    for (variable in variables) {
        val data = flows.map { it.diffExtractor(variable, commonFlow) }
        commonFlow.updater(data)
    }

    commonFlow.addVariableAliases(aliasedVariablesThatDontChangeAlias)
    updateAllReceivers(commonFlow)
    return commonFlow
}

fun <FLOW : AbstractPersistentFlow<FLOW>> FLOW.addImplication(implication: Implication) {
    if ((implication.effect as? TypeStatement)?.isEmpty == true) return
    if (implication.condition == implication.effect) return

    val variable = implication.condition.variable
    val existingImplications = logicStatements[variable]

    logicStatements = logicStatements.put(variable, existingImplications?.plus(implication) ?: persistentListOf(implication))
}

inline fun <FLOW : AbstractPersistentFlow<FLOW>> FLOW.translateVariableFromConditionInStatements(
    originalVariable: DataFlowVariable,
    newVariable: DataFlowVariable,
    shouldRemoveOriginalStatements: Boolean,
    filter: (Implication) -> Boolean,
    transform: (Implication) -> Implication
) {
    val statements = logicStatements[originalVariable]?.takeIf { it.isNotEmpty() } ?: return
    val newStatements = statements.filter(filter).map {
        val newStatement = OperationStatement(newVariable, it.condition.operation) implies it.effect
        transform(newStatement)
    }.toPersistentList()
    if (shouldRemoveOriginalStatements) {
        logicStatements -= originalVariable
    }
    logicStatements = logicStatements.put(newVariable, logicStatements[newVariable]?.let { it + newStatements } ?: newStatements)
}

fun <FLOW : AbstractPersistentFlow<FLOW>> FLOW.approveOperationStatementsInternal(
    approvedStatements: LinkedList<OperationStatement>,
    initialStatements: Collection<Implication>?,
    shouldRemoveSynthetics: Boolean,
    approvedTypeStatements: ArrayListMultimap<RealVariable, TypeStatement>
): Set<OperationStatement> {
    if (approvedStatements.isEmpty()) return emptySet()
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
            ?: logicStatements[approvedStatement.variable]?.takeIf { it.isNotEmpty() }
            ?: continue
        if (shouldRemoveSynthetics && approvedStatement.variable.isSynthetic()) {
            logicStatements -= approvedStatement.variable
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
    return approvedOperationStatements
}

fun <FLOW : AbstractPersistentFlow<FLOW>> FLOW.addLocalVariableAlias(alias: RealVariable, underlyingVariable: RealVariableAndType) {
    removeLocalVariableAlias(alias)
    directAliasMap = directAliasMap.put(alias, underlyingVariable)
    backwardsAliasMap = backwardsAliasMap.put(
        underlyingVariable.variable,
        { persistentListOf(alias) },
        { variables -> variables + alias }
    )
}

fun <FLOW : AbstractPersistentFlow<FLOW>> FLOW.removeLocalVariableAlias(alias: RealVariable) {
    updatedAliasDiff += alias
    val original = directAliasMap[alias]?.variable ?: return
    directAliasMap = directAliasMap.remove(alias)
    val variables = backwardsAliasMap.getValue(original)
    backwardsAliasMap = backwardsAliasMap.put(original, variables - alias)
}
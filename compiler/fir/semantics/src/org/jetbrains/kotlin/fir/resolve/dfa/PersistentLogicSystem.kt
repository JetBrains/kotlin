/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.types.AbstractTypeChecker
import java.util.*
import kotlin.math.max

data class PersistentTypeStatement(
    override val variable: RealVariable,
    override val exactType: PersistentSet<ConeKotlinType>,
) : TypeStatement()

typealias PersistentApprovedTypeStatements = PersistentMap<RealVariable, PersistentTypeStatement>
typealias PersistentImplications = PersistentMap<DataFlowVariable, PersistentList<Implication>>

class PersistentFlow : Flow {
    val previousFlow: PersistentFlow?
    override var approvedTypeStatements: PersistentApprovedTypeStatements
    var logicStatements: PersistentImplications
    val level: Int

    /*
     * val x = a
     * val y = a
     *
     * directAliasMap: { x -> a, y -> a}
     * backwardsAliasMap: { a -> [x, y] }
     */
    var directAliasMap: PersistentMap<RealVariable, RealVariable>
    var backwardsAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>

    var assignmentIndex: PersistentMap<RealVariable, Int>

    constructor(previousFlow: PersistentFlow) {
        this.previousFlow = previousFlow
        approvedTypeStatements = previousFlow.approvedTypeStatements
        logicStatements = previousFlow.logicStatements
        level = previousFlow.level + 1

        directAliasMap = previousFlow.directAliasMap
        backwardsAliasMap = previousFlow.backwardsAliasMap
        assignmentIndex = previousFlow.assignmentIndex
    }

    constructor() {
        previousFlow = null
        approvedTypeStatements = persistentHashMapOf()
        logicStatements = persistentHashMapOf()
        level = 1

        directAliasMap = persistentMapOf()
        backwardsAliasMap = persistentMapOf()
        assignmentIndex = persistentMapOf()
    }

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    fun getTypeStatement(variable: RealVariable): TypeStatement =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable) ?: MutableTypeStatement(variable)

    override fun getType(variable: RealVariable): Set<ConeKotlinType>? =
        approvedTypeStatements[unwrapVariable(variable)]?.exactType
}

abstract class PersistentLogicSystem(context: ConeInferenceContext) : LogicSystem<PersistentFlow>(context) {
    abstract val variableStorage: VariableStorageImpl

    override fun createEmptyFlow(): PersistentFlow =
        PersistentFlow()

    override fun forkFlow(flow: PersistentFlow): PersistentFlow =
        PersistentFlow(flow)

    override fun copyAllInformation(from: PersistentFlow, to: PersistentFlow) {
        to.approvedTypeStatements = from.approvedTypeStatements
        to.logicStatements = from.logicStatements
        to.directAliasMap = from.directAliasMap
        to.backwardsAliasMap = from.backwardsAliasMap
        to.assignmentIndex = from.assignmentIndex
    }

    override fun joinFlow(flows: Collection<PersistentFlow>): PersistentFlow =
        foldFlow(flows, allExecute = false)

    override fun unionFlow(flows: Collection<PersistentFlow>): PersistentFlow =
        foldFlow(flows, allExecute = true)

    private fun foldFlow(flows: Collection<PersistentFlow>, allExecute: Boolean): PersistentFlow {
        when (flows.size) {
            0 -> return createEmptyFlow()
            1 -> return forkFlow(flows.first())
        }

        val commonFlow = forkFlow(flows.reduce(::lowestCommonFlow))
        // Aliases that have occurred before branching should already be in `commonFlow`,
        // but it might be useful to also add aliases that happen in all branches, e.g.
        // the aliasing of `y` to `a.x` after `if (p) { y = a.x } else { y = a.x }`.
        val commonAliases = computeCommonAliases(flows)
        // Computing the statements for these aliases is redundant as the result is equal
        // to the statements for whatever they are aliasing.
        val variables = flows.flatMapTo(mutableSetOf()) { it.approvedTypeStatements.keys + it.directAliasMap.keys } - commonAliases.keys
        val statements = variables.mapNotNull { variable ->
            val statement = if (allExecute) {
                // All input flows execute in some order. If none of the flows reassign, i.e. the only key
                // in `byAssignment` is `commonFlow`'s assignment index, then all statements are true.
                // Otherwise, one of the assignments executed last, but we don't know which, and the flows
                // that don't reassign should be ignored because they may have executed before or after that.
                val byAssignment = flows.groupByTo(mutableMapOf()) { it.assignmentIndex[variable] ?: -1 }
                if (byAssignment.size > 1) {
                    byAssignment.remove(commonFlow.assignmentIndex[variable] ?: -1)
                }
                or(byAssignment.values.map { flowSubset -> and(flowSubset.map { it.getTypeStatement(variable) }) })
            } else {
                // One input flow executes - one set of statements is true, others might be false.
                or(flows.map { it.getTypeStatement(variable) })
            }
            if (statement.isNotEmpty) variable to statement.toPersistent() else null
        }

        // If a variable was reassigned in one branch, it was reassigned at the join point.
        val reassignedVariables = mutableMapOf<RealVariable, Int>()
        for (flow in flows) {
            for ((variable, index) in flow.assignmentIndex) {
                if (commonFlow.assignmentIndex[variable] != index) {
                    // Ideally we should generate an entirely new index here, but it doesn't really
                    // matter; the important part is that it's different from `commonFlow.previousFlow`.
                    reassignedVariables[variable] = max(index, reassignedVariables[variable] ?: 0)
                }
            }
        }
        for ((variable, index) in reassignedVariables) {
            recordNewAssignment(commonFlow, variable, index)
        }

        commonFlow.approvedTypeStatements += statements

        for ((alias, underlyingVariable) in commonAliases) {
            addLocalVariableAlias(commonFlow, alias, underlyingVariable)
        }
        return commonFlow
    }

    private fun computeCommonAliases(flows: Collection<PersistentFlow>): Map<RealVariable, RealVariable> =
        flows.first().directAliasMap.filterTo(mutableMapOf()) { (variable, alias) ->
            flows.all { it.directAliasMap[variable] == alias }
        }

    override fun addLocalVariableAlias(flow: PersistentFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        flow.addAliases(persistentSetOf(alias), flow.unwrapVariable(underlyingVariable))
    }

    override fun removeAllAboutVariable(flow: PersistentFlow, variable: RealVariable) {
        flow.replaceVariable(variable, null)
    }

    private fun PersistentFlow.replaceVariable(variable: RealVariable, replacement: RealVariable?) {
        val original = directAliasMap[variable]
        if (original != null) {
            // All statements should've been made about whatever variable this is an alias to. There is nothing to replace.
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                assert(variable !in backwardsAliasMap)
                assert(variable !in logicStatements)
                assert(variable !in approvedTypeStatements)
            }
            // `variable.dependentVariables` is not separated by flow, so it may be non-empty if aliasing of this variable
            // was broken in another flow. However, in *this* flow dependent variables should have no information attached to them.

            val siblings = backwardsAliasMap.getValue(original).remove(variable)
            directAliasMap -= variable
            backwardsAliasMap = if (siblings.isNotEmpty()) {
                backwardsAliasMap.put(original, siblings)
            } else {
                backwardsAliasMap.remove(original)
            }
            if (replacement != null) {
                addLocalVariableAlias(this, replacement, original)
            }
        } else {
            val aliases = backwardsAliasMap[variable]
            // If asked to remove the variable but there are aliases, replace with a new representative for the alias group instead.
            val replacementOrNext = replacement ?: aliases?.first()
            for (dependent in variable.dependentVariables) {
                replaceVariable(dependent, replacementOrNext?.let {
                    variableStorage.copyRealVariableWithRemapping(dependent, variable, it)
                })
            }
            logicStatements = logicStatements.replaceVariable(variable, replacementOrNext)
            approvedTypeStatements = approvedTypeStatements.replaceVariable(variable, replacementOrNext)
            if (aliases != null) {
                backwardsAliasMap -= variable
                if (replacementOrNext != null) {
                    directAliasMap -= replacementOrNext
                    addAliases(aliases, replacementOrNext)
                }
            }
        }
    }

    private fun PersistentFlow.addAliases(aliases: PersistentSet<RealVariable>, target: RealVariable) {
        val withoutSelf = aliases - target
        if (withoutSelf.isNotEmpty()) {
            directAliasMap += withoutSelf.map { it to target }
            backwardsAliasMap = backwardsAliasMap.put(target, { withoutSelf }, { it + withoutSelf })
        }
    }

    override fun addTypeStatement(flow: PersistentFlow, statement: TypeStatement) {
        if (statement.exactType.isEmpty()) return
        val variable = statement.variable
        val oldExactType = flow.approvedTypeStatements[variable]?.exactType
        val newExactType = oldExactType?.addAll(statement.exactType) ?: statement.exactType.toPersistentSet()
        if (newExactType === oldExactType) return
        flow.approvedTypeStatements = flow.approvedTypeStatements.put(variable, PersistentTypeStatement(variable, newExactType))
        if (variable.isThisReference) {
            processUpdatedReceiverVariable(flow, variable)
        }
    }

    override fun addImplication(flow: PersistentFlow, implication: Implication) {
        val effect = implication.effect
        if (effect == implication.condition) return
        if (effect is TypeStatement && (effect.isEmpty ||
                    flow.approvedTypeStatements[effect.variable]?.exactType?.containsAll(effect.exactType) == true)
        ) return
        val variable = implication.condition.variable
        flow.logicStatements = flow.logicStatements.put(variable, { persistentListOf(implication) }, { it + implication })
    }

    override fun translateVariableFromConditionInStatements(
        flow: PersistentFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        shouldRemoveOriginalStatements: Boolean,
        filter: (Implication) -> Boolean,
        transform: (Implication) -> Implication?
    ) {
        with(flow) {
            val statements = logicStatements[originalVariable]?.takeIf { it.isNotEmpty() } ?: return
            val newStatements = statements.filter(filter).mapNotNull {
                val newStatement = OperationStatement(newVariable, it.condition.operation) implies it.effect
                transform(newStatement)
            }.toPersistentList()
            if (shouldRemoveOriginalStatements) {
                logicStatements -= originalVariable
            }
            logicStatements = logicStatements.put(newVariable, logicStatements[newVariable]?.let { it + newStatements } ?: newStatements)
        }
    }

    override fun commitOperationStatement(flow: PersistentFlow, statement: OperationStatement, shouldRemoveSynthetics: Boolean) {
        approveOperationStatementsInternal(flow, statement, shouldRemoveSynthetics).values.forEach {
            addTypeStatement(flow, it)
        }
    }

    private fun approveOperationStatementsInternal(
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        shouldRemoveSynthetics: Boolean
    ): TypeStatements {
        val approvedTypeStatements: ArrayListMultimap<RealVariable, TypeStatement> = ArrayListMultimap.create()
        val queue = LinkedList<OperationStatement>().apply { this += approvedStatement }
        val approved = mutableSetOf<OperationStatement>()
        while (queue.isNotEmpty()) {
            val next: OperationStatement = queue.removeFirst()
            // Defense from cycles in facts
            if (!approved.add(next)) continue
            val variable = next.variable
            val statements = flow.logicStatements[variable]?.takeIf { it.isNotEmpty() } ?: continue
            if (shouldRemoveSynthetics && variable.isSynthetic()) {
                flow.logicStatements -= variable
            }
            for (statement in statements) {
                if (statement.condition == next) {
                    when (val effect = statement.effect) {
                        is OperationStatement -> queue += effect
                        is TypeStatement -> approvedTypeStatements.put(effect.variable, effect)
                    }
                }
            }
        }
        return approvedTypeStatements.asMap().mapValues { and(it.value) }
    }

    override fun approveOperationStatement(
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
    ): TypeStatements = approveOperationStatementsInternal(flow, approvedStatement, shouldRemoveSynthetics = false)

    override fun getImplicationsWithVariable(flow: PersistentFlow, variable: DataFlowVariable): Collection<Implication> {
        return flow.logicStatements[variable] ?: emptyList()
    }

    override fun recordNewAssignment(flow: PersistentFlow, variable: RealVariable, index: Int) {
        removeAllAboutVariable(flow, variable)
        flow.assignmentIndex = flow.assignmentIndex.put(variable, index)
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

private fun TypeStatement.toPersistent(): PersistentTypeStatement = when (this) {
    is PersistentTypeStatement -> this
    else -> PersistentTypeStatement(variable, exactType.toPersistentSet())
}

@JvmName("replaceVariableInStatements")
private fun PersistentApprovedTypeStatements.replaceVariable(from: RealVariable, to: RealVariable?): PersistentApprovedTypeStatements {
    val existing = this[from] ?: return this
    return if (to != null) remove(from).put(to, existing.copy(variable = to)) else remove(from)
}

@JvmName("replaceVariableInImplications")
private fun PersistentImplications.replaceVariable(from: RealVariable, to: RealVariable?): PersistentImplications =
    mutate { result ->
        for ((variable, implications) in this) {
            val newImplications = if (to != null) {
                implications.replaceAll { it.replaceVariable(from, to) }
            } else {
                implications.removeAll { it.effect.variable == from }
            }
            if (newImplications.isNotEmpty()) {
                result[implications.first().condition.variable] = newImplications
            } else {
                result.remove(variable)
            }
        }
        result.remove(from)
    }

private inline fun <T> PersistentList<T>.replaceAll(block: (T) -> T): PersistentList<T> =
    mutate { result ->
        val it = result.listIterator()
        while (it.hasNext()) {
            it.set(block(it.next()))
        }
    }

private fun Implication.replaceVariable(from: RealVariable, to: RealVariable): Implication = when {
    condition.variable == from -> Implication(condition.copy(variable = to), effect.replaceVariable(from, to))
    effect.variable == from -> Implication(condition, effect.replaceVariable(from, to))
    else -> this
}

private fun Statement.replaceVariable(from: RealVariable, to: RealVariable): Statement =
    if (variable != from) this else when (this) {
        is OperationStatement -> copy(variable = to)
        is PersistentTypeStatement -> copy(variable = to)
        is MutableTypeStatement -> MutableTypeStatement(to, exactType)
    }

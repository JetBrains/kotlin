/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.types.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import java.util.*
import kotlin.math.max

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

    override fun unwrapVariable(variable: RealVariable): RealVariable {
        return directAliasMap[variable] ?: variable
    }

    fun getTypeStatement(variable: RealVariable): TypeStatement {
        val result = MutableTypeStatement(variable)
        approvedTypeStatements[variable]?.let { result += it }
        val variableUnderAlias = directAliasMap[variable]
        if (variableUnderAlias != null) {
            approvedTypeStatements[variableUnderAlias]?.let { result += it }
        }
        return result
    }

    override fun getType(variable: RealVariable): Set<ConeKotlinType> {
        return getTypeStatement(variable).exactType
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
        // One input flow executes - one set of statements is true, others might be false.
        return foldFlow(flows) { variable -> or(flows.map { it.getTypeStatement(variable) }).takeIf { it.isNotEmpty } }
    }

    override fun unionFlow(flows: Collection<PersistentFlow>): PersistentFlow {
        // All input flows execute in some order. If none of them reassign the variable, then
        // *all* sets of statements are true; otherwise the assignments might have happened in
        // an arbitrary order, so only one set of statements is true depending on which assignment
        // happened last (and the flows that don't reassign may or may not have executed after that).
        return foldFlow(flows) { variable ->
            or(flows.groupBy { it.assignmentIndex[variable] ?: -1 }.values.map { flowSubset ->
                and(flowSubset.map { it.getTypeStatement(variable) })
            })
        }
    }

    private inline fun foldFlow(flows: Collection<PersistentFlow>, mergeOperation: (RealVariable) -> TypeStatement?): PersistentFlow =
        when (flows.size) {
            0 -> createEmptyFlow()
            1 -> flows.first()
            else -> foldFlow(flows, flows.flatMapTo(mutableSetOf()) { it.approvedTypeStatements.keys }.mapNotNull(mergeOperation))
        }

    private fun foldFlow(flows: Collection<PersistentFlow>, statements: Collection<TypeStatement>): PersistentFlow {
        val commonFlow = flows.reduce(::lowestCommonFlow)
        // Aliases that have occurred before branching should already be in `commonFlow`,
        // but it might be useful to also add aliases that happen in all branches, e.g.
        // the aliasing of `y` to `a.x` after `if (p) { y = a.x } else { y = a.x }`.
        val commonAliases = computeCommonAliases(flows)

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

        val toReplace = statements.map { it.variable to it.toPersistent() }
        commonFlow.approvedTypeStatements += toReplace
        if (commonFlow.previousFlow != null) {
            commonFlow.approvedTypeStatementsDiff += toReplace
        }

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

    override fun removeLocalVariableAlias(flow: PersistentFlow, alias: RealVariable) {
        val original = flow.directAliasMap[alias]
        val backAliases = flow.backwardsAliasMap[alias]
        flow.directAliasMap -= alias
        flow.backwardsAliasMap -= alias

        // Move all statements to the next representative of the equivalence set
        val replacement = original ?: backAliases?.first()
        if (replacement != null) {
            flow.logicStatements[alias]?.let { statements ->
                val newStatements = statements.map { it.replaceVariable(alias, replacement) }
                flow.logicStatements = flow.logicStatements.put(replacement, { newStatements.toPersistentList() }, { it + newStatements })
            }
            flow.approvedTypeStatements[alias]?.let {
                flow.approvedTypeStatements = flow.approvedTypeStatements.addTypeStatement(it.replaceVariable(alias, replacement))
            }
            flow.approvedTypeStatementsDiff[alias]?.let {
                flow.approvedTypeStatementsDiff = flow.approvedTypeStatementsDiff.addTypeStatement(it.replaceVariable(alias, replacement))
            }
        }

        // Point all other members of the equivalence set to the new representative
        if (original != null) {
            val siblings = flow.backwardsAliasMap.getValue(original).remove(alias)
            // Although this code is written defensively, in practice `backAliases` should be null;
            // any aliases to this variable should have been treated as aliases to `original` instead.
            flow.backwardsAliasMap = if (backAliases != null) {
                flow.directAliasMap += backAliases.map { it to original }
                flow.backwardsAliasMap.put(original, siblings + backAliases)
            } else if (siblings.isNotEmpty()) {
                flow.backwardsAliasMap.put(original, siblings)
            } else {
                flow.backwardsAliasMap.remove(original)
            }
        } else if (replacement != null) {
            flow.directAliasMap -= replacement
            flow.addAliases(backAliases!!, replacement)
        }
    }

    private fun PersistentFlow.addAliases(aliases: PersistentSet<RealVariable>, target: RealVariable) {
        val withoutSelf = aliases - target
        if (withoutSelf.isNotEmpty()) {
            directAliasMap += withoutSelf.map { it to target }
            backwardsAliasMap = backwardsAliasMap.put(target, { withoutSelf }, { it + withoutSelf })
        }
    }

    private fun Implication.replaceVariable(from: RealVariable, to: RealVariable): Implication {
        return Implication(condition.replaceVariable(from, to), effect.replaceVariable(from, to))
    }

    private fun <T : Statement<T>> Statement<T>.replaceVariable(from: RealVariable, to: RealVariable): T {
        val statement = when (this) {
            is OperationStatement -> if (variable == from) copy(variable = to) else this
            is PersistentTypeStatement -> if (variable == from) copy(variable = to) else this
            is MutableTypeStatement -> if (variable == from) MutableTypeStatement(to, exactType, exactNotType) else this
            else -> throw IllegalArgumentException("unknown type of statement $this")
        }
        @Suppress("UNCHECKED_CAST")
        return statement as T
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

    override fun removeTypeStatementsAboutVariable(flow: PersistentFlow, variable: RealVariable) {
        variable.forEachTransitiveDependentVariable {
            flow.approvedTypeStatements -= it
            flow.approvedTypeStatementsDiff -= it
        }
    }

    override fun removeLogicStatementsAboutVariable(flow: PersistentFlow, variable: RealVariable) {
        variable.forEachTransitiveDependentVariable {
            flow.logicStatements -= it
        }
        var newLogicStatements = flow.logicStatements
        for ((key, implications) in flow.logicStatements) {
            val implicationsToDelete = mutableListOf<Implication>()
            variable.forEachTransitiveDependentVariable {
                implications.filterTo(implicationsToDelete) { implication ->
                    implication.effect.variable == it
                }
            }
            if (implicationsToDelete.isEmpty()) continue
            val newImplications = implications.removeAll(implicationsToDelete)
            newLogicStatements = if (newImplications.isNotEmpty()) {
                newLogicStatements.put(key, newImplications)
            } else {
                newLogicStatements.remove(key)
            }
        }
        flow.logicStatements = newLogicStatements
    }

    override fun removeAliasInformationAboutVariable(flow: PersistentFlow, variable: RealVariable) {
        variable.forEachTransitiveDependentVariable {
            removeLocalVariableAlias(flow, it)
        }
    }

    private fun RealVariable.forEachTransitiveDependentVariable(action: (RealVariable) -> Unit) {
        action(this)
        dependentVariables.forEach {
            it.forEachTransitiveDependentVariable(action)
        }
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

private fun PersistentApprovedTypeStatements.addTypeStatement(info: TypeStatement): PersistentApprovedTypeStatements {
    val variable = info.variable
    val existingInfo = this[variable]
    return put(variable, if (existingInfo != null) existingInfo + info else info.toPersistent())
}

private fun TypeStatement.toPersistent(): PersistentTypeStatement = when (this) {
    is PersistentTypeStatement -> this
    else -> PersistentTypeStatement(variable, exactType.toPersistentSet(), exactNotType.toPersistentSet())
}

fun TypeStatement.asMutableStatement(): MutableTypeStatement = when (this) {
    is MutableTypeStatement -> this
    else -> MutableTypeStatement(variable, exactType.toMutableSet(), exactNotType.toMutableSet())
}

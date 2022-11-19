/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

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
    var approvedTypeStatements: PersistentApprovedTypeStatements
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

    override val knownVariables: Set<RealVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: RealVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)
}

abstract class PersistentLogicSystem(context: ConeInferenceContext) : LogicSystem<PersistentFlow>(context) {
    abstract val variableStorage: VariableStorageImpl

    override fun createEmptyFlow(): PersistentFlow =
        PersistentFlow()

    override fun forkFlow(flow: PersistentFlow): PersistentFlow =
        PersistentFlow(flow)

    override fun joinFlow(flows: Collection<PersistentFlow>): PersistentFlow =
        foldFlow(flows, allExecute = false)

    override fun unionFlow(flows: Collection<PersistentFlow>): PersistentFlow =
        foldFlow(flows, allExecute = true)

    private fun foldFlow(flows: Collection<PersistentFlow>, allExecute: Boolean): PersistentFlow {
        when (flows.size) {
            0 -> return createEmptyFlow()
            1 -> return forkFlow(flows.first())
        }

        val commonFlow = flows.reduce(::lowestCommonFlow)
        val result = forkFlow(commonFlow)

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
            recordNewAssignment(result, variable, index)
        }

        // TODO: if `allExecute`, then all aliases from all flows are valid so long as other flows don't have a contradicting one
        for ((from, to) in flows.first().directAliasMap) {
            // If `from -> to` is still in `result` (was not removed by the above code), then it is also in all `flows`,
            // as the only way to break aliasing is by reassignment.
            if (result.directAliasMap[from] != to && flows.all { it.directAliasMap[from] == to }) {
                // if (p) { y = x } else { y = x } <-- after `if`, `y -> x` is in all `flows`, but not in `result`
                // (which was forked from the flow before the `if`)
                addLocalVariableAlias(result, from, to)
            }
        }

        val approvedTypeStatements = result.approvedTypeStatements.builder()
        flows.flatMapTo(mutableSetOf()) { it.knownVariables }.forEach computeStatement@{ variable ->
            val statement = if (variable in result.directAliasMap) {
                return@computeStatement // statements about alias == statements about aliased variable
            } else if (!allExecute) {
                // if (condition) { /* x: S1 */ } else { /* x: S2 */ } // -> x: S1 | S2
                or(flows.mapTo(mutableSetOf()) { it.getTypeStatement(variable) ?: return@computeStatement })
            } else if (variable !in reassignedVariables) {
                // callAllInSomeOrder({ /* x: S1 */ }, { /* x: S2 */ }) // -> x: S1 & S2
                and(flows.mapNotNullTo(mutableSetOf()) { it.getTypeStatement(variable) })
            } else {
                // callAllInSomeOrder({ x = ...; /* x: S1 */  }, { x = ...; /* x: S2 */ }, { /* x: S3 */ }) // -> x: S1 | S2
                val byAssignment =
                    flows.groupByTo(mutableMapOf(), { it.assignmentIndex[variable] ?: -1 }, { it.getTypeStatement(variable) })
                // This throws out S3 from the above example, as the flow that makes that statement is unordered w.r.t. the other two:
                byAssignment.remove(commonFlow.assignmentIndex[variable] ?: -1)
                // In the above example each list in `byAssignment.values` only has one entry, but in general we can have something like
                //   A -> B (assigns) -> C (does not assign)
                //    \             \--> D (does not assign)
                //     \-> E (assigns)
                // in which case for {C, D, E} the result is (C && D) || E. Not sure that kind of control flow can exist
                // without intermediate nodes being created for (C && D) though.
                or(byAssignment.values.mapTo(mutableSetOf()) { and(it.filterNotNull()) ?: return@computeStatement })
            }
            if (statement?.isNotEmpty == true) {
                approvedTypeStatements[variable] = statement.toPersistent()
            }
        }
        result.approvedTypeStatements = approvedTypeStatements.build()
        // TODO: compute common implications?
        return result
    }

    override fun addLocalVariableAlias(flow: PersistentFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        flow.addAliases(persistentSetOf(alias), flow.unwrapVariable(underlyingVariable))
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
            directAliasMap = withoutSelf.associateWithTo(directAliasMap.builder()) { target }.build()
            backwardsAliasMap = backwardsAliasMap.put(target, { withoutSelf }, { it + withoutSelf })
        }
    }

    private fun PersistentFlow.completeStatement(statement: TypeStatement): PersistentTypeStatement? {
        if (statement.exactType.isEmpty()) return null
        val variable = statement.variable
        val oldExactType = approvedTypeStatements[variable]?.exactType
        val newExactType = oldExactType?.addAll(statement.exactType) ?: statement.exactType.toPersistentSet()
        if (newExactType === oldExactType) return null
        return PersistentTypeStatement(variable, newExactType)
    }

    override fun addTypeStatement(flow: PersistentFlow, statement: TypeStatement): TypeStatement? =
        flow.completeStatement(statement)?.also {
            flow.approvedTypeStatements = flow.approvedTypeStatements.put(it.variable, it)
        }

    override fun addTypeStatements(flow: PersistentFlow, statements: TypeStatements): List<TypeStatement> {
        val builder = flow.approvedTypeStatements.builder()
        val result = statements.values.mapNotNull { statement ->
            flow.completeStatement(statement)?.also { builder[it.variable] = it }
        }
        flow.approvedTypeStatements = builder.build()
        return result
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
        transform: (Implication) -> Implication?
    ) {
        val statements = flow.logicStatements[originalVariable]?.takeIf { it.isNotEmpty() } ?: return
        val existing = flow.logicStatements[newVariable] ?: persistentListOf()
        val result = statements.mapNotNullTo(existing.builder()) {
            // TODO: rethink this API - technically it permits constructing invalid flows
            //  (transform can replace the variable in the condition with anything)
            transform(OperationStatement(newVariable, it.condition.operation) implies it.effect)
        }.build()
        if (shouldRemoveOriginalStatements) {
            flow.logicStatements -= originalVariable
        }
        flow.logicStatements = flow.logicStatements.put(newVariable, result)
    }

    private val nullableNothingType = context.session.builtinTypes.nullableNothingType.type
    private val anyType = context.session.builtinTypes.anyType.type

    override fun approveOperationStatement(
        flow: PersistentFlow,
        approvedStatement: OperationStatement,
        removeApprovedOrImpossible: Boolean,
    ): TypeStatements {
        val result = mutableMapOf<RealVariable, MutableTypeStatement>()
        val queue = LinkedList<OperationStatement>().apply { this += approvedStatement }
        val approved = mutableSetOf<OperationStatement>()
        val logicStatements = flow.logicStatements.builder()
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            // Defense from cycles in facts
            if (!removeApprovedOrImpossible && !approved.add(next)) continue

            val operation = next.operation
            val variable = next.variable
            if (variable.isReal()) {
                val impliedType = if (operation == Operation.EqNull) nullableNothingType else anyType
                result.getOrPut(variable) { MutableTypeStatement(variable) }.exactType.add(impliedType)
            }

            val statements = logicStatements[variable] ?: continue
            val stillUnknown = statements.removeAll {
                val knownValue = it.condition.operation.valueIfKnown(operation)
                if (knownValue == true) {
                    when (val effect = it.effect) {
                        is OperationStatement -> queue += effect
                        is TypeStatement ->
                            result.getOrPut(effect.variable) { MutableTypeStatement(effect.variable) } += effect
                    }
                }
                removeApprovedOrImpossible && knownValue != null
            }
            if (stillUnknown.isEmpty()) {
                logicStatements.remove(variable)
            } else if (stillUnknown != statements) {
                logicStatements[variable] = stillUnknown
            }
        }
        flow.logicStatements = logicStatements.build()
        return result
    }

    override fun recordNewAssignment(flow: PersistentFlow, variable: RealVariable, index: Int) {
        flow.replaceVariable(variable, null)
        flow.assignmentIndex = flow.assignmentIndex.put(variable, index)
    }

    override fun isSameValueIn(a: PersistentFlow, b: PersistentFlow, variable: RealVariable): Boolean =
        a.assignmentIndex[variable] == b.assignmentIndex[variable]
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

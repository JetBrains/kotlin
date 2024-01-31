/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.types.AbstractTypeChecker
import java.util.*
import kotlin.math.max

abstract class LogicSystem(private val context: ConeInferenceContext) {
    val session: FirSession get() = context.session
    private val nullableNothingType = session.builtinTypes.nullableNothingType.type
    private val anyType = session.builtinTypes.anyType.type

    abstract val variableStorage: VariableStorageImpl

    protected open fun ConeKotlinType.isAcceptableForSmartcast(): Boolean {
        return !isNullableNothing
    }

    /**
     * Creates the next [Flow] by joining a set of previous [Flow]s.
     *
     * @param flows All [PersistentFlow]s which flow into the join flow. These will determine assignments and variable aliases for the
     * resulting join flow.
     * @param statementFlows A *subset* of [flows] used to determine what [TypeStatement]s and [Implication]s will be copied to the joined
     * flow.
     * @param union Determines if [TypeStatement]s from different flows should be combined with union or intersection logic.
     */
    fun joinFlow(flows: Collection<PersistentFlow>, statementFlows: Collection<PersistentFlow>, union: Boolean): MutableFlow {
        when (flows.size) {
            0 -> return MutableFlow()
            1 -> return flows.first().fork()
        }
        // If you're debugging this assertion error, most likely cause is that a node is not
        // marked as dead when all its input edges are dead. In that case it will have an empty flow,
        // and joining that with a non-empty flow from another branch will fail.
        val commonFlow = flows.reduce { a, b -> a.lowestCommonAncestor(b) ?: error("no common ancestor in $a, $b") }
        val result = commonFlow.fork()
        result.mergeAssignments(flows)
        if (union) {
            result.copyNonConflictingAliases(flows, commonFlow)
        } else {
            result.copyCommonAliases(flows)
        }
        result.copyStatements(statementFlows, commonFlow, union)
        result.copyImplications(statementFlows)
        return result
    }

    fun addLocalVariableAlias(flow: MutableFlow, alias: RealVariable, underlyingVariable: RealVariable) {
        if (underlyingVariable == alias) return // x = x
        flow.directAliasMap[alias] = underlyingVariable
        flow.backwardsAliasMap[underlyingVariable] = flow.backwardsAliasMap[underlyingVariable]?.add(alias) ?: persistentSetOf(alias)
    }

    fun addTypeStatement(flow: MutableFlow, statement: TypeStatement): TypeStatement? {
        if (statement.exactType.isEmpty()) return null
        val variable = statement.variable
        val oldExactType = flow.approvedTypeStatements[variable]?.exactType
        val newExactType = oldExactType?.addAll(statement.exactType) ?: statement.exactType.toPersistentSet()
        if (newExactType === oldExactType) return null
        return PersistentTypeStatement(variable, newExactType).also { flow.approvedTypeStatements[variable] = it }
    }

    fun addTypeStatements(flow: MutableFlow, statements: TypeStatements): List<TypeStatement> =
        statements.values.mapNotNull { addTypeStatement(flow, it) }

    fun addImplication(flow: MutableFlow, implication: Implication) {
        val effect = implication.effect
        if (effect == implication.condition) return
        if (effect is TypeStatement &&
            (effect.isEmpty || flow.approvedTypeStatements[effect.variable]?.exactType?.containsAll(effect.exactType) == true)
        ) return
        val variable = implication.condition.variable
        flow.implications[variable] = flow.implications[variable]?.add(implication) ?: persistentListOf(implication)
    }

    fun translateVariableFromConditionInStatements(
        flow: MutableFlow,
        originalVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        transform: (Implication) -> Implication? = { it }
    ) {
        val statements = if (originalVariable.isSynthetic())
            flow.implications.remove(originalVariable)
        else
            flow.implications[originalVariable]
        if (statements.isNullOrEmpty()) return
        val existing = flow.implications[newVariable] ?: persistentListOf()
        flow.implications[newVariable] = statements.mapNotNullTo(existing.builder()) {
            // TODO: rethink this API - technically it permits constructing invalid flows
            //  (transform can replace the variable in the condition with anything)
            transform(OperationStatement(newVariable, it.condition.operation) implies it.effect)
        }.build()
    }

    fun approveOperationStatement(flow: PersistentFlow, statement: OperationStatement): TypeStatements {
        return approveOperationStatement(flow.implications.toMutableMap(), statement, removeApprovedOrImpossible = false)
    }

    fun approveOperationStatement(
        flow: MutableFlow,
        statement: OperationStatement,
        removeApprovedOrImpossible: Boolean,
    ): TypeStatements {
        return approveOperationStatement(flow.implications, statement, removeApprovedOrImpossible)
    }

    fun recordNewAssignment(flow: MutableFlow, variable: RealVariable, index: Int) {
        flow.replaceVariable(variable, null)
        flow.assignmentIndex[variable] = index
    }

    fun isSameValueIn(a: PersistentFlow, b: PersistentFlow, variable: RealVariable): Boolean =
        a.assignmentIndex[variable] == b.assignmentIndex[variable]

    fun isSameValueIn(a: PersistentFlow, b: MutableFlow, variable: RealVariable): Boolean {
        return a.assignmentIndex[variable] == b.assignmentIndex[variable]
    }

    private fun MutableFlow.mergeAssignments(flows: Collection<PersistentFlow>) {
        // If a variable was reassigned in one branch, it was reassigned at the join point.
        val reassignedVariables = mutableMapOf<RealVariable, Int>()
        for (flow in flows) {
            for ((variable, index) in flow.assignmentIndex) {
                if (assignmentIndex[variable] != index) {
                    // Ideally we should generate an entirely new index here, but it doesn't really
                    // matter; the important part is that it's different from `commonFlow.previousFlow`.
                    reassignedVariables[variable] = max(index, reassignedVariables[variable] ?: 0)
                }
            }
        }
        for ((variable, index) in reassignedVariables) {
            recordNewAssignment(this, variable, index)
        }
    }

    private fun MutableFlow.copyCommonAliases(flows: Collection<PersistentFlow>) {
        for ((from, to) in flows.first().directAliasMap) {
            // If `from -> to` is still in `this` (was not removed by the above code), then it is also in all `flows`,
            // as the only way to break aliasing is by reassignment.
            if (directAliasMap[from] != to && flows.all { it.unwrapVariable(from) == to }) {
                // if (p) { y = x } else { y = x } <-- after `if`, `y -> x` is in all `flows`, but not in `result`
                // (which was forked from the flow before the `if`)
                addLocalVariableAlias(this, from, to)
            }
        }
    }

    private fun MutableFlow.copyNonConflictingAliases(flows: Collection<PersistentFlow>, commonFlow: PersistentFlow) {
        val candidates = mutableMapOf<RealVariable, RealVariable?>()
        for (flow in flows) {
            for ((from, to) in flow.directAliasMap) {
                candidates[from] = when {
                    // f({ a = b }, { notReassigning() }) -> a = b
                    commonFlow.assignmentIndex[from] == flow.assignmentIndex[from] -> continue
                    // f({ a = b }, { a = c }) -> a = b or c; can't express that, so just don't
                    from in candidates && candidates[from] != to -> null
                    // f({ a = b }, { a = b }) -> a = b
                    else -> to
                }
            }
        }
        for ((from, to) in candidates) {
            addLocalVariableAlias(this, from, to ?: continue)
        }
    }

    private fun MutableFlow.copyStatements(flows: Collection<PersistentFlow>, commonFlow: PersistentFlow, union: Boolean) {
        flows.flatMapTo(mutableSetOf()) { it.knownVariables }.forEach computeStatement@{ variable ->
            val statement = if (variable in directAliasMap) {
                return@computeStatement // statements about alias == statements about aliased variable
            } else if (!union) {
                // if (condition) { /* x: S1 */ } else { /* x: S2 */ } // -> x: S1 | S2
                or(flows.mapTo(mutableSetOf()) { it.getTypeStatement(variable) ?: return@computeStatement })
            } else if (assignmentIndex[variable] == commonFlow.assignmentIndex[variable]) {
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
    }

    private fun MutableFlow.copyImplications(flows: Collection<PersistentFlow>) {
        when (flows.size) {
            0 -> {}
            1 -> implications += flows.first().implications
            else -> {} // TODO, KT-65293: compute common implications?
        }
    }

    private fun MutableFlow.replaceVariable(variable: RealVariable, replacement: RealVariable?) {
        val original = directAliasMap.remove(variable)
        if (original != null) {
            // All statements should've been made about whatever variable this is an alias to. There is nothing to replace.
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                assert(variable !in backwardsAliasMap)
                assert(variable !in implications)
                assert(variable !in approvedTypeStatements)
            }
            // `variable.dependentVariables` is not separated by flow, so it may be non-empty if aliasing of this variable
            // was broken in another flow. However, in *this* flow dependent variables should have no information attached to them.
            val siblings = backwardsAliasMap.getValue(original)
            if (siblings.size > 1) {
                backwardsAliasMap[original] = siblings.remove(variable)
            } else {
                backwardsAliasMap.remove(original)
            }
            if (replacement != null) {
                addLocalVariableAlias(this, replacement, original)
            }
        } else {
            val aliases = backwardsAliasMap.remove(variable)
            // If asked to remove the variable but there are aliases, replace with a new representative for the alias group instead.
            val replacementOrNext = replacement ?: aliases?.first()
            for (dependent in variable.dependentVariables) {
                replaceVariable(dependent, replacementOrNext?.let {
                    variableStorage.copyRealVariableWithRemapping(dependent, variable, it)
                })
            }
            implications.replaceVariable(variable, replacementOrNext)
            approvedTypeStatements.replaceVariable(variable, replacementOrNext)
            if (aliases != null && replacementOrNext != null) {
                directAliasMap -= replacementOrNext
                val withoutSelf = aliases - replacementOrNext
                if (withoutSelf.isNotEmpty()) {
                    withoutSelf.associateWithTo(directAliasMap) { replacementOrNext }
                    backwardsAliasMap[replacementOrNext] = backwardsAliasMap[replacementOrNext]?.addAll(withoutSelf) ?: withoutSelf
                }
            }
        }
    }

    private fun approveOperationStatement(
        logicStatements: Map<DataFlowVariable, PersistentList<Implication>>,
        approvedStatement: OperationStatement,
        removeApprovedOrImpossible: Boolean
    ): TypeStatements {
        val result = mutableMapOf<RealVariable, MutableTypeStatement>()
        val queue = LinkedList<OperationStatement>().apply { this += approvedStatement }
        val approved = mutableSetOf<OperationStatement>()
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
            if (stillUnknown != statements && logicStatements is MutableMap) {
                if (stillUnknown.isEmpty()) {
                    logicStatements.remove(variable)
                } else {
                    logicStatements[variable] = stillUnknown
                }
            }
        }
        return result
    }

    // ------------------------------- Public TypeStatement util functions -------------------------------

    fun orForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> left
        right.isEmpty() -> right
        else -> buildMap {
            for ((variable, leftStatement) in left) {
                put(variable, or(listOf(leftStatement, right[variable] ?: continue)) ?: continue)
            }
        }
    }

    fun andForTypeStatements(left: TypeStatements, right: TypeStatements): TypeStatements = when {
        left.isEmpty() -> right
        right.isEmpty() -> left
        else -> left.toMutableMap().apply {
            for ((variable, rightStatement) in right) {
                this[variable] = and(this[variable], rightStatement)
            }
        }
    }

    private operator fun MutableTypeStatement.plusAssign(other: TypeStatement) {
        exactType += other.exactType
    }

    fun and(a: TypeStatement?, b: TypeStatement): TypeStatement {
        return a?.toMutable()?.apply { this += b } ?: b
    }

    fun and(statements: Collection<TypeStatement>): TypeStatement? {
        when (statements.size) {
            0 -> return null
            1 -> return statements.first()
        }
        val iterator = statements.iterator()
        val result = iterator.next().toMutable()
        while (iterator.hasNext()) {
            result += iterator.next()
        }
        return result
    }

    fun or(statements: Collection<TypeStatement>): TypeStatement? {
        when (statements.size) {
            0 -> return null
            1 -> return statements.first()
        }
        val variable = statements.first().variable
        assert(statements.all { it.variable == variable }) { "folding statements for different variables" }
        if (statements.any { it.isEmpty }) return null
        val intersected = statements.map { ConeTypeIntersector.intersectTypes(context, it.exactType.toList()) }
        val unified = context.commonSuperTypeOrNull(intersected) ?: return null
        val result = when {
            unified.isNullableAny -> return null
            unified.isAcceptableForSmartcast() -> unified
            unified.canBeNull(context.session) -> return null
            else -> context.anyType()
        }
        return PersistentTypeStatement(variable, persistentSetOf(result))
    }
}

private fun TypeStatement.toPersistent(): PersistentTypeStatement = when (this) {
    is PersistentTypeStatement -> this
    // If this statement was obtained via `toMutable`, `toPersistentSet` will call `build`.
    else -> PersistentTypeStatement(variable, exactType.toPersistentSet())
}

private fun TypeStatement.toMutable(): MutableTypeStatement = when (this) {
    is PersistentTypeStatement -> MutableTypeStatement(variable, exactType.builder())
    else -> MutableTypeStatement(variable, LinkedHashSet(exactType))
}

@JvmName("replaceVariableInStatements")
private fun MutableMap<RealVariable, PersistentTypeStatement>.replaceVariable(from: RealVariable, to: RealVariable?) {
    val existing = remove(from) ?: return
    if (to != null) {
        put(to, existing.copy(variable = to))
    }
}

@JvmName("replaceVariableInImplications")
private fun MutableMap<DataFlowVariable, PersistentList<Implication>>.replaceVariable(from: RealVariable, to: RealVariable?) {
    val existing = remove(from)
    val toReplace = entries.mapNotNull { (variable, implications) ->
        val newImplications = if (to != null) {
            implications.replaceAll { it.replaceVariable(from, to) }
        } else {
            implications.removeAll { it.effect.variable == from }
        }
        if (newImplications != implications) variable to newImplications else null
    }
    for ((variable, implications) in toReplace) {
        if (implications.isEmpty()) {
            remove(variable)
        } else {
            put(variable, implications)
        }
    }
    if (existing != null && to != null) {
        put(to, existing.replaceAll { it.replaceVariable(from, to) })
    }
}

private inline fun <T> PersistentList<T>.replaceAll(block: (T) -> T): PersistentList<T> {
    return mutate { result ->
        val it = result.listIterator()
        while (it.hasNext()) {
            it.set(block(it.next()))
        }
    }
}

private fun Implication.replaceVariable(from: RealVariable, to: RealVariable): Implication = when {
    condition.variable == from -> Implication(condition.copy(variable = to), effect.replaceVariable(from, to))
    effect.variable == from -> Implication(condition, effect.replaceVariable(from, to))
    else -> this
}

private fun Statement.replaceVariable(from: RealVariable, to: RealVariable): Statement {
    if (variable != from) return this
    return when (this) {
        is OperationStatement -> copy(variable = to)
        is PersistentTypeStatement -> copy(variable = to)
        is MutableTypeStatement -> MutableTypeStatement(to, exactType)
    }
}

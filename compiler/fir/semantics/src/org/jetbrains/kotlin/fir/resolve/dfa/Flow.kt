/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.FirExpression

abstract class Flow {
    internal abstract operator fun get(variable: RealVariable): FlowVariable?
    internal fun getAlias(variable: RealVariable) = get(variable) as? FlowVariable.Alias
    internal fun getReal(variable: RealVariable) = get(variable) as? FlowVariable.Real

    abstract val knownVariables: Set<RealVariable>

    fun unwrapVariable(variable: RealVariable): RealVariable =
        getAlias(variable)?.underlyingVariable ?: variable

    fun getTypeStatement(variable: RealVariable): TypeStatement? =
        getReal(unwrapVariable(variable))?.approvedTypeStatement?.copy(variable = variable)

    abstract fun getImplications(variable: DataFlowVariable): Collection<Implication>?

    abstract fun getKnown(variable: RealVariable): RealVariable?

    /**
     * Retrieve a [DataFlowVariable] representing the given [FirExpression]. If [fir] is a property reference,
     * return a [RealVariable], otherwise a [SyntheticVariable].
     *
     * @param unwrapAlias When multiple [RealVariable]s are known to have the same value in every execution, this lambda
     * can map them to some "representative" variable so that type information about all these variables is shared. It can also
     * return null to abort the entire [get] operation, making it also return null.
     *
     * @param unwrapAliasInReceivers Same as [unwrapAlias], but used when looking up [RealVariable]s for receivers of [fir]
     * if it is a qualified access expression.
     */
    fun get(
        fir: FirExpression,
        session: FirSession,
        unwrapAlias: (RealVariable) -> RealVariable? = this::unwrapVariable,
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = unwrapAlias,
    ): DataFlowVariable? {
        val prototype = DataFlowVariable.of(fir, session, unwrapAliasInReceivers)
        if (prototype !is RealVariable) return prototype
        val real = getKnown(prototype) ?: return null
        return unwrapAlias(real)
    }
}

internal sealed class FlowVariable {
    abstract val variable: RealVariable
    abstract val assignmentIndex: Int?
    // TODO implications?

    abstract fun copy(assignmentIndex: Int? = this.assignmentIndex): FlowVariable

    data class Alias(
        override val variable: RealVariable,
        override val assignmentIndex: Int? = null,
        // RealVariables thus form equivalence sets by values they reference. One is chosen
        // as a representative of that set, while the rest are mapped to that representative
        // in `directAliasMap`. `backwardsAliasMap` maps each representative to the rest of the set.
        val underlyingVariable: RealVariable,
    ) : FlowVariable() {
        override fun copy(assignmentIndex: Int?): FlowVariable {
            return copy(
                variable = variable,
                assignmentIndex = assignmentIndex,
                underlyingVariable = underlyingVariable,
            )
        }
    }

    data class Real(
        override val variable: RealVariable,
        override val assignmentIndex: Int? = null,
        val members: PersistentSet<RealVariable> = persistentSetOf(),
        val approvedTypeStatement: PersistentTypeStatement? = null,
        // RealVariable describes a storage in memory; a pair of RealVariable with its assignment
        // index at a particular execution point forms an SSA value corresponding to the result of
        // an initializer.
        val aliases: PersistentSet<RealVariable> = persistentSetOf(),
    ) : FlowVariable() {
        override fun copy(assignmentIndex: Int?): FlowVariable {
            return copy(
                variable = variable,
                assignmentIndex = assignmentIndex,
                members = members,
                approvedTypeStatement = approvedTypeStatement,
                aliases = aliases,
            )
        }
    }
}

class PersistentFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    internal val variables: PersistentMap<RealVariable, FlowVariable>,
    internal val implications: PersistentMap<DataFlowVariable, PersistentList<Implication>>,
) : Flow() {
    private val level: Int = if (previousFlow != null) previousFlow.level + 1 else 0

    override val knownVariables: Set<RealVariable>
        get() = variables.keys

    val allVariablesForDebug: Set<DataFlowVariable>
        get() = variables.keys +
                implications.keys +
                implications.values.flatten().map { it.effect.variable }

    override operator fun get(variable: RealVariable) = variables[variable]

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    fun lowestCommonAncestor(other: PersistentFlow): PersistentFlow? {
        var left = this
        var right = other
        while (left.level > right.level) {
            left = left.previousFlow ?: return null
        }
        while (right.level > left.level) {
            right = right.previousFlow ?: return null
        }
        while (left != right) {
            left = left.previousFlow ?: return null
            right = right.previousFlow ?: return null
        }
        return left
    }

    fun fork(): MutableFlow = MutableFlow(
        this,
        variables.builder(),
        implications.builder(),
    )

    override fun getKnown(variable: RealVariable): RealVariable? =
        variables[variable]?.variable
}

class MutableFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    internal val variables: PersistentMap.Builder<RealVariable, FlowVariable>,
    internal val implications: PersistentMap.Builder<DataFlowVariable, PersistentList<Implication>>,
) : Flow() {
    constructor() : this(
        null,
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
    )

    override operator fun get(variable: RealVariable) = variables[variable]

    override val knownVariables: Set<RealVariable>
        get() = variables.keys

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow,
        variables.build(),
        implications.build(),
    )

    /**
     * Retrieve a [DataFlowVariable] representing the given [FirExpression]. If [fir] is a property reference,
     * return a [RealVariable], otherwise a [SyntheticVariable].
     *
     * @param unwrapAlias When multiple [RealVariable]s are known to have the same value in every execution, this lambda
     * can map them to some "representative" variable so that type information about all these variables is shared. It can also
     * return null to abort the entire [get] operation, making it also return null.
     *
     * @param unwrapAliasInReceivers Same as [unwrapAlias], but used when looking up [RealVariable]s for receivers of [fir]
     * if it is a qualified access expression.
     */
    fun create(
        fir: FirExpression,
        session: FirSession,
        unwrapAlias: (RealVariable) -> RealVariable? = this::unwrapVariable,
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = unwrapAlias,
    ): DataFlowVariable? {
        val prototype = DataFlowVariable.of(fir, session, unwrapAliasInReceivers)
        if (prototype !is RealVariable) return prototype
        val real = rememberWithKnownReceivers(prototype)
        return unwrapAlias(real)
    }

    override fun getKnown(variable: RealVariable): RealVariable? =
        variables[variable]?.variable

    /** Store a reference to a variable so that [get] can return it even if `createReal` is false. */
    fun remember(variable: RealVariable): RealVariable =
        rememberWithKnownReceivers(variable.mapReceivers(::remember))

    private fun rememberWithKnownReceivers(variable: RealVariable): RealVariable {
        return variables.getOrPut(variable) {
            variable.dispatchReceiver?.let { addMember(it, variable) }
            variable.extensionReceiver?.let { addMember(it, variable) }
            FlowVariable.Real(variable)
        }.variable
    }

    private fun addMember(variable: RealVariable, member: RealVariable) {
        variables.updateReal(variable) { it.copy(members = it.members.add(member)) }
    }

    private inline fun RealVariable.mapReceivers(block: (RealVariable) -> RealVariable): RealVariable =
        RealVariable(symbol, isReceiver, dispatchReceiver?.let(block), extensionReceiver?.let(block), originalType)

    /**
     * Call a lambda with every known [RealVariable] that represents a member property of another [RealVariable].
     *
     * @param to If not null, additionally replace these variables' receivers with the new value, and pass the modified member
     * to the lambda as the second argument.
     *
     * For example, if [from] represents `x`, [to] represents `y`, and there is a known [RealVariable] representing `x.p`, then
     * [processMember] will be called that variable as the first argument and a new variable representing `y.p` as the second.
     */
    fun replaceReceiverReferencesInMembers(from: RealVariable, to: RealVariable?, processMember: (RealVariable, RealVariable?) -> Unit) {
        for (member in getReal(from)?.members ?: return) {
            val remapped = to?.let { rememberWithKnownReceivers(member.mapReceivers { if (it == from) to else it }) }
            processMember(member, remapped)
        }
    }
}

private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()

internal inline fun MutableMap<RealVariable, FlowVariable>.update(
    key: RealVariable,
    transform: (FlowVariable) -> FlowVariable,
) {
    val variable = get(key) ?: return
    this[key] = transform(variable)
}

internal inline fun MutableMap<RealVariable, FlowVariable>.updateReal(
    key: RealVariable,
    transform: (FlowVariable.Real) -> FlowVariable.Real,
) {
    val variable = getOrDefault(key, FlowVariable.Real(key))
    if (variable !is FlowVariable.Real) return
    this[key] = transform(variable)
}

internal inline fun MutableMap<RealVariable, FlowVariable>.updateAlias(
    key: RealVariable,
    transform: (FlowVariable.Alias) -> FlowVariable.Alias,
) {
    val variable = get(key)
    if (variable !is FlowVariable.Alias) return
    this[key] = transform(variable)
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.*

abstract class Flow {
    abstract val knownVariables: Set<RealVariable>
    abstract fun unwrapVariable(variable: RealVariable): RealVariable
    abstract fun getTypeStatement(variable: RealVariable): TypeStatement?
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

class PersistentFlow internal constructor(
    private val previousFlow: PersistentFlow?,

    // This is basically a set, since it maps each key to itself. The only point of having it as a map
    // is to deduplicate equal instances with lookups. The impact of that is questionable, but whatever.
    internal val realVariables: PersistentMap<RealVariable, RealVariable>,
    private val memberVariables: PersistentMap<RealVariable, PersistentSet<RealVariable>>,

    private val approvedTypeStatements: PersistentMap<RealVariable, PersistentTypeStatement>,
    internal val implications: PersistentMap<DataFlowVariable, PersistentList<Implication>>,
    // RealVariable describes a storage in memory; a pair of RealVariable with its assignment
    // index at a particular execution point forms an SSA value corresponding to the result of
    // an initializer.
    internal val assignmentIndex: PersistentMap<RealVariable, Int>,
    // RealVariables thus form equivalence sets by values they reference. One is chosen
    // as a representative of that set, while the rest are mapped to that representative
    // in `directAliasMap`. `backwardsAliasMap` maps each representative to the rest of the set.
    internal val directAliasMap: PersistentMap<RealVariable, RealVariable>,
    private val backwardsAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
) : Flow() {
    private val level: Int = if (previousFlow != null) previousFlow.level + 1 else 0

    override val knownVariables: Set<RealVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    val allVariablesForDebug: Set<DataFlowVariable>
        get() = realVariables.keys +
                approvedTypeStatements.keys +
                directAliasMap.keys +
                implications.keys +
                implications.values.flatten().map { it.effect.variable }

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: RealVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

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
        realVariables.builder(),
        memberVariables.builder(),
        approvedTypeStatements.builder(),
        implications.builder(),
        assignmentIndex.builder(),
        directAliasMap.builder(),
        backwardsAliasMap.builder(),
    )

    override fun getKnown(variable: RealVariable): RealVariable? =
        realVariables[variable]
}

class MutableFlow internal constructor(
    private val previousFlow: PersistentFlow?,

    // This is basically a set, since it maps each key to itself. The only point of having it as a map
    // is to deduplicate equal instances with lookups. The impact of that is questionable, but whatever.
    private val realVariables: PersistentMap.Builder<RealVariable, RealVariable>,
    private val memberVariables: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,

    internal val approvedTypeStatements: PersistentMap.Builder<RealVariable, PersistentTypeStatement>,
    internal val implications: PersistentMap.Builder<DataFlowVariable, PersistentList<Implication>>,
    internal val assignmentIndex: PersistentMap.Builder<RealVariable, Int>,
    internal val directAliasMap: PersistentMap.Builder<RealVariable, RealVariable>,
    internal val backwardsAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
) : Flow() {
    constructor() : this(
        null,
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
    )

    override val knownVariables: Set<RealVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: RealVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow,
        realVariables.build(),
        memberVariables.build(),
        approvedTypeStatements.build(),
        implications.build(),
        assignmentIndex.build(),
        directAliasMap.build(),
        backwardsAliasMap.build(),
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
        realVariables[variable]

    /** Store a reference to a variable so that [get] can return it even if `createReal` is false. */
    fun remember(variable: RealVariable): RealVariable =
        rememberWithKnownReceivers(variable.mapReceivers(::remember))

    private fun rememberWithKnownReceivers(variable: RealVariable): RealVariable {
        return realVariables.getOrPut(variable) {
            variable.dispatchReceiver?.let { addMember(it, variable) }
            variable.extensionReceiver?.let { addMember(it, variable) }
            variable
        }
    }

    private fun addMember(variable: RealVariable, member: RealVariable) {
        val members = memberVariables[variable] ?: persistentSetOf()
        memberVariables[variable] = members.add(member)
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
        for (member in memberVariables[from] ?: return) {
            val remapped = to?.let { rememberWithKnownReceivers(member.mapReceivers { if (it == from) to else it }) }
            processMember(member, remapped)
        }
    }
}

private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()

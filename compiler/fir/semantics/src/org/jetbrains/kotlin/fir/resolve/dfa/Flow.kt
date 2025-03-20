/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf

abstract class Flow {
    abstract val knownVariables: Set<DataFlowVariable>
    abstract fun unwrapVariable(variable: RealVariable): RealVariable
    abstract fun getTypeStatement(variable: DataFlowVariable): TypeStatement?
    abstract fun getImplications(variable: DataFlowVariable): Collection<Implication>?

    open fun unwrapVariable(variable: DataFlowVariable): DataFlowVariable =
        if (variable is RealVariable) unwrapVariable(variable) else variable
}

class PersistentFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    private val approvedTypeStatements: PersistentMap<DataFlowVariable, PersistentTypeStatement>,
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

    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    val allVariablesForDebug: Set<DataFlowVariable>
        get() = knownVariables + implications.keys + implications.values.flatten().map { it.effect.variable }

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
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
        approvedTypeStatements.builder(),
        implications.builder(),
        assignmentIndex.builder(),
        directAliasMap.builder(),
        backwardsAliasMap.builder(),
    )
}

class MutableFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    internal val approvedTypeStatements: PersistentMap.Builder<DataFlowVariable, PersistentTypeStatement>,
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
    )

    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow,
        approvedTypeStatements.build(),
        implications.build(),
        assignmentIndex.build(),
        directAliasMap.build(),
        backwardsAliasMap.build(),
    )
}

private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()

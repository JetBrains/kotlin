/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.*
import org.jetbrains.kotlin.fir.DfaType
import org.jetbrains.kotlin.fir.types.ConeKotlinType

sealed class Flow(protected val previousFlow: PersistentFlow?) {
    internal abstract val approvedTypeStatements: Map<DataFlowVariable, PersistentTypeStatement>
    internal abstract val implications: Map<DataFlowVariable, PersistentList<Implication>>

    /**
     * RealVariable describes a storage in memory; a pair of RealVariable with its assignment
     * index at a particular execution point forms an SSA value corresponding to the result of
     * an initializer.
     */
    internal abstract val assignmentIndex: Map<RealVariable, Int>

    /**
     * RealVariables thus form equivalence sets by values they reference. One is chosen
     * as a representative of that set, while the rest are mapped to that representative
     * in `directAliasMap`. `backwardsAliasMap` maps each representative to the rest of the set.
     */
    internal abstract val directAliasMap: Map<RealVariable, RealVariable>
    internal abstract val backwardsAliasMap: Map<RealVariable, PersistentSet<RealVariable>>
    internal abstract val oneWayAliasMap: Map<RealVariable, PersistentSet<RealVariable>>

    val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    fun unwrapVariable(variable: DataFlowVariable): DataFlowVariable {
        return if (variable is RealVariable) unwrapVariable(variable) else variable
    }

    fun unwrapVariable(variable: RealVariable): RealVariable {
        return directAliasMap[variable] ?: variable
    }

    /**
     * Collects all smartcast information available for [variable],
     * including smart casts from one-way aliases to it.
     *
     * Avoid reading this information back into the [Flow] as it
     * will produce redundant smart casts on [variable] itself.
     * To merge flows or update the existing information, use [getTypeStatement].
     */
    fun getTypeStatementWithOneWayData(variable: DataFlowVariable): TypeStatement? {
        return combineTypeStatements(variable, approvedTypeStatements, oneWayAliasMap)
    }

    fun getTypeStatement(variable: DataFlowVariable): TypeStatement? {
        return approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)
    }

    fun getImplications(variable: DataFlowVariable): Collection<Implication>? {
        return implications[variable]
    }

}

class PersistentFlow internal constructor(
    previousFlow: PersistentFlow?,
    override val approvedTypeStatements: PersistentMap<DataFlowVariable, PersistentTypeStatement>,
    override val implications: PersistentMap<DataFlowVariable, PersistentList<Implication>>,
    override val assignmentIndex: PersistentMap<RealVariable, Int>,
    override val directAliasMap: PersistentMap<RealVariable, RealVariable>,
    override val backwardsAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
    override val oneWayAliasMap: PersistentMap<RealVariable, PersistentSet<RealVariable>>,
) : Flow(previousFlow) {
    private val level: Int = if (previousFlow != null) previousFlow.level + 1 else 0

    val allVariablesForDebug: Set<DataFlowVariable>
        get() = knownVariables + implications.keys + implications.values.flatten().map { it.effect.variable }

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
        oneWayAliasMap.builder(),
    )
}

class MutableFlow internal constructor(
    previousFlow: PersistentFlow?,
    override val approvedTypeStatements: PersistentMap.Builder<DataFlowVariable, PersistentTypeStatement>,
    override val implications: PersistentMap.Builder<DataFlowVariable, PersistentList<Implication>>,
    override val assignmentIndex: PersistentMap.Builder<RealVariable, Int>,
    override val directAliasMap: PersistentMap.Builder<RealVariable, RealVariable>,
    override val backwardsAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
    override val oneWayAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
) : Flow(previousFlow) {
    constructor() : this(
        null,
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
        emptyPersistentHashMapBuilder(),
    )

    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow,
        approvedTypeStatements.build(),
        implications.build(),
        assignmentIndex.build(),
        directAliasMap.build(),
        backwardsAliasMap.build(),
        oneWayAliasMap.build(),
    )
}

private fun transitiveClosure(
    over: DataFlowVariable,
    to: MutableList<PersistentTypeStatement>,
    approvedTypeStatements: Map<DataFlowVariable, PersistentTypeStatement>,
    oneWayAliasMap: Map<RealVariable, PersistentSet<RealVariable>>,
): List<TypeStatement> = to.also {
    oneWayAliasMap[over]?.forEach {
        approvedTypeStatements[it]?.let(to::add)
        transitiveClosure(over = it, to = to, approvedTypeStatements, oneWayAliasMap)
    }
}

private fun Flow.combineTypeStatements(
    variable: DataFlowVariable,
    approvedTypeStatements: Map<DataFlowVariable, PersistentTypeStatement>,
    oneWayAliasMap: Map<RealVariable, PersistentSet<RealVariable>>,
): TypeStatement? {
    val unwrapped = unwrapVariable(variable)
    val ownStatement = approvedTypeStatements[unwrapped]
    val oneWayAliasMapStatements = transitiveClosure(over = unwrapped, to = mutableListOf(), approvedTypeStatements, oneWayAliasMap)
        .takeIf { it.isNotEmpty() }
        ?: return ownStatement?.copy(variable = variable)

    val combinedUpper = emptyPersistentHashSetBuilder<ConeKotlinType>()
        .apply { ownStatement?.upperTypes?.let { addAll(it) } }
    val combinedLower = emptyPersistentHashSetBuilder<DfaType>()
        .apply { ownStatement?.lowerTypes?.let { addAll(it) } }

    for ((upperTypes, lowerTypes) in oneWayAliasMapStatements) {
        combinedUpper.addAll(upperTypes)
        combinedLower.addAll(lowerTypes)
    }

    return PersistentTypeStatement(variable, combinedUpper.build(), combinedLower.build())
}

private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()

private fun <T> emptyPersistentHashSetBuilder(): PersistentSet.Builder<T> =
    persistentHashSetOf<T>().builder()

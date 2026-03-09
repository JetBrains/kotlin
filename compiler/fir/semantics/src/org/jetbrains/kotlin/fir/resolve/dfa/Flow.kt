/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentSetOf
import org.jetbrains.kotlin.fir.expressions.DomainStatus

abstract class Flow {
    abstract val knownVariables: Set<DataFlowVariable>
    abstract fun unwrapVariable(variable: RealVariable): RealVariable
    abstract fun getTypeStatement(variable: DataFlowVariable): TypeStatement?
    abstract fun getImplications(variable: DataFlowVariable): Collection<Implication>?

    open fun unwrapVariable(variable: DataFlowVariable): DataFlowVariable =
        if (variable is RealVariable) unwrapVariable(variable) else variable

    abstract val knownDomains: Set<Domain>
    abstract fun getReferences(domain: Domain): Set<DataFlowVariable>
    abstract fun getDomains(variable: DataFlowVariable): Set<Domain>
    abstract fun domainStatus(domain: Domain): DomainStatus
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
    internal val domainStatus: PersistentMap<Domain, DomainStatus>,
    internal val domainReferences: PersistentMap<Domain, PersistentSet<DataFlowVariable>>,
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

    override val knownDomains: Set<Domain>
        get() = domainStatus.keys + domainReferences.keys

    override fun getReferences(domain: Domain): Set<DataFlowVariable> =
        domainReferences[domain] ?: emptySet()

    override fun getDomains(variable: DataFlowVariable): Set<Domain> =
        domainReferences.filterValues { it.contains(variable) }.keys

    override fun domainStatus(domain: Domain): DomainStatus =
        domainStatus[domain] ?: DomainStatus.OK

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
        domainStatus.builder(),
        domainReferences.builder(),
    )
}

class MutableFlow internal constructor(
    private val previousFlow: PersistentFlow?,
    internal val approvedTypeStatements: PersistentMap.Builder<DataFlowVariable, PersistentTypeStatement>,
    internal val implications: PersistentMap.Builder<DataFlowVariable, PersistentList<Implication>>,
    internal val assignmentIndex: PersistentMap.Builder<RealVariable, Int>,
    internal val directAliasMap: PersistentMap.Builder<RealVariable, RealVariable>,
    internal val backwardsAliasMap: PersistentMap.Builder<RealVariable, PersistentSet<RealVariable>>,
    internal val domainStatus: PersistentMap.Builder<Domain, DomainStatus>,
    internal val domainReferences: PersistentMap.Builder<Domain, PersistentSet<DataFlowVariable>>,
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

    override val knownVariables: Set<DataFlowVariable>
        get() = approvedTypeStatements.keys + directAliasMap.keys

    override fun unwrapVariable(variable: RealVariable): RealVariable =
        directAliasMap[variable] ?: variable

    override fun getTypeStatement(variable: DataFlowVariable): TypeStatement? =
        approvedTypeStatements[unwrapVariable(variable)]?.copy(variable = variable)

    override fun getImplications(variable: DataFlowVariable): Collection<Implication>? =
        implications[variable]

    override val knownDomains: Set<Domain>
        get() = domainStatus.keys + domainReferences.keys

    override fun getReferences(domain: Domain): Set<DataFlowVariable> =
        domainReferences[domain] ?: emptySet()

    override fun getDomains(variable: DataFlowVariable): Set<Domain> {
        val variable = unwrapVariable(variable)
        val domains = domainReferences.filterValues { it.contains(variable) }.keys
        if (domains.isNotEmpty()) return domains

        // create a fresh domain for this variable
        val fresh = Domain.fresh(variable)
        domainReferences[fresh] = persistentSetOf(variable)
        return setOf(fresh)
    }

    override fun domainStatus(domain: Domain): DomainStatus =
        domainStatus[domain] ?: DomainStatus.OK

    fun freeze(): PersistentFlow = PersistentFlow(
        previousFlow,
        approvedTypeStatements.build(),
        implications.build(),
        assignmentIndex.build(),
        directAliasMap.build(),
        backwardsAliasMap.build(),
        domainStatus.build(),
        domainReferences.build(),
    )
}

private fun <K, V> emptyPersistentHashMapBuilder(): PersistentMap.Builder<K, V> =
    persistentHashMapOf<K, V>().builder()

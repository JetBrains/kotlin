/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.HashMultimap

class Flow(
    val approvedFacts: MutableMap<DataFlowVariable, FirDataFlowInfo> = mutableMapOf(),
    val notApprovedFacts: HashMultimap<DataFlowVariable, UnapprovedFirDataFlowInfo> = HashMultimap.create(),
    private var state: State = State.Building
) {
    private val isFrozen: Boolean get() = state == State.Frozen

    fun freeze() {
        state = State.Frozen
    }

    fun addApprovedFact(variable: DataFlowVariable, info: FirDataFlowInfo): Flow {
        if (isFrozen) return copyForBuilding().addApprovedFact(variable, info)
        approvedFacts.compute(variable) { _, existingInfo ->
            if (existingInfo == null) info
            else existingInfo + info
        }
        return this
    }

    fun addNotApprovedFact(variable: DataFlowVariable, info: UnapprovedFirDataFlowInfo): Flow {
        if (isFrozen) return copyForBuilding().addNotApprovedFact(variable, info)
        notApprovedFacts.put(variable, info)
        return this
    }

    fun copyNotApprovedFacts(
        from: DataFlowVariable,
        to: DataFlowVariable,
        transform: ((UnapprovedFirDataFlowInfo) -> UnapprovedFirDataFlowInfo)? = null
    ): Flow {
        if (isFrozen)
            return copyForBuilding().copyNotApprovedFacts(from, to, transform)
        var facts = if (from.isSynthetic) {
            notApprovedFacts.removeAll(from)
        } else {
            notApprovedFacts[from]
        }
        if (transform != null) {
            facts = facts.mapTo(mutableSetOf(), transform)
        }
        notApprovedFacts.putAll(to, facts)
        return this
    }

    fun approvedFacts(variable: DataFlowVariable): FirDataFlowInfo? {
        return approvedFacts[variable]
    }

    fun removeVariableFromFlow(variable: DataFlowVariable): Flow {
        if (isFrozen) return copyForBuilding().removeVariableFromFlow(variable)
        notApprovedFacts.removeAll(variable)
        approvedFacts.remove(variable)
        return this
    }

    companion object {
        val EMPTY = Flow(mutableMapOf(), HashMultimap.create(), State.Frozen)
    }

    enum class State {
        Building, Frozen
    }

    fun copy(): Flow {
        return when (state) {
            State.Frozen -> this
            State.Building -> copyForBuilding()
        }
    }

    fun copyForBuilding(): Flow {
        return Flow(approvedFacts.toMutableMap(), notApprovedFacts.copy(), State.Building)
    }
}

private fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> = HashMultimap.create(this)
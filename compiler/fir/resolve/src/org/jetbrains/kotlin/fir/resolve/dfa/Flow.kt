/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.HashMultimap

class Flow(
    val approvedInfos: MutableMap<DataFlowVariable, FirDataFlowInfo> = mutableMapOf(),
    val conditionalInfos: HashMultimap<DataFlowVariable, ConditionalFirDataFlowInfo> = HashMultimap.create(),
    private var state: State = State.Building
) {
    private val isFrozen: Boolean get() = state == State.Frozen

    fun freeze() {
        state = State.Frozen
    }

    fun addApprovedFact(variable: DataFlowVariable, info: FirDataFlowInfo): Flow {
        if (isFrozen) return copyForBuilding().addApprovedFact(variable, info)
        approvedInfos.compute(variable) { _, existingInfo ->
            if (existingInfo == null) info
            else existingInfo + info
        }
        return this
    }

    fun addNotApprovedFact(variable: DataFlowVariable, info: ConditionalFirDataFlowInfo): Flow {
        if (isFrozen) return copyForBuilding().addNotApprovedFact(variable, info)
        conditionalInfos.put(variable, info)
        return this
    }

    fun copyNotApprovedFacts(
        from: DataFlowVariable,
        to: DataFlowVariable,
        transform: ((ConditionalFirDataFlowInfo) -> ConditionalFirDataFlowInfo)? = null
    ): Flow {
        if (isFrozen)
            return copyForBuilding().copyNotApprovedFacts(from, to, transform)
        var facts = if (from.isSynthetic) {
            conditionalInfos.removeAll(from)
        } else {
            conditionalInfos[from]
        }
        if (transform != null) {
            facts = facts.mapTo(mutableSetOf(), transform)
        }
        conditionalInfos.putAll(to, facts)
        return this
    }

    fun approvedFacts(variable: DataFlowVariable): FirDataFlowInfo? {
        return approvedInfos[variable]
    }

    fun removeVariableFromFlow(variable: DataFlowVariable): Flow {
        if (isFrozen) return copyForBuilding().removeVariableFromFlow(variable)
        conditionalInfos.removeAll(variable)
        approvedInfos.remove(variable)
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
        return Flow(approvedInfos.toMutableMap(), conditionalInfos.copy(), State.Building)
    }
}

private fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> = HashMultimap.create(this)
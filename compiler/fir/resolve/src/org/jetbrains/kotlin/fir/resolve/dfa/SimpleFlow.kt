/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.HashMultimap

class SimpleFlow private constructor(
    val approvedInfos: MutableMap<RealDataFlowVariable, FirDataFlowInfo>,
    val conditionalInfos: HashMultimap<DataFlowVariable, ConditionalFirDataFlowInfo>,
    private var state: State = State.Building
) {
    constructor(
        approvedInfos: MutableMap<RealDataFlowVariable, FirDataFlowInfo> = mutableMapOf(),
        conditionalInfos: HashMultimap<DataFlowVariable, ConditionalFirDataFlowInfo> = HashMultimap.create()
    ) : this(approvedInfos, conditionalInfos, State.Building)

    companion object {
        val EMPTY = SimpleFlow(mutableMapOf(), HashMultimap.create(), State.Frozen)
    }

    private val isFrozen: Boolean get() = state == State.Frozen

    fun freeze() {
        state = State.Frozen
    }

    fun addApprovedFact(variable: RealDataFlowVariable, info: FirDataFlowInfo): SimpleFlow {
        if (isFrozen) return copyForBuilding().addApprovedFact(variable, info)
        approvedInfos.compute(variable) { _, existingInfo ->
            if (existingInfo == null) info
            else existingInfo + info
        }
        return this
    }

    fun addNotApprovedFact(variable: DataFlowVariable, info: ConditionalFirDataFlowInfo): SimpleFlow {
        if (isFrozen) return copyForBuilding().addNotApprovedFact(variable, info)
        conditionalInfos.put(variable, info)
        return this
    }

    fun copyNotApprovedFacts(
        from: DataFlowVariable,
        to: DataFlowVariable,
        transform: ((ConditionalFirDataFlowInfo) -> ConditionalFirDataFlowInfo)? = null
    ): SimpleFlow {
        if (isFrozen) {
            return copyForBuilding().copyNotApprovedFacts(from, to, transform)
        }

        var facts = if (from.isSynthetic()) {
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

    fun approvedFacts(variable: RealDataFlowVariable): FirDataFlowInfo? {
        return approvedInfos[variable]
    }

    fun removeVariableFromFlow(variable: DataFlowVariable): SimpleFlow {
        if (isFrozen) return copyForBuilding().removeVariableFromFlow(variable)
        conditionalInfos.removeAll(variable)
        approvedInfos.remove(variable)
        return this
    }

    private enum class State {
        Building, Frozen
    }

    fun copy(): SimpleFlow {
        return when (state) {
            State.Frozen -> this
            State.Building -> copyForBuilding()
        }
    }

    fun copyForBuilding(): SimpleFlow {
        return SimpleFlow(approvedInfos.toMutableMap(), conditionalInfos.copy(), State.Building)
    }
}

private fun <K, V> HashMultimap<K, V>.copy(): HashMultimap<K, V> = HashMultimap.create(this)
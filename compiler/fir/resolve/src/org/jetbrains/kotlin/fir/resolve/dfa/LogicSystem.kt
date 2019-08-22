/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.HashMultimap

class LogicSystem(private val context: DataFlowInferenceContext) {
    private fun <E> List<Set<E>>.intersectSets(): Set<E> = takeIf { isNotEmpty() }?.reduce { x, y -> x.intersect(y) } ?: emptySet()

    fun or(storages: Collection<Flow>): Flow {
        storages.singleOrNull()?.let {
            return it.copy()
        }
        val approvedFacts = mutableMapOf<DataFlowVariable, FirDataFlowInfo>().apply {
            storages.map { it.approvedFacts.keys }
                .intersectSets()
                .forEach { variable ->
                    val infos = storages.map { it.approvedFacts[variable]!! }
                    if (infos.isNotEmpty()) {
                        this[variable] = context.or(infos)
                    }
                }
        }

        val notApprovedFacts = HashMultimap.create<DataFlowVariable, UnapprovedFirDataFlowInfo>()
            .apply {
                storages.map { it.notApprovedFacts.keySet() }
                    .intersectSets()
                    .forEach { variable ->
                        val infos = storages.map { it.notApprovedFacts[variable] }.intersectSets()
                        if (infos.isNotEmpty()) {
                            this.putAll(variable, infos)
                        }
                    }
            }
        return Flow(approvedFacts, notApprovedFacts)
    }

    fun andForVerifiedFacts(
        left: Map<DataFlowVariable, FirDataFlowInfo>?,
        right: Map<DataFlowVariable, FirDataFlowInfo>?
    ): Map<DataFlowVariable, FirDataFlowInfo>? {
        if (left.isNullOrEmpty()) return right
        if (right.isNullOrEmpty()) return left

        val map = mutableMapOf<DataFlowVariable, FirDataFlowInfo>()
        for (variable in left.keys.union(right.keys)) {
            val leftInfo = left[variable]
            val rightInfo = right[variable]
            map[variable] = context.and(listOfNotNull(leftInfo, rightInfo))
        }
        return map
    }

    fun orForVerifiedFacts(
        left: Map<DataFlowVariable, FirDataFlowInfo>?,
        right: Map<DataFlowVariable, FirDataFlowInfo>?
    ): Map<DataFlowVariable, FirDataFlowInfo>? {
        if (left.isNullOrEmpty() || right.isNullOrEmpty()) return null
        val map = mutableMapOf<DataFlowVariable, FirDataFlowInfo>()
        for (variable in left.keys.intersect(right.keys)) {
            val leftInfo = left[variable]!!
            val rightInfo = right[variable]!!
            map[variable] = context.or(listOf(leftInfo, rightInfo))
        }
        return map
    }

    fun approveFactsInsideFlow(proof: Condition, flow: Flow): Flow {
        val notApprovedFacts: Set<UnapprovedFirDataFlowInfo> = flow.notApprovedFacts[proof.variable]
        if (notApprovedFacts.isEmpty()) {
            return flow
        }
        @Suppress("NAME_SHADOWING")
        val flow = flow.copyForBuilding()
        val newFacts = HashMultimap.create<DataFlowVariable, FirDataFlowInfo>()
        notApprovedFacts.forEach {
            if (it.condition == proof.rhs) {
                newFacts.put(it.variable, it.info)
            }
        }
        newFacts.asMap().forEach { (variable, infos) ->
            @Suppress("NAME_SHADOWING")
            val infos = ArrayList(infos)
            flow.approvedFacts[variable]?.let {
                infos.add(it)
            }
            flow.approvedFacts[variable] = context.and(infos)
        }
        return flow
    }

    fun approveFact(proof: Condition, flow: Flow): MutableMap<DataFlowVariable, FirDataFlowInfo> {
        val notApprovedFacts: Set<UnapprovedFirDataFlowInfo> = flow.notApprovedFacts[proof.variable]
        if (notApprovedFacts.isEmpty()) {
            return mutableMapOf()
        }
        val newFacts = HashMultimap.create<DataFlowVariable, FirDataFlowInfo>()
        notApprovedFacts.forEach {
            if (it.condition == proof.rhs) {
                newFacts.put(it.variable, it.info)
            }
        }
        return newFacts.asMap().mapValuesTo(mutableMapOf()) { (_, infos) -> context.and(infos) }
    }
}
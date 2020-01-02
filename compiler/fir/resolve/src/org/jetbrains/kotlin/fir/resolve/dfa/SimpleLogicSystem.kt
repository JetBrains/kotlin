/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext

private class SimpleFlow(
    val approvedInfos: MutableApprovedInfos = mutableMapOf(),
    val conditionalInfos: ConditionalInfos = HashMultimap.create()
) : Flow {
    override fun getApprovedInfo(variable: RealDataFlowVariable): FirDataFlowInfo? {
        return approvedInfos[variable]
    }

    override fun getConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo> {
        return conditionalInfos[variable]
    }

    override fun getVariablesInApprovedInfos(): Collection<RealDataFlowVariable> {
        return approvedInfos.keys
    }

    override fun removeConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo> {
        return conditionalInfos.removeAll(variable)
    }
}

abstract class SimpleLogicSystem(context: ConeInferenceContext) : LogicSystem(context) {
    override val Flow.approvedInfos: MutableApprovedInfos
        get() = (this as SimpleFlow).approvedInfos

    override val Flow.conditionalInfos: ConditionalInfos
        get() = (this as SimpleFlow).conditionalInfos

    override fun createEmptyFlow(): Flow {
        return SimpleFlow()
    }

    override fun forkFlow(flow: Flow): Flow {
        require(flow is SimpleFlow)
        return SimpleFlow(
            flow.approvedInfos.mapValuesTo(mutableMapOf()) { (_, info) -> info.copy() },
            flow.conditionalInfos.copy()
        )
    }

    override fun joinFlow(flows: Collection<Flow>): Flow {
        @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
        val flows = flows as Collection<SimpleFlow>
        flows.singleOrNull()?.let { return it }
        val approvedFacts: MutableApprovedInfos = mutableMapOf()
        flows.map { it.approvedInfos.keys }
            .intersectSets()
            .forEach { variable ->
                val infos = flows.map { it.approvedInfos[variable]!! }
                if (infos.isNotEmpty()) {
                    approvedFacts[variable] = or(infos)
                }
            }

        val notApprovedFacts: ConditionalInfos = ArrayListMultimap.create()
        flows.map { it.conditionalInfos.keySet() }
            .intersectSets()
            .forEach { variable ->
                val infos = flows.map { it.conditionalInfos[variable] }.intersectSets()
                if (infos.isNotEmpty()) {
                    notApprovedFacts.putAll(variable, infos)
                }
            }

        val result = SimpleFlow(approvedFacts, notApprovedFacts)
        updateAllReceivers(result)
        return result
    }

    override fun collectInfoForBooleanOperator(
        leftFlow: Flow,
        leftVariable: DataFlowVariable,
        rightFlow: Flow,
        rightVariable: DataFlowVariable
    ): InfoForBooleanOperator {
        require(leftFlow is SimpleFlow && rightFlow is SimpleFlow)
        return InfoForBooleanOperator(
            leftFlow.conditionalInfos[leftVariable],
            rightFlow.conditionalInfos[rightVariable],
            rightFlow.approvedInfos - leftFlow.approvedInfos
        )
    }

    private operator fun ApprovedInfos.minus(other: ApprovedInfos): ApprovedInfos {
        val result: MutableApprovedInfos = mutableMapOf()
        forEach { (variable, info) ->
            require(info is MutableFirDataFlowInfo)
            result[variable] = other[variable]?.let { info - it } ?: info
        }
        return result
    }
}

private fun <K, V> Multimap<K, V>.copy(): Multimap<K, V> = ArrayListMultimap.create(this)

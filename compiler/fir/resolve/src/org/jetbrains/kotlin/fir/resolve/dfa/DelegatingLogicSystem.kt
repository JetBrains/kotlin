/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap

class DelegatingFlow(
    val previousFlow: DelegatingFlow?,
    val approvedInfos: MutableApprovedInfos = mutableMapOf(),
    val conditionalInfos: ConditionalInfos = ArrayListMultimap.create()
) : Flow {
    val level: Int = previousFlow?.level?.plus(1) ?: 0

    override fun getApprovedInfo(variable: RealDataFlowVariable): FirDataFlowInfo? {
        val info = MutableFirDataFlowInfo(mutableSetOf(), mutableSetOf())
        collect { flow ->
            flow.approvedInfos[variable]?.let { info += it }
            true
        }
        return info.takeIf { it.isNotEmpty }
    }

    override fun getConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo> {
        val result = mutableSetOf<ConditionalFirDataFlowInfo>()
        val isReal = !variable.isSynthetic()
        collect {
            val infos = it.conditionalInfos[variable]
            result += infos
            /*
             * Here we use invariant that if we found some conditional info about synthetic variable
             *   on some level then there is definitely no another info on other levels of flow
             * It's correct because we can add condtional info about synthetic variable only
             *   when we first met expression corresponding to that variable
             */
            isReal || infos.isEmpty()
        }
        return result
    }

    override fun getVariablesInApprovedInfos(): Collection<RealDataFlowVariable> {
        val result = mutableSetOf<RealDataFlowVariable>()
        collect {
            result += approvedInfos.keys
            true
        }
        return result
    }

    override fun removeConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo> {
        return conditionalInfos.removeAll(variable)
    }

    private inline fun collect(block: (DelegatingFlow) -> Boolean) {
        var flow: DelegatingFlow? = this
        while (flow != null) {
            val shouldContinue = block(flow)
            if (!shouldContinue) break
            flow = flow.previousFlow
        }
    }
}

fun DelegatingFlow.approvedInfosFromTopFlow(): ApprovedInfos {
    return approvedInfos
}

fun DelegatingFlow.conditionalInfosFromTopFlow(): ConditionalInfos {
    return ImmutableMultimap(conditionalInfos)
}

private fun DelegatingFlow.approvedInfosUntilParent(parent: DelegatingFlow): ApprovedInfos {
    return when(level - parent.level) {
        0 -> emptyMap()
        1 -> approvedInfosFromTopFlow()
        else -> mutableMapOf<RealDataFlowVariable, FirDataFlowInfo>().apply {
            traverse(parent) {
                it.approvedInfosFromTopFlow().forEach { (variable, info) ->
                    compute(variable) { _, existingInfo ->
                        existingInfo?.plus(info) ?: info
                    }
                }
            }
        }
    }
}

private inline fun DelegatingFlow.traverse(untilFlow: DelegatingFlow, block: (DelegatingFlow) -> Unit) {
    var flow = this
    while (flow != untilFlow) {
        block(flow)
        flow = flow.previousFlow!!
    }
}

private class ImmutableMultimap<K, V>(private val original: Multimap<K, V>) : Multimap<K, V> by original {
    override fun put(p0: K?, p1: V?): Boolean {
        throw IllegalStateException()
    }

    override fun remove(p0: Any?, p1: Any?): Boolean {
        throw IllegalStateException()
    }

    override fun putAll(p0: Multimap<out K, out V>): Boolean {
        throw IllegalStateException()
    }

    override fun putAll(p0: K?, p1: MutableIterable<V>): Boolean {
        throw IllegalStateException()
    }

    override fun removeAll(p0: Any?): MutableCollection<V> {
        throw IllegalStateException()
    }

    override fun replaceValues(p0: K?, p1: MutableIterable<V>): MutableCollection<V> {
        throw IllegalStateException()
    }
}

abstract class DelegatingLogicSystem(context: DataFlowInferenceContext) : LogicSystem(context) {
    override val Flow.approvedInfos: MutableApprovedInfos
        get() = (this as DelegatingFlow).approvedInfos

    override val Flow.conditionalInfos: ConditionalInfos
        get() = (this as DelegatingFlow).conditionalInfos

    override fun createEmptyFlow(): Flow {
        return DelegatingFlow(null)
    }

    override fun forkFlow(flow: Flow): Flow {
        require(flow is DelegatingFlow)
        return DelegatingFlow(flow)
    }

    override fun collectInfoForBooleanOperator(
        leftFlow: Flow,
        leftVariable: DataFlowVariable,
        rightFlow: Flow,
        rightVariable: DataFlowVariable
    ): InfoForBooleanOperator {
        require(leftFlow is DelegatingFlow && rightFlow is DelegatingFlow)
        val parent = lowestCommonFlow(leftFlow, rightFlow)

        return InfoForBooleanOperator(
            leftFlow.previousFlow!!.conditionalInfosFromTopFlow()[leftVariable],
            rightFlow.conditionalInfosFromTopFlow()[rightVariable],
            rightFlow.approvedInfosUntilParent(parent)
        )
    }

    // ------------------------------- Flow operations -------------------------------

    override fun joinFlow(flows: Collection<Flow>): Flow {
        if (flows.isEmpty()) return createEmptyFlow()
        flows.singleOrNull()?.let { return it }

        @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
        val flows = flows as Collection<DelegatingFlow>
        val commonFlow = flows.reduce(this::lowestCommonFlow)

        val approvedInfosFromAllFlows = mutableListOf<ApprovedInfos>()
        flows.forEach {
            approvedInfosFromAllFlows += it.collectInfos(commonFlow)
        }

        // approved info
        val variablesFromApprovedInfos = approvedInfosFromAllFlows.map { it.keys }.intersectSets()
        for (variable in variablesFromApprovedInfos) {
            val infos = approvedInfosFromAllFlows.mapNotNull { it[variable] }
            if (infos.size != flows.size) continue
            val intersectedInfo = or(infos)
            if (intersectedInfo.isEmpty) continue
            val existingInfo = commonFlow.approvedInfos[variable]
            if (existingInfo == null) {
                commonFlow.approvedInfos[variable] = intersectedInfo
            } else {
                existingInfo += intersectedInfo
            }
        }

        updateAllReceivers(commonFlow)

        return commonFlow
    }

    // ------------------------------- Util functions -------------------------------

    private fun lowestCommonFlow(left: DelegatingFlow, right: DelegatingFlow): DelegatingFlow {
        val level = minOf(left.level, right.level)
        @Suppress("NAME_SHADOWING")
        var left = left
        while (left.level > level) {
            left = left.previousFlow!!
        }
        @Suppress("NAME_SHADOWING")
        var right = right
        while (right.level > level) {
            right = right.previousFlow!!
        }
        while (left != right) {
            left = left.previousFlow!!
            right = right.previousFlow!!
        }
        return left
    }

    private fun DelegatingFlow.collectInfos(untilFlow: DelegatingFlow): ApprovedInfos {
        val approvedInfos: MutableApprovedInfos = mutableMapOf()

        // val conditionalInfos: ConditionalInfos = ArrayListMultimap.create()

        var flow = this
        while (flow != untilFlow) {
            flow.approvedInfos.forEach { (variable, info) -> approvedInfos.addInfo(variable, info) }
            /*
             * we don't join conditional infos yet
            flow.conditionalInfos.asMap().forEach { (variable, infos) ->
                conditionalInfos.putAll(variable, infos)
            }
             */
            flow = flow.previousFlow!!
        }
        return approvedInfos
    }
}
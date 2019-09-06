/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.jetbrains.kotlin.fir.types.ConeKotlinType

interface DelegatingFlow {
    companion object {
        fun createDefaultFlow(): DelegatingFlow = DelegatingFlowImpl(null)
    }

    fun getApprovedInfo(variable: RealDataFlowVariable): FirDataFlowInfo?
    fun getConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo>

    fun getVariablesInApprovedInfos(): Collection<RealDataFlowVariable>

    fun createNewFlow(): DelegatingFlow
}

private class DelegatingFlowImpl(
    val previousFlow: DelegatingFlowImpl?,
    val approvedInfos: ApprovedInfos = mutableMapOf(),
    val conditionalInfos: ConditionalInfos = ArrayListMultimap.create()
) : DelegatingFlow {
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

    override fun createNewFlow(): DelegatingFlowImpl {
        return DelegatingFlowImpl(this)
    }

    private inline fun collect(block: (DelegatingFlowImpl) -> Boolean) {
        var flow: DelegatingFlowImpl? = this
        while (flow != null) {
            val shouldContinue = block(flow)
            if (!shouldContinue) break
            flow = flow.previousFlow
        }
    }
}

fun DelegatingFlow.approvedInfosFromTopFlow(): Map<RealDataFlowVariable, FirDataFlowInfo> {
    require(this is DelegatingFlowImpl)
    return approvedInfos
}

fun DelegatingFlow.conditionalInfosFromTopFlow(): Multimap<DataFlowVariable, ConditionalFirDataFlowInfo> {
    require(this is DelegatingFlowImpl)
    return ImmutableMultimap(conditionalInfos)
}

fun ApprovedInfos.mergeInfo(other: Map<RealDataFlowVariable, FirDataFlowInfo>) {
    other.forEach { (variable, info) ->
        merge(variable, info) { existingInfo, newInfo ->
            (existingInfo as MutableFirDataFlowInfo) += newInfo
            existingInfo
        }
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

abstract class DelegatingLogicSystem(private val context: DataFlowInferenceContext) {

    // ------------------------------- Flow operations -------------------------------

    fun mergeFlows(flows: Collection<DelegatingFlow>): DelegatingFlow {
        if (flows.isEmpty()) return DelegatingFlow.createDefaultFlow()
        flows.singleOrNull()?.let { return it }

        @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
        val flows = flows as Collection<DelegatingFlowImpl>
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
                (existingInfo as MutableFirDataFlowInfo) += intersectedInfo
            }
        }

        updateAllReceivers(commonFlow)

        return commonFlow
    }

    /*
     *  used for:
     *   1. val b = x is String
     *   2. b = x is String
     *   3. !b | b.not()   for Booleans
     */
    fun changeVariableForConditionFlow(
        flow: DelegatingFlow,
        sourceVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        transform: ((ConditionalFirDataFlowInfo) -> ConditionalFirDataFlowInfo)? = null
    ) {
        require(flow is DelegatingFlowImpl)
        var infos = flow.getConditionalInfos(sourceVariable)
        if (transform != null) {
            infos = infos.map(transform)
        }
        flow.conditionalInfos.putAll(newVariable, infos)
        if (sourceVariable.isSynthetic()) {
            flow.conditionalInfos.removeAll(sourceVariable)
        }
    }

    fun approveFactsInsideFlow(
        variable: DataFlowVariable,
        condition: Condition,
        flow: DelegatingFlow,
        shouldCreateNewFlow: Boolean,
        shouldRemoveSynthetics: Boolean
    ): DelegatingFlow {
        require(flow is DelegatingFlowImpl)

        val notApprovedFacts: Collection<ConditionalFirDataFlowInfo> = if (shouldRemoveSynthetics && variable.isSynthetic()) {
            flow.conditionalInfos.removeAll(variable)
        } else {
            flow.getConditionalInfos(variable)
        }

        val resultFlow = if (shouldCreateNewFlow) flow.createNewFlow() else flow
        if (notApprovedFacts.isEmpty()) {
            return resultFlow
        }

        val newFacts = ArrayListMultimap.create<RealDataFlowVariable, FirDataFlowInfo>()
        notApprovedFacts.forEach {
            if (it.condition == condition) {
                newFacts.put(it.variable, it.info)
            }
        }

        val updatedReceivers = mutableSetOf<RealDataFlowVariable>()

        newFacts.asMap().forEach { (variable, infos) ->
            @Suppress("NAME_SHADOWING")
            val info = MutableFirDataFlowInfo()
            infos.forEach {
                info += it
            }
            if (variable.isThisReference) {
                updatedReceivers += variable
            }
            addApprovedInfo(resultFlow, variable, info)
        }
        updatedReceivers.forEach {
            processUpdatedReceiverVariable(resultFlow, it)
        }
        return resultFlow
    }

    fun addApprovedInfo(flow: DelegatingFlow, variable: RealDataFlowVariable, info: FirDataFlowInfo) {
        assert(info is MutableFirDataFlowInfo)
        require(flow is DelegatingFlowImpl)
        flow.approvedInfos.addInfo(variable, info)
        if (variable.isThisReference) {
            processUpdatedReceiverVariable(flow, variable)
        }
    }

    fun addConditionalInfo(flow: DelegatingFlow, variable: DataFlowVariable, info: ConditionalFirDataFlowInfo) {
        require(flow is DelegatingFlowImpl)
        flow.conditionalInfos.put(variable, info)
    }

    // ------------------------------- DataFlowInfo operations -------------------------------

    fun orForVerifiedFacts(
        left: Map<RealDataFlowVariable, FirDataFlowInfo>,
        right: Map<RealDataFlowVariable, FirDataFlowInfo>
    ): ApprovedInfos {
        if (left.isNullOrEmpty() || right.isNullOrEmpty()) return mutableMapOf()
        val map = mutableMapOf<RealDataFlowVariable, FirDataFlowInfo>()
        for (variable in left.keys.intersect(right.keys)) {
            val leftInfo = left.getValue(variable)
            val rightInfo = right.getValue(variable)
            map[variable] = or(listOf(leftInfo, rightInfo))
        }
        return map
    }

    fun approveFactTo(
        destination: ApprovedInfos,
        condition: Condition,
        notApprovedFacts: Collection<ConditionalFirDataFlowInfo>
    ) {
        if (notApprovedFacts.isEmpty()) {
            return
        }
        notApprovedFacts.forEach {
            if (it.condition == condition) {
                destination.addInfo(it.variable, it.info)
            }
        }
    }

    fun approveFactTo(
        destination: ApprovedInfos,
        variable: DataFlowVariable,
        condition: Condition,
        flow: DelegatingFlow
    ) {
        require(flow is DelegatingFlowImpl)
        val notApprovedFacts: Collection<ConditionalFirDataFlowInfo> = flow.getConditionalInfos(variable)
        approveFactTo(destination, condition, notApprovedFacts)
    }

    fun approveFact(condition: Condition, notApprovedFacts: Collection<ConditionalFirDataFlowInfo>): ApprovedInfos {
        return mutableMapOf<RealDataFlowVariable, FirDataFlowInfo>().apply { approveFactTo(this, condition, notApprovedFacts)}
    }

    fun approveFact(
        variable: DataFlowVariable,
        condition: Condition,
        flow: DelegatingFlow
    ): ApprovedInfos {
        return mutableMapOf<RealDataFlowVariable, FirDataFlowInfo>().apply { approveFactTo(this, variable, condition, flow)}
    }

    // ------------------------------- Util functions -------------------------------

    abstract fun processUpdatedReceiverVariable(flow: DelegatingFlow, variable: RealDataFlowVariable)

    abstract fun updateAllReceivers(flow: DelegatingFlow)

    // ------------------------------- Util functions -------------------------------

    private fun <E> Collection<Set<E>>.intersectSets(): Set<E> = takeIf { isNotEmpty() }?.reduce { x, y -> x.intersect(y) } ?: emptySet()

    private fun or(infos: Collection<FirDataFlowInfo>): FirDataFlowInfo {
        infos.singleOrNull()?.let { return it }
        val exactType = orTypes(infos.map { it.exactType })
        val exactNotType = orTypes(infos.map { it.exactNotType })
        return FirDataFlowInfo(exactType, exactNotType)
    }

    private fun orTypes(types: Collection<Set<ConeKotlinType>>): Set<ConeKotlinType> {
        if (types.any { it.isEmpty() }) return emptySet()
        val allTypes = types.flatMapTo(mutableSetOf()) { it }
        val commonTypes = allTypes.toMutableSet()
        types.forEach { commonTypes.retainAll(it) }
        val differentTypes = allTypes - commonTypes
        context.commonSuperTypeOrNull(differentTypes.toList())?.let { commonTypes += it }
        return commonTypes
    }

    private fun lowestCommonFlow(left: DelegatingFlowImpl, right: DelegatingFlowImpl): DelegatingFlowImpl {
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

    private fun DelegatingFlowImpl.collectInfos(untilFlow: DelegatingFlowImpl): ApprovedInfos {
        val approvedInfos: ApprovedInfos = mutableMapOf()

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
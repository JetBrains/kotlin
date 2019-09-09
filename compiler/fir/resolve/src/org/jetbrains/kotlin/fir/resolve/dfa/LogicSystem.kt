/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.ConeKotlinType

interface Flow {
    fun getApprovedInfo(variable: RealDataFlowVariable): FirDataFlowInfo?
    fun getConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo>

    fun getVariablesInApprovedInfos(): Collection<RealDataFlowVariable>
}

abstract class LogicSystem(private val context: DataFlowInferenceContext) {
    // ------------------------------- Flow operations -------------------------------

    abstract fun createEmptyFlow(): Flow
    abstract fun forkFlow(flow: Flow): Flow
    abstract fun joinFlow(flows: Collection<Flow>): Flow

    abstract fun addApprovedInfo(flow: Flow, variable: RealDataFlowVariable, info: FirDataFlowInfo)
    abstract fun addConditionalInfo(flow: Flow, variable: DataFlowVariable, info: ConditionalFirDataFlowInfo)

    /*
     *  used for:
     *   1. val b = x is String
     *   2. b = x is String
     *   3. !b | b.not()   for Booleans
     */
    abstract fun changeVariableForConditionFlow(
        flow: Flow,
        sourceVariable: DataFlowVariable,
        newVariable: DataFlowVariable,
        transform: ((ConditionalFirDataFlowInfo) -> ConditionalFirDataFlowInfo)? = null
    )

    abstract fun approveFactsInsideFlow(
        variable: DataFlowVariable,
        condition: Condition,
        flow: Flow,
        shouldForkFlow: Boolean,
        shouldRemoveSynthetics: Boolean
    ): Flow

    // ------------------------------- Callbacks for updating implicit receiver stack -------------------------------

    abstract fun processUpdatedReceiverVariable(flow: Flow, variable: RealDataFlowVariable)
    abstract fun updateAllReceivers(flow: Flow)

    // ------------------------------- Public DataFlowInfo util functions -------------------------------

    data class InfoForBooleanOperator(
        val conditionalFromLeft: ConditionalInfos,
        val conditionalFromRight: ConditionalInfos,
        val approvedFromRight: ApprovedInfos
    )

    abstract fun collectInfoForBooleanOperator(leftFlow: Flow, rightFlow: Flow): InfoForBooleanOperator

    fun orForVerifiedFacts(
        left: ApprovedInfos,
        right: ApprovedInfos
    ): MutableApprovedInfos {
        if (left.isNullOrEmpty() || right.isNullOrEmpty()) return mutableMapOf()
        val map = mutableMapOf<RealDataFlowVariable, MutableFirDataFlowInfo>()
        for (variable in left.keys.intersect(right.keys)) {
            val leftInfo = left.getValue(variable)
            val rightInfo = right.getValue(variable)
            map[variable] = or(listOf(leftInfo, rightInfo))
        }
        return map
    }

    fun approveFactTo(destination: MutableApprovedInfos, variable: DataFlowVariable, condition: Condition, flow: Flow) {
        val notApprovedFacts: Collection<ConditionalFirDataFlowInfo> = flow.getConditionalInfos(variable)
        approveFactTo(destination, condition, notApprovedFacts)
    }

    fun approveFactTo(destination: MutableApprovedInfos, condition: Condition, notApprovedFacts: Collection<ConditionalFirDataFlowInfo>) {
        if (notApprovedFacts.isEmpty()) return
        notApprovedFacts.forEach {
            if (it.condition == condition) {
                destination.addInfo(it.variable, it.info)
            }
        }
    }

    fun approveFact(condition: Condition, notApprovedFacts: Collection<ConditionalFirDataFlowInfo>): MutableApprovedInfos {
        return mutableMapOf<RealDataFlowVariable, MutableFirDataFlowInfo>().apply { approveFactTo(this, condition, notApprovedFacts) }
    }

    fun approveFact(variable: DataFlowVariable, condition: Condition, flow: Flow): MutableApprovedInfos {
        return mutableMapOf<RealDataFlowVariable, MutableFirDataFlowInfo>().apply { approveFactTo(this, variable, condition, flow) }
    }

    // ------------------------------- Util functions -------------------------------

    // TODO
    protected fun <E> Collection<Collection<E>>.intersectSets(): Set<E> {
        if (isEmpty()) return emptySet()
        val iterator = iterator()
        val result = HashSet<E>(iterator.next())
        while (iterator.hasNext()) {
            result.retainAll(iterator.next())
        }
        return result
    }

    protected fun or(infos: Collection<FirDataFlowInfo>): MutableFirDataFlowInfo {
        infos.singleOrNull()?.let { return it as MutableFirDataFlowInfo }
        val exactType = orTypes(infos.map { it.exactType })
        val exactNotType = orTypes(infos.map { it.exactNotType })
        return MutableFirDataFlowInfo(exactType, exactNotType)
    }

    private fun orTypes(types: Collection<Set<ConeKotlinType>>): MutableSet<ConeKotlinType> {
        if (types.any { it.isEmpty() }) return mutableSetOf()
        val allTypes = types.flatMapTo(mutableSetOf()) { it }
        val commonTypes = allTypes.toMutableSet()
        types.forEach { commonTypes.retainAll(it) }
        val differentTypes = allTypes - commonTypes
        context.commonSuperTypeOrNull(differentTypes.toList())?.let { commonTypes += it }
        return commonTypes
    }
}
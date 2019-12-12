///*
// * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
// * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
// */
//
//package org.jetbrains.kotlin.fir.resolve.dfa
//
//import kotlinx.collections.immutable.*
//import kotlinx.collections.immutable.PersistentMap
//import kotlinx.collections.immutable.PersistentSet
//import org.jetbrains.kotlin.fir.types.ConeKotlinType
//
//
//private data class PersistentFirDataFlowInfo(
//    override val exactType: PersistentSet<ConeKotlinType>,
//    override val exactNotType: PersistentSet<ConeKotlinType>
//) : FirDataFlowInfo {
//
//    override operator fun plus(other: FirDataFlowInfo): PersistentFirDataFlowInfo {
//        return PersistentFirDataFlowInfo(
//            exactType + other.exactType,
//            exactNotType + other.exactNotType
//        )
//    }
//
//    override fun minus(other: FirDataFlowInfo): FirDataFlowInfo {
//        // TODO
//        throw IllegalStateException()
//    }
//
//    override val isNotEmpty: Boolean
//        get() = exactType.isNotEmpty() || exactNotType.isNotEmpty()
//
//    override fun invert(): PersistentFirDataFlowInfo {
//        return PersistentFirDataFlowInfo(exactNotType, exactType)
//    }
//
////    override fun toMutableInfo(): MutableFirDataFlowInfo {
////        return MutableFirDataFlowInfo(exactType.toMutableSet(), exactNotType.toMutableSet())
////    }
//}
//
//private typealias PersistentApprovedInfos = PersistentMap<RealDataFlowVariable, PersistentFirDataFlowInfo>
//private typealias PersistentConditionalInfos = PersistentMap<DataFlowVariable, PersistentList<ConditionalFirDataFlowInfo>>
//
//private fun FirDataFlowInfo.toPersistent(): PersistentFirDataFlowInfo = PersistentFirDataFlowInfo(
//    exactType.toPersistentSet(),
//    exactNotType.toPersistentSet()
//)
//
//private fun PersistentApprovedInfos.addNewInfo(variable: RealDataFlowVariable, info: FirDataFlowInfo): PersistentApprovedInfos {
//    val existingInfo = this[variable]
//    return if (existingInfo == null) {
//        val persistentInfo = if (info is PersistentFirDataFlowInfo) info else info.toPersistent()
//        put(variable, persistentInfo)
//    } else {
//        put(variable, existingInfo + info)
//    }
//}
//
//private class PersistentFlow : Flow {
//    val previousFlow: PersistentFlow?
//    var approvedInfos: PersistentApprovedInfos
//    var conditionalInfos: PersistentConditionalInfos
//
//    constructor(previousFlow: PersistentFlow) {
//        this.previousFlow = previousFlow
//        approvedInfos = previousFlow.approvedInfos
//        conditionalInfos = previousFlow.conditionalInfos
//        level = previousFlow.level + 1
//    }
//
//    constructor() {
//        previousFlow = null
//        approvedInfos = persistentHashMapOf()
//        conditionalInfos = persistentHashMapOf()
//        level = 1
//    }
//
//    val level: Int
//    var approvedInfosDiff: PersistentApprovedInfos = persistentHashMapOf()
//
//    override fun getApprovedInfo(variable: RealDataFlowVariable): FirDataFlowInfo? {
//        return approvedInfos[variable]
//    }
//
//    override fun getConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo> {
//        return conditionalInfos[variable] ?: emptyList()
//    }
//
//    override fun getVariablesInApprovedInfos(): Collection<RealDataFlowVariable> {
//        return approvedInfos.keys
//    }
//
//    override fun removeConditionalInfos(variable: DataFlowVariable): Collection<ConditionalFirDataFlowInfo> {
//        val result = getConditionalInfos(variable)
//        if (result.isNotEmpty()) {
//            conditionalInfos = conditionalInfos.remove(variable)
//        }
//        return result
//    }
//}
//
//abstract class PersistentLogicSystem(context: DataFlowInferenceContext) : LogicSystem(context) {
//    override fun createEmptyFlow(): Flow {
//        return PersistentFlow()
//    }
//
//    override fun forkFlow(flow: Flow): Flow {
//        require(flow is PersistentFlow)
//        return PersistentFlow(flow)
//    }
//
//    override fun joinFlow(flows: Collection<Flow>): Flow {
//        if (flows.isEmpty()) return createEmptyFlow()
//        flows.singleOrNull()?.let { return it }
//
//        @Suppress("UNCHECKED_CAST", "NAME_SHADOWING")
//        val flows = flows as Collection<PersistentFlow>
//        val commonFlow = flows.reduce(this::lowestCommonFlow)
//
//        val commonVariables = flows.map { it.diffVariablesIterable(commonFlow).toList() }
//            .intersectSets()
//            .takeIf { it.isNotEmpty() }
//            ?: return commonFlow
//
//        for (variable in commonVariables) {
//            val info = or(flows.map { it.getApprovedDiff(variable, commonFlow) })
//            if (info.isEmpty) continue
//            commonFlow.approvedInfos = commonFlow.approvedInfos.addNewInfo(variable, info)
//            commonFlow.approvedInfosDiff = commonFlow.approvedInfosDiff.addNewInfo(variable, info)
//        }
//
//        updateAllReceivers(commonFlow)
//
//        return commonFlow
//    }
//
//    private fun PersistentFlow.diffVariablesIterable(parentFlow: PersistentFlow): Iterable<RealDataFlowVariable> =
//        object : DiffIterable<RealDataFlowVariable>(parentFlow, this) {
//            override fun extractIterator(flow: PersistentFlow): Iterator<RealDataFlowVariable> {
//                return flow.approvedInfosDiff.keys.iterator()
//            }
//        }
//
//    private abstract class DiffIterable<T>(private val parentFlow: PersistentFlow, private var currentFlow: PersistentFlow) : Iterable<T> {
//        private var currentIterator = extractIterator(currentFlow)
//
//        abstract fun extractIterator(flow: PersistentFlow): Iterator<T>
//
//        override fun iterator(): Iterator<T> {
//            return object : Iterator<T> {
//                override fun hasNext(): Boolean {
//                    if (currentIterator.hasNext()) return true
//                    while (currentFlow != parentFlow) {
//                        currentFlow = currentFlow.previousFlow!!
//                        currentIterator = extractIterator(currentFlow)
//                        if (currentIterator.hasNext()) return true
//                    }
//                    return false
//                }
//
//                override fun next(): T {
//                    if (!hasNext()) {
//                        throw NoSuchElementException()
//                    }
//                    return currentIterator.next()
//                }
//            }
//        }
//    }
//
//    private fun PersistentFlow.getApprovedDiff(variable: RealDataFlowVariable, parentFlow: PersistentFlow): MutableFirDataFlowInfo {
//        var flow = this
//        val result = MutableFirDataFlowInfo()
//        while (flow != parentFlow) {
//            flow.approvedInfosDiff[variable]?.let {
//                result += it
//            }
//            flow = flow.previousFlow!!
//        }
//        return result
//    }
//
//    override fun collectInfoForBooleanOperator(
//        leftFlow: Flow,
//        leftVariable: DataFlowVariable,
//        rightFlow: Flow,
//        rightVariable: DataFlowVariable
//    ): InfoForBooleanOperator {
//        require(leftFlow is PersistentFlow && rightFlow is PersistentFlow)
//        return InfoForBooleanOperator(
//            leftFlow.conditionalInfos[leftVariable] ?: emptyList(),
//            rightFlow.conditionalInfos[rightVariable] ?: emptyList(),
//            rightFlow.approvedInfosDiff
//        )
//    }
//
//    override fun changeVariableForConditionFlow(
//        flow: Flow,
//        sourceVariable: DataFlowVariable,
//        newVariable: DataFlowVariable,
//        transform: ((ConditionalFirDataFlowInfo) -> ConditionalFirDataFlowInfo?)?
//    ) {
//        require(flow is PersistentFlow)
//        with(flow) {
//            val existingInfo = conditionalInfos[sourceVariable]?.takeIf { it.isNotEmpty() } ?: return
//            val transformedInfo = if (transform == null) {
//                existingInfo
//            } else {
//                existingInfo.map(transform).toPersistentList()
//            }
//            conditionalInfos = conditionalInfos.remove(sourceVariable).put(newVariable, transformedInfo)
//        }
//    }
//
//    override fun addApprovedInfo(flow: Flow, variable: RealDataFlowVariable, info: FirDataFlowInfo) {
//        require(flow is PersistentFlow)
//        with(flow) {
//            approvedInfos = approvedInfos.addNewInfo(variable, info)
//            if (previousFlow != null) {
//                approvedInfosDiff = approvedInfosDiff.addNewInfo(variable, info)
//            }
//            if (variable.isThisReference) {
//                processUpdatedReceiverVariable(flow, variable)
//            }
//        }
//    }
//
//    override fun addConditionalInfo(flow: Flow, variable: DataFlowVariable, info: ConditionalFirDataFlowInfo) {
//        require(flow is PersistentFlow)
//        with(flow) {
//            val existingInfo = conditionalInfos[variable]
//            conditionalInfos = if (existingInfo == null) {
//                conditionalInfos.put(variable, persistentListOf(info))
//            } else {
//                conditionalInfos.put(variable, existingInfo + info)
//            }
//        }
//    }
//
//    override val Flow.approvedInfos: MutableApprovedInfos
//        get() = throw IllegalStateException()
//
//    override val Flow.conditionalInfos: ConditionalInfos
//        get() = throw IllegalStateException()
//
//    private fun lowestCommonFlow(left: PersistentFlow, right: PersistentFlow): PersistentFlow {
//        val level = minOf(left.level, right.level)
//        @Suppress("NAME_SHADOWING")
//        var left = left
//        while (left.level > level) {
//            left = left.previousFlow!!
//        }
//        @Suppress("NAME_SHADOWING")
//        var right = right
//        while (right.level > level) {
//            right = right.previousFlow!!
//        }
//        while (left != right) {
//            left = left.previousFlow!!
//            right = right.previousFlow!!
//        }
//        return left
//    }
//}
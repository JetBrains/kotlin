/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import com.google.common.collect.Multimap
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.render

data class ConditionalFirDataFlowInfo(
    val condition: Condition,
    val variable: RealDataFlowVariable,
    val info: FirDataFlowInfo
) {
    fun invert(): ConditionalFirDataFlowInfo {
        return ConditionalFirDataFlowInfo(condition.invert(), variable, info)
    }

    override fun toString(): String {
        return "$condition -> $variable: ${info.exactType.render()}, ${info.exactNotType.render()}"
    }

    private fun Set<ConeKotlinType>.render(): String = joinToString { it.render() }
}

typealias ApprovedInfos = Map<RealDataFlowVariable, FirDataFlowInfo>
typealias MutableApprovedInfos = MutableMap<RealDataFlowVariable, MutableFirDataFlowInfo>
typealias ConditionalInfos = Multimap<DataFlowVariable, ConditionalFirDataFlowInfo>

interface FirDataFlowInfo {
    companion object {
        // TODO: temporary
        operator fun invoke(exactType: Set<ConeKotlinType>, exactNotType: Set<ConeKotlinType>): FirDataFlowInfo =
            MutableFirDataFlowInfo(exactType.toMutableSet(), exactNotType.toMutableSet())
    }

    val exactType: Set<ConeKotlinType>
    val exactNotType: Set<ConeKotlinType>
    operator fun plus(other: FirDataFlowInfo): FirDataFlowInfo
    operator fun minus(other: FirDataFlowInfo): FirDataFlowInfo
    val isNotEmpty: Boolean
    val isEmpty: Boolean get() = !isNotEmpty
    fun invert(): FirDataFlowInfo
}

data class MutableFirDataFlowInfo(
    override val exactType: MutableSet<ConeKotlinType> = mutableSetOf(),
    override val exactNotType: MutableSet<ConeKotlinType> = mutableSetOf()
) : FirDataFlowInfo {
    override operator fun plus(other: FirDataFlowInfo): MutableFirDataFlowInfo = MutableFirDataFlowInfo(
        HashSet(exactType).apply { addAll(other.exactType) },
        HashSet(exactNotType).apply { addAll(other.exactNotType) }
    )

    override operator fun minus(other: FirDataFlowInfo): MutableFirDataFlowInfo = MutableFirDataFlowInfo(
        HashSet(exactType).apply { removeAll(other.exactType) },
        HashSet(exactNotType).apply { removeAll(other.exactNotType) }
    )

    override val isNotEmpty: Boolean get() = exactType.isNotEmpty() || exactNotType.isNotEmpty()

    override fun invert(): FirDataFlowInfo = MutableFirDataFlowInfo(exactNotType, exactType)

    operator fun plusAssign(info: FirDataFlowInfo) {
        exactType += info.exactType
        exactNotType += info.exactNotType
    }

    fun copy(): MutableFirDataFlowInfo = MutableFirDataFlowInfo(exactType.toMutableSet(), exactNotType.toMutableSet())
}

operator fun FirDataFlowInfo.plus(other: FirDataFlowInfo?): FirDataFlowInfo = other?.let { this + other } ?: this

fun FirDataFlowInfo.toConditional(condition: Condition, variable: RealDataFlowVariable): ConditionalFirDataFlowInfo =
    ConditionalFirDataFlowInfo(condition, variable, this)

fun MutableApprovedInfos.addInfo(variable: RealDataFlowVariable, info: FirDataFlowInfo) {
    merge(variable, info as MutableFirDataFlowInfo) { existingInfo, newInfo ->
        existingInfo.apply { this += newInfo }
    }
}

fun MutableApprovedInfos.mergeInfo(other: Map<RealDataFlowVariable, FirDataFlowInfo>) {
    other.forEach { (variable, info) ->
        addInfo(variable, info)
    }
}
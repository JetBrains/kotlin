/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.render

data class UnapprovedFirDataFlowInfo(
    val condition: Condition,
    val variable: DataFlowVariable,
    val info: FirDataFlowInfo
) {
    fun invert(): UnapprovedFirDataFlowInfo {
        return UnapprovedFirDataFlowInfo(condition.invert(), variable, info)
    }

    override fun toString(): String {
        return "$condition -> $variable: ${info.exactType.render()}, ${info.exactNotType.render()}"
    }

    private fun Set<ConeKotlinType>.render(): String = joinToString { it.render() }
}

data class FirDataFlowInfo(
    val exactType: Set<ConeKotlinType>,
    val exactNotType: Set<ConeKotlinType>
) {
    operator fun plus(other: FirDataFlowInfo): FirDataFlowInfo = FirDataFlowInfo(
        exactType + other.exactType,
        exactNotType + other.exactNotType
    )

    operator fun minus(other: FirDataFlowInfo): FirDataFlowInfo = FirDataFlowInfo(
        exactType - other.exactType,
        exactNotType - other.exactNotType
    )

    val isNotEmpty: Boolean get() = exactType.isNotEmpty() || exactNotType.isNotEmpty()

    fun invert(): FirDataFlowInfo = FirDataFlowInfo(exactNotType, exactType)
}

operator fun FirDataFlowInfo.plus(other: FirDataFlowInfo?): FirDataFlowInfo = other?.let { this + other } ?: this
/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.types.model.TypeSystemCommonSuperTypesContext

interface DataFlowInferenceContext : TypeSystemCommonSuperTypesContext, ConeInferenceContext {
    fun myCommonSuperType(types: List<ConeKotlinType>): ConeKotlinType? {
        return when (types.size) {
            0 -> null
            1 -> types.first()
            else -> with(NewCommonSuperTypeCalculator) {
                commonSuperType(types) as ConeKotlinType
            }
        }
    }

    fun myIntersectTypes(types: List<ConeKotlinType>): ConeKotlinType? {
        return when (types.size) {
            0 -> null
            1 -> types.first()
            else -> ConeTypeIntersector.intersectTypes(this, types)
        }
    }

    fun or(infos: Collection<FirDataFlowInfo>): FirDataFlowInfo {
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
        myCommonSuperType(differentTypes.toList())?.let { commonTypes += it }
        return commonTypes
    }

    fun and(infos: Collection<FirDataFlowInfo>): FirDataFlowInfo {
        infos.singleOrNull()?.let { return it }
        val exactType = infos.flatMapTo(mutableSetOf()) { it.exactType }
        val exactNotType = infos.flatMapTo(mutableSetOf()) { it.exactNotType }
        return FirDataFlowInfo(exactType, exactNotType)
    }
}
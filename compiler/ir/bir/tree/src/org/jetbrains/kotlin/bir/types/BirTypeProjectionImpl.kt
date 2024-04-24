/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.types

import org.jetbrains.kotlin.bir.types.utils.buildTypeProjection
import org.jetbrains.kotlin.bir.types.utils.toBuilder
import org.jetbrains.kotlin.ir.types.IdSignatureValues
import org.jetbrains.kotlin.types.Variance

class BirTypeProjectionImpl internal constructor(
    override val type: BirType,
    override val variance: Variance,
) : BirTypeProjection {
    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is BirTypeProjectionImpl && type == other.type && variance == other.variance)

    override fun hashCode(): Int =
        type.hashCode() * 31 + variance.hashCode()
}

fun makeTypeProjection(type: BirType, variance: Variance): BirTypeProjection =
    when {
        type is BirCapturedType -> BirTypeProjectionImpl(type, variance)
        type is BirTypeProjection && type.variance == variance -> type
        type is BirSimpleType -> type.toBuilder().buildTypeProjection(variance)
        type is BirDynamicType -> BirDynamicType(null, type.annotations, variance)
        type is BirErrorType -> BirErrorType(null, type.annotations, variance)
        type is BirTypeProjection -> BirTypeProjectionImpl(type.type, variance)
        else -> BirTypeProjectionImpl(type, variance)
    }

fun makeTypeIntersection(types: Collection<BirType>): BirType =
    with(types.map { makeTypeProjection(it, Variance.INVARIANT).type }.distinct()) {
        if (size == 1) return single()
        else firstOrNull { !it.isClassType(IdSignatureValues.any) } ?: first { it.isNotNullClassType((IdSignatureValues.any)) }
    }

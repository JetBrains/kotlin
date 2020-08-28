/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

val ConeKotlinType.isNullable: Boolean get() = nullability != ConeNullability.NOT_NULL

val ConeKotlinType.isMarkedNullable: Boolean get() = nullability == ConeNullability.NULLABLE

val ConeKotlinType.classId: ClassId? get() = this.safeAs<ConeClassLikeType>()?.lookupTag?.classId

fun ConeKotlinType.contains(predicate: (ConeKotlinType) -> Boolean): Boolean {
    return contains(predicate, null)
}

private fun ConeKotlinType.contains(predicate: (ConeKotlinType) -> Boolean, visited: SmartSet<ConeKotlinType>?): Boolean {
    if (visited?.contains(this) == true) return false
    if (predicate(this)) return true

    @Suppress("NAME_SHADOWING")
    val visited = visited ?: SmartSet.create()
    visited += this

    return when (this) {
        is ConeFlexibleType -> lowerBound.contains(predicate, visited) || upperBound.contains(predicate, visited)
        is ConeDefinitelyNotNullType -> original.contains(predicate, visited)
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { it is ConeKotlinTypeProjection && it.type.contains(predicate, visited) }
    }
}

fun ConeClassLikeType.withArguments(typeArguments: Array<out ConeTypeProjection>): ConeClassLikeType = when (this) {
    is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable, attributes)
    is ConeClassErrorType -> this
    else -> error("Unknown cone type: ${this::class}")
}

fun ConeKotlinType.toTypeProjection(variance: Variance): ConeTypeProjection =
    when (variance) {
        Variance.INVARIANT -> this
        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(this)
        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(this)
    }

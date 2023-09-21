/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.renderer.*
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartSet

val ConeKotlinType.isNullable: Boolean get() = nullability != ConeNullability.NOT_NULL
val ConeKotlinType.isMarkedNullable: Boolean get() = nullability == ConeNullability.NULLABLE

val ConeKotlinType.classId: ClassId? get() = (this as? ConeClassLikeType)?.lookupTag?.classId

/**
 * Recursively visits each [ConeKotlinType] inside (including itself) and performs the given action.
 */
fun ConeKotlinType.forEachType(action: (ConeKotlinType) -> Unit) {
    action(this)

    return when (this) {
        is ConeFlexibleType -> {
            lowerBound.forEachType(action)
            upperBound.forEachType(action)
        }

        is ConeDefinitelyNotNullType -> original.forEachType(action)
        is ConeIntersectionType -> intersectedTypes.forEach { it.forEachType(action) }
        else -> typeArguments.forEach { if (it is ConeKotlinTypeProjection) it.type.forEachType(action) }
    }
}

fun ConeKotlinType.contains(predicate: (ConeKotlinType) -> Boolean): Boolean {
    return contains(predicate, SmartSet.create())
}

private fun ConeKotlinType.contains(predicate: (ConeKotlinType) -> Boolean, visited: SmartSet<ConeKotlinType>): Boolean {
    if (this in visited) return false
    if (predicate(this)) return true
    visited += this

    return when (this) {
        is ConeFlexibleType -> lowerBound.contains(predicate, visited) || upperBound.contains(predicate, visited)
        is ConeDefinitelyNotNullType -> original.contains(predicate, visited)
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { it is ConeKotlinTypeProjection && it.type.contains(predicate, visited) }
    }
}

// ----------------------------------- Transformations -----------------------------------

fun ConeKotlinType.upperBoundIfFlexible(): ConeSimpleKotlinType {
    return when (this) {
        is ConeSimpleKotlinType -> this
        is ConeFlexibleType -> upperBound
    }
}

fun ConeKotlinType.lowerBoundIfFlexible(): ConeSimpleKotlinType {
    return when (this) {
        is ConeSimpleKotlinType -> this
        is ConeFlexibleType -> lowerBound
    }
}

fun ConeKotlinType.originalIfDefinitelyNotNullable(): ConeKotlinType {
    return when (this) {
        is ConeDefinitelyNotNullType -> original
        else -> this
    }
}

fun ConeIntersectionType.withAlternative(alternativeType: ConeKotlinType): ConeIntersectionType {
    return ConeIntersectionType(intersectedTypes, alternativeType)
}

fun ConeIntersectionType.mapTypes(func: (ConeKotlinType) -> ConeKotlinType): ConeIntersectionType {
    return ConeIntersectionType(intersectedTypes.map(func), alternativeType?.let(func))
}

fun ConeClassLikeType.withArguments(typeArguments: Array<out ConeTypeProjection>): ConeClassLikeType = when (this) {
    is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable, attributes)
    is ConeErrorType -> this
    else -> error("Unknown cone type: ${this::class}")
}

fun ConeKotlinType.toTypeProjection(variance: Variance): ConeTypeProjection =
    when (variance) {
        Variance.INVARIANT -> this
        Variance.IN_VARIANCE -> ConeKotlinTypeProjectionIn(this)
        Variance.OUT_VARIANCE -> ConeKotlinTypeProjectionOut(this)
    }

fun ConeKotlinType.toTypeProjection(projectionKind: ProjectionKind): ConeTypeProjection {
    return when (projectionKind) {
        ProjectionKind.INVARIANT -> this
        ProjectionKind.IN -> ConeKotlinTypeProjectionIn(this)
        ProjectionKind.OUT -> ConeKotlinTypeProjectionOut(this)
        ProjectionKind.STAR -> ConeStarProjection
    }
}

fun ConeClassLikeType.replaceArgumentsWithStarProjections(): ConeClassLikeType {
    if (typeArguments.isEmpty()) return this
    val newArguments = Array(typeArguments.size) { ConeStarProjection }
    return withArguments(newArguments)
}

fun ConeKotlinType.renderForDebugging(): String {
    val builder = StringBuilder()
    ConeTypeRendererForDebugging(builder).render(this)
    return builder.toString()
}

fun ConeKotlinType.renderReadable(): String {
    val builder = StringBuilder()
    ConeTypeRendererForReadability(builder) { ConeIdShortRenderer() }.render(this)
    return builder.toString()
}

fun ConeKotlinType.renderReadableWithFqNames(): String {
    val builder = StringBuilder()
    ConeTypeRendererForReadability(builder) { ConeIdRendererForDiagnostics() }.render(this)
    return builder.toString()
}

fun ConeKotlinType.hasError(): Boolean = contains { it is ConeErrorType }

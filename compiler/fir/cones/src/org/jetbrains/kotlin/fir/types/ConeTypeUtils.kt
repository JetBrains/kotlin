/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.renderer.ConeIdRendererForDiagnostics
import org.jetbrains.kotlin.fir.renderer.ConeIdShortRenderer
import org.jetbrains.kotlin.fir.renderer.ConeTypeRendererForDebugging
import org.jetbrains.kotlin.fir.renderer.ConeTypeRendererForReadability
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import org.jetbrains.kotlin.utils.SmartSet
import org.jetbrains.kotlin.utils.addToStdlib.popLast

/**
 * Returns `true` if the type is flexible and either bound [isMarkedNullable] or if the type itself [isMarkedNullable].
 */
val ConeKotlinType.isMarkedOrFlexiblyNullable: Boolean
    get() = when (this) {
        is ConeFlexibleType -> upperBound.isMarkedNullable
        is ConeRigidType -> isMarkedNullable
    }

@Deprecated(
    "`isMarkedOrFlexiblyNullable` on non-flexible types is the same as `isMarkedNullable`. Also consider using `canBeNull()`.",
    level = DeprecationLevel.ERROR
)
val ConeRigidType.isMarkedOrFlexiblyNullable: Boolean get() = isMarkedNullable

/**
 * Returns `true` if the type is marked as nullable.
 *
 * Note that a return value of `true` implies that this type can be `null`, however, the inverse isn't true.
 *
 * A type resolving to a typealias not marked as nullable can contain `null` if the typealias expands to a nullable type.
 * A type parameter type not marked as nullable can contain `null` if the type has a nullable upper bound.
 *
 * For a comprehensive check if a type can be `null`, consider using `canBeNull()`.
 */
val ConeKotlinType.isMarkedNullable: Boolean
    get() = when (this) {
        is ConeLookupTagBasedType -> isMarkedNullable
        is ConeFlexibleType -> lowerBound.isMarkedNullable && upperBound.isMarkedNullable
        is ConeCapturedType -> isMarkedNullable
        is ConeIntegerLiteralType -> isMarkedNullable
        is ConeTypeVariableType -> isMarkedNullable
        is ConeDefinitelyNotNullType -> false
        is ConeIntersectionType -> false
        is ConeStubType -> isMarkedNullable
    }

val ConeKotlinType.hasFlexibleMarkedNullability: Boolean
    get() = this is ConeFlexibleType && lowerBound.isMarkedNullable != upperBound.isMarkedNullable

val ConeKotlinType.classId: ClassId? get() = (this.lowerBoundIfFlexible() as? ConeClassLikeType)?.lookupTag?.classId

val ConeKotlinType.lookupTagIfAny: ConeClassifierLookupTag?
    get() = (this.lowerBoundIfFlexible() as? ConeLookupTagBasedType)?.lookupTag

val ConeKotlinType.classLikeLookupTagIfAny: ConeClassLikeLookupTag?
    get() = (this.lowerBoundIfFlexible() as? ConeClassLikeType)?.lookupTag

/**
 * Recursively visits each [ConeKotlinType] inside (including itself) and performs the given action.
 * Doesn't give guarantees on the traversal order.
 */
inline fun ConeKotlinType.forEachType(
    prepareType: (ConeKotlinType) -> ConeKotlinType = { it },
    action: (ConeKotlinType) -> Unit,
) {
    val stack = mutableListOf(this)

    while (stack.isNotEmpty()) {
        val next = stack.popLast().let(prepareType)
        action(next)

        when (next) {
            is ConeFlexibleType -> {
                stack.add(next.lowerBound)
                if (!next.isTrivial) {
                    stack.add(next.upperBound)
                }
            }

            is ConeDefinitelyNotNullType -> stack.add(next.original)
            is ConeIntersectionType -> stack.addAll(next.intersectedTypes)
            else -> next.typeArguments.forEach { if (it is ConeKotlinTypeProjection) stack.add(it.type) }
        }
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
        is ConeFlexibleType -> lowerBound.contains(predicate, visited) || !isTrivial && upperBound.contains(predicate, visited)
        is ConeDefinitelyNotNullType -> original.contains(predicate, visited)
        is ConeIntersectionType -> intersectedTypes.any { it.contains(predicate, visited) }
        else -> typeArguments.any { it is ConeKotlinTypeProjection && it.type.contains(predicate, visited) }
    }
}

// ----------------------------------- Transformations -----------------------------------

fun ConeKotlinType.unwrapLowerBound(): ConeSimpleKotlinType {
    return when (this) {
        is ConeDefinitelyNotNullType -> original.unwrapLowerBound()
        is ConeFlexibleType -> lowerBound.unwrapLowerBound()
        is ConeSimpleKotlinType -> this
    }
}

fun ConeKotlinType.upperBoundIfFlexible(): ConeRigidType {
    return when (this) {
        is ConeSimpleKotlinType -> this
        is ConeFlexibleType -> upperBound
        is ConeDefinitelyNotNullType -> this
    }
}

fun ConeKotlinType.lowerBoundIfFlexible(): ConeRigidType {
    return when (this) {
        is ConeSimpleKotlinType -> this
        is ConeFlexibleType -> lowerBound
        is ConeDefinitelyNotNullType -> this
    }
}

fun ConeIntersectionType.withUpperBound(upperBound: ConeKotlinType): ConeIntersectionType {
    return ConeIntersectionType(intersectedTypes, upperBoundForApproximation = upperBound)
}

inline fun ConeIntersectionType.mapTypes(func: (ConeKotlinType) -> ConeKotlinType): ConeIntersectionType {
    return ConeIntersectionType(intersectedTypes.map(func), upperBoundForApproximation?.let(func))
}

fun ConeClassLikeType.withArguments(typeArguments: Array<out ConeTypeProjection>): ConeClassLikeType = when (this) {
    is ConeClassLikeTypeImpl -> ConeClassLikeTypeImpl(lookupTag, typeArguments, isMarkedNullable, attributes)
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

fun ConeKotlinType.renderReadableWithFqNames(preRenderedConstructors: Map<TypeConstructorMarker, String>? = null): String {
    val builder = StringBuilder()
    ConeTypeRendererForReadability(builder, preRenderedConstructors) { ConeIdRendererForDiagnostics() }.render(this)
    return builder.toString()
}

fun ConeKotlinType.hasError(): Boolean = contains { it is ConeErrorType }

fun ConeKotlinType.hasCapture(): Boolean = contains { it is ConeCapturedType }

fun ConeRigidType.getConstructor(): TypeConstructorMarker {
    return when (this) {
        is ConeLookupTagBasedType -> this.lookupTag
        is ConeCapturedType -> this.constructor
        is ConeTypeVariableType -> this.typeConstructor
        is ConeIntersectionType -> this
        is ConeStubType -> this.constructor
        is ConeDefinitelyNotNullType -> original.getConstructor()
        is ConeIntegerLiteralType -> this
    }
}

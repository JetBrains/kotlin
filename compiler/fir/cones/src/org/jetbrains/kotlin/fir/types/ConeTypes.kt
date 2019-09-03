/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.*

sealed class ConeKotlinTypeProjection : TypeArgumentMarker {
    abstract val kind: ProjectionKind

    companion object {
        val EMPTY_ARRAY = arrayOf<ConeKotlinTypeProjection>()
    }
}

enum class ProjectionKind {
    STAR, IN, OUT, INVARIANT
}

object ConeStarProjection : ConeKotlinTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.STAR
}

interface ConeTypedProjection {
    val type: ConeKotlinType
}

data class ConeKotlinTypeProjectionIn(override val type: ConeKotlinType) : ConeKotlinTypeProjection(), ConeTypedProjection {
    override val kind: ProjectionKind
        get() = ProjectionKind.IN
}

data class ConeKotlinTypeProjectionOut(override val type: ConeKotlinType) : ConeKotlinTypeProjection(), ConeTypedProjection {
    override val kind: ProjectionKind
        get() = ProjectionKind.OUT
}

enum class ConeNullability(val suffix: String) {
    NULLABLE("?"),
    UNKNOWN("!"),
    NOT_NULL("");

    val isNullable: Boolean get() = this != NOT_NULL

    companion object {
        fun create(isNullable: Boolean) = if (isNullable) NULLABLE else NOT_NULL
    }
}

// We assume type IS an invariant type projection to prevent additional wrapper here
// (more exactly, invariant type projection contains type)
sealed class ConeKotlinType : ConeKotlinTypeProjection(), ConeTypedProjection, KotlinTypeMarker, TypeArgumentListMarker {
    override val kind: ProjectionKind
        get() = ProjectionKind.INVARIANT

    abstract val typeArguments: Array<out ConeKotlinTypeProjection>

    override val type: ConeKotlinType
        get() = this

    abstract val nullability: ConeNullability

    override fun toString(): String {
        return render()
    }
}

val ConeKotlinType.isNullable: Boolean get() = nullability != ConeNullability.NOT_NULL

val ConeKotlinType.isMarkedNullable: Boolean get() = nullability == ConeNullability.NULLABLE

typealias ConeKotlinErrorType = ConeClassErrorType

class ConeClassErrorType(val reason: String) : ConeClassLikeType() {
    override val lookupTag: ConeClassLikeLookupTag
        get() = ConeClassLikeLookupTagImpl(ClassId.fromString("<error>"))

    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = EMPTY_ARRAY

    override val nullability: ConeNullability
        get() = ConeNullability.UNKNOWN

    override fun toString(): String {
        return "<ERROR CLASS: $reason>"
    }
}

sealed class ConeLookupTagBasedType : ConeKotlinType(), SimpleTypeMarker {
    abstract val lookupTag: ConeClassifierLookupTag
}

sealed class ConeClassLikeType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeClassLikeLookupTag
}

abstract class ConeClassType : ConeClassLikeType()

abstract class ConeAbbreviatedType : ConeClassLikeType() {
    abstract val abbreviationLookupTag: ConeClassLikeLookupTag
}

abstract class ConeTypeParameterType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeTypeParameterLookupTag
}

data class ConeFlexibleType(val lowerBound: ConeKotlinType, val upperBound: ConeKotlinType) : ConeKotlinType(),
    FlexibleTypeMarker {

    init {
        val message = { "Bounds violation: $lowerBound, $upperBound" }
        require(lowerBound is SimpleTypeMarker, message)
        require(upperBound is SimpleTypeMarker, message)
    }

    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = emptyArray()

    override val nullability: ConeNullability
        get() = lowerBound.nullability.takeIf { it == upperBound.nullability } ?: ConeNullability.UNKNOWN
}

fun ConeKotlinType.upperBoundIfFlexible() = (this as? ConeFlexibleType)?.upperBound ?: this
fun ConeKotlinType.lowerBoundIfFlexible() = (this as? ConeFlexibleType)?.lowerBound ?: this

class ConeCapturedTypeConstructor(
    val projection: ConeKotlinTypeProjection,
    var supertypes: List<ConeKotlinType>? = null
) : TypeConstructorMarker

class ConeCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: ConeKotlinType?,
    override val nullability: ConeNullability = ConeNullability.NOT_NULL,
    val constructor: ConeCapturedTypeConstructor
) : ConeKotlinType(), SimpleTypeMarker, CapturedTypeMarker {
    constructor(captureStatus: CaptureStatus, lowerType: ConeKotlinType?, projection: ConeKotlinTypeProjection) : this(
        captureStatus,
        lowerType,
        constructor = ConeCapturedTypeConstructor(
            projection
        )
    )

    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = emptyArray()
}

class ConeTypeVariableType(
    override val nullability: ConeNullability,
    override val lookupTag: ConeClassifierLookupTag
) : ConeLookupTagBasedType() {
    override val typeArguments: Array<out ConeKotlinTypeProjection> get() = emptyArray()
}

class ConeDefinitelyNotNullType(val original: ConeKotlinType): ConeKotlinType(), DefinitelyNotNullTypeMarker {
    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = original.typeArguments
    override val nullability: ConeNullability
        get() = ConeNullability.NOT_NULL
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.symbols.*
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

class ConeKotlinTypeProjectionIn(override val type: ConeKotlinType) : ConeKotlinTypeProjection(), ConeTypedProjection {
    override val kind: ProjectionKind
        get() = ProjectionKind.IN
}

class ConeKotlinTypeProjectionOut(override val type: ConeKotlinType) : ConeKotlinTypeProjection(), ConeTypedProjection {
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
}

val ConeKotlinType.isNullable: Boolean get() = nullability != ConeNullability.NOT_NULL

val ConeKotlinType.isMarkedNullable: Boolean get() = nullability == ConeNullability.NULLABLE

class ConeKotlinErrorType(val reason: String) : ConeKotlinType() {
    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = EMPTY_ARRAY

    override val nullability: ConeNullability
        get() = ConeNullability.UNKNOWN

    override fun toString(): String {
        return "<ERROR TYPE: $reason>"
    }
}

class ConeClassErrorType(val reason: String) : ConeClassLikeType() {
    override val lookupTag: ConeClassLikeLookupTag
        get() = error("!")

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


abstract class ConeFunctionType : ConeClassLikeType() {

    abstract override val lookupTag: ConeClassLikeLookupTag
    abstract val receiverType: ConeKotlinType?
    abstract val parameterTypes: List<ConeKotlinType>
    abstract val returnType: ConeKotlinType
}

class ConeFlexibleType(val lowerBound: ConeLookupTagBasedType, val upperBound: ConeLookupTagBasedType) : ConeKotlinType(), FlexibleTypeMarker {
    override val typeArguments: Array<out ConeKotlinTypeProjection>
        get() = emptyArray()

    override val nullability: ConeNullability
        get() = lowerBound.nullability.takeIf { it == upperBound.nullability } ?: ConeNullability.UNKNOWN
}

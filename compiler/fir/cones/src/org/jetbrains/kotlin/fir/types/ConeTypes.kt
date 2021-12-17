/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.foldMap

// We assume type IS an invariant type projection to prevent additional wrapper here
// (more exactly, invariant type projection contains type)
sealed class ConeKotlinType : ConeKotlinTypeProjection(), KotlinTypeMarker, TypeArgumentListMarker {
    final override val kind: ProjectionKind
        get() = ProjectionKind.INVARIANT

    abstract val typeArguments: Array<out ConeTypeProjection>

    final override val type: ConeKotlinType
        get() = this

    abstract val nullability: ConeNullability

    abstract val attributes: ConeAttributes

    final override fun toString(): String {
        return render()
    }

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

sealed class ConeSimpleKotlinType : ConeKotlinType(), SimpleTypeMarker

class ConeClassLikeErrorLookupTag(override val classId: ClassId) : ConeClassLikeLookupTag()

class ConeErrorType(val diagnostic: ConeDiagnostic, val isUninferredParameter: Boolean = false) : ConeClassLikeType() {
    override val lookupTag: ConeClassLikeLookupTag
        get() = ConeClassLikeErrorLookupTag(ClassId.fromString("<error>"))

    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override val nullability: ConeNullability
        get() = ConeNullability.UNKNOWN

    override val attributes: ConeAttributes
        get() = ConeAttributes.Empty

    override fun equals(other: Any?) = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

abstract class ConeLookupTagBasedType : ConeSimpleKotlinType() {
    abstract val lookupTag: ConeClassifierLookupTag
}

abstract class ConeClassLikeType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeClassLikeLookupTag
}

open class ConeFlexibleType(
    val lowerBound: ConeSimpleKotlinType,
    val upperBound: ConeSimpleKotlinType
) : ConeKotlinType(), FlexibleTypeMarker {

    final override val typeArguments: Array<out ConeTypeProjection>
        get() = lowerBound.typeArguments

    final override val nullability: ConeNullability
        get() = lowerBound.nullability.takeIf { it == upperBound.nullability } ?: ConeNullability.UNKNOWN

    final override val attributes: ConeAttributes
        get() = lowerBound.attributes

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // I suppose dynamic type (see below) and flexible type should use the same equals
        if (other !is ConeFlexibleType) return false

        if (lowerBound != other.lowerBound) return false
        if (upperBound != other.upperBound) return false

        return true
    }

    final override fun hashCode(): Int {
        var result = lowerBound.hashCode()
        result = 31 * result + upperBound.hashCode()
        return result
    }
}

@RequiresOptIn(message = "Please use ConeDynamicType.create instead")
annotation class DynamicTypeConstructor

class ConeDynamicType @DynamicTypeConstructor constructor(
    lowerBound: ConeSimpleKotlinType,
    upperBound: ConeSimpleKotlinType
) : ConeFlexibleType(lowerBound, upperBound), DynamicTypeMarker {
    companion object
}

fun ConeSimpleKotlinType.unwrapDefinitelyNotNull(): ConeSimpleKotlinType {
    return when (this) {
        is ConeDefinitelyNotNullType -> original
        else -> this
    }
}

class ConeCapturedTypeConstructor(
    val projection: ConeTypeProjection,
    var supertypes: List<ConeKotlinType>? = null,
    val typeParameterMarker: TypeParameterMarker? = null
) : CapturedTypeConstructorMarker

data class ConeCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: ConeKotlinType?,
    override val nullability: ConeNullability = ConeNullability.NOT_NULL,
    val constructor: ConeCapturedTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
    val isProjectionNotNull: Boolean = false
) : ConeSimpleKotlinType(), CapturedTypeMarker {
    constructor(
        captureStatus: CaptureStatus, lowerType: ConeKotlinType?, projection: ConeTypeProjection,
        typeParameterMarker: TypeParameterMarker
    ) : this(
        captureStatus,
        lowerType,
        constructor = ConeCapturedTypeConstructor(
            projection,
            typeParameterMarker = typeParameterMarker
        )
    )

    override val typeArguments: Array<out ConeTypeProjection>
        get() = emptyArray()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeCapturedType

        if (lowerType != other.lowerType) return false
        if (constructor.projection != other.constructor.projection) return false
        if (constructor.typeParameterMarker != other.constructor.typeParameterMarker) return false
        if (captureStatus != other.captureStatus) return false
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + (lowerType?.hashCode() ?: 0)
        result = 31 * result + constructor.projection.hashCode()
        result = 31 * result + constructor.typeParameterMarker.hashCode()
        result = 31 * result + captureStatus.hashCode()
        result = 31 * result + nullability.hashCode()
        return result
    }
}


data class ConeDefinitelyNotNullType(val original: ConeSimpleKotlinType) : ConeSimpleKotlinType(), DefinitelyNotNullTypeMarker {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = original.typeArguments

    override val nullability: ConeNullability
        get() = ConeNullability.NOT_NULL

    override val attributes: ConeAttributes
        get() = original.attributes

    companion object
}

class ConeRawType(
    lowerBound: ConeSimpleKotlinType,
    upperBound: ConeSimpleKotlinType
) : ConeFlexibleType(lowerBound, upperBound), RawTypeMarker

/*
 * Contract of the intersection type: it is flat. It means that
 *   intersection type can not contains another intersection types
 *   inside it. To keep this contract construct new intersection types
 *   only via ConeTypeIntersector
 */
class ConeIntersectionType(
    val intersectedTypes: Collection<ConeKotlinType>,
    val alternativeType: ConeKotlinType? = null,
) : ConeSimpleKotlinType(), IntersectionTypeConstructorMarker {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = emptyArray()

    override val nullability: ConeNullability
        get() = ConeNullability.NOT_NULL

    override val attributes: ConeAttributes = intersectedTypes.foldMap(
        { it.attributes },
        { a, b -> a.intersect(b) }
    )

    private var hashCode = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeIntersectionType

        if (intersectedTypes != other.intersectedTypes) return false

        return true
    }

    override fun hashCode(): Int {
        if (hashCode != 0) return hashCode
        return intersectedTypes.hashCode().also { hashCode = it }
    }
}

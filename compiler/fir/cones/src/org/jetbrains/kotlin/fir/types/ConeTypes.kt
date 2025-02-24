/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnosticWithNullability
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.foldMap

// We assume type IS an invariant type projection to prevent additional wrapper here
// (more exactly, invariant type projection contains type)
sealed class ConeKotlinType : ConeKotlinTypeProjection(), KotlinTypeMarker, TypeArgumentListMarker {
    final override val kind: ProjectionKind
        get() = ProjectionKind.INVARIANT

    /**
     * When this is a [ConeClassLikeType], returns the type arguments of this type.
     *
     * When this is a [ConeFlexibleType], returns the type arguments of the [ConeFlexibleType.lowerBound].
     *
     * In all other cases, returns an empty array.
     */
    abstract val typeArguments: Array<out ConeTypeProjection>

    @Deprecated("Useless call. Receiver is already a ConeKotlinType.", level = DeprecationLevel.ERROR)
    final override val type: ConeKotlinType
        get() = this

    abstract val attributes: ConeAttributes

    final override fun toString(): String {
        return renderForDebugging()
    }

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}

/**
 * Normally should represent a type with one related constructor, see [getConstructor],
 * but still can require unwrapping, as [ConeDefinitelyNotNullType].
 *
 * Known properties of [ConeRigidType] are:
 * - it does not have bounds as [ConeFlexibleType]
 * - it has one related constructor. [ConeIntersectionType] is currently an exception, see [KT-70049](https://youtrack.jetbrains.com/issue/KT-70049).
 * - it can require unwrapping
 *
 */
sealed class ConeRigidType : ConeKotlinType(), RigidTypeMarker

/**
 * Normally should represent a type with one related constructor that does not require unwrapping.
 */
sealed class ConeSimpleKotlinType : ConeRigidType(), SimpleTypeMarker

class ConeClassLikeErrorLookupTag(override val classId: ClassId) : ConeClassLikeLookupTag()

class ConeErrorType(
    val diagnostic: ConeDiagnostic,
    val isUninferredParameter: Boolean = false,
    val delegatedType: ConeKotlinType? = null,
    override val typeArguments: Array<out ConeTypeProjection> = EMPTY_ARRAY,
    override val attributes: ConeAttributes = ConeAttributes.Empty
) : ConeClassLikeType() {
    override val lookupTag: ConeClassLikeLookupTag
        get() = ConeClassLikeErrorLookupTag(ClassId.fromString("<error>"))

    override val isMarkedNullable: Boolean
        get() = (diagnostic as? ConeDiagnosticWithNullability)?.isNullable == true

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

abstract class ConeLookupTagBasedType : ConeSimpleKotlinType() {
    abstract val lookupTag: ConeClassifierLookupTag
    abstract val isMarkedNullable: Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeLookupTagBasedType) return false

        if (lookupTag != other.lookupTag) return false
        if (!typeArguments.contentEquals(other.typeArguments)) return false
        if (isMarkedNullable != other.isMarkedNullable) return false
        if (attributes definitelyDifferFrom other.attributes) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lookupTag.hashCode()
        result = 31 * result + typeArguments.contentHashCode()
        result = 31 * result + isMarkedNullable.hashCode()
        return result
    }
}

abstract class ConeClassLikeType : ConeLookupTagBasedType() {
    abstract override val lookupTag: ConeClassLikeLookupTag
}

/**
 * Represents types with two bounds. [lowerBound] must be a subtype of [upperBound].
 */
open class ConeFlexibleType(
    val lowerBound: ConeRigidType,
    val upperBound: ConeRigidType,
    /**
     * If `true`, the upper bound is a trivial, nullable copy of the lower bound.
     *
     * This flag is purely for optimization purposes.
     * Callers should check this flag when they need to iterate all nested types because when it's `true`,
     * the type arguments of [lowerBound] and [upperBound] are guaranteed to be the same,
     * and therefore, iterating them once is enough.
     * This prevents an exponential performance on such operations on deeply nested flexible types.
     */
    val isTrivial: Boolean,
) : ConeKotlinType(), FlexibleTypeMarker {
    final override val typeArguments: Array<out ConeTypeProjection>
        get() = lowerBound.typeArguments

    final override val attributes: ConeAttributes
        get() = lowerBound.attributes

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        // I suppose dynamic type (see below) and flexible type should use the same equals,
        // because ft<Any?, Nothing> should never be created
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
    lowerBound: ConeRigidType,
    upperBound: ConeRigidType
) : ConeFlexibleType(lowerBound, upperBound, isTrivial = false), DynamicTypeMarker {
    companion object
}

private fun ConeRigidType.unwrapDefinitelyNotNull(): ConeSimpleKotlinType {
    return when (this) {
        is ConeDefinitelyNotNullType -> original
        is ConeSimpleKotlinType -> this
    }
}

fun ConeKotlinType.unwrapToSimpleTypeUsingLowerBound(): ConeSimpleKotlinType {
    return lowerBoundIfFlexible().unwrapDefinitelyNotNull()
}

sealed interface ConeTypeConstructorMarker : TypeConstructorMarker

class ConeCapturedTypeConstructor(
    val projection: ConeTypeProjection,
    var supertypes: List<ConeKotlinType>? = null,
    val typeParameterMarker: TypeParameterMarker? = null
) : CapturedTypeConstructorMarker, ConeTypeConstructorMarker

data class ConeCapturedType(
    val captureStatus: CaptureStatus,
    val lowerType: ConeKotlinType?,
    val isMarkedNullable: Boolean = false,
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
        get() = EMPTY_ARRAY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeCapturedType

        if (lowerType != other.lowerType) return false
        if (constructor != other.constructor) return false
        if (captureStatus != other.captureStatus) return false
        if (isMarkedNullable != other.isMarkedNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 7
        result = 31 * result + (lowerType?.hashCode() ?: 0)
        result = 31 * result + constructor.hashCode()
        result = 31 * result + captureStatus.hashCode()
        result = 31 * result + isMarkedNullable.hashCode()
        return result
    }
}

/**
 * Types of this kind are represented as (Type & Any).
 *
 * @param original the base type for being DNN. It can be [ConeTypeVariableType], ConeTypeParameterType or [ConeCapturedType].
 */
data class ConeDefinitelyNotNullType(
    val original: ConeSimpleKotlinType
) : ConeRigidType(), DefinitelyNotNullTypeMarker {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override val attributes: ConeAttributes
        get() = original.attributes

    companion object
}

class ConeRawType private constructor(
    lowerBound: ConeRigidType,
    upperBound: ConeRigidType
) : ConeFlexibleType(lowerBound, upperBound, isTrivial = false) {
    companion object {
        fun create(
            lowerBound: ConeRigidType,
            upperBound: ConeRigidType,
        ): ConeRawType {
            require(lowerBound is ConeClassLikeType && upperBound is ConeClassLikeType) {
                "Raw bounds are expected to be class-like types, but $lowerBound and $upperBound were found"
            }

            val lowerBoundToUse = if (!lowerBound.attributes.contains(CompilerConeAttributes.RawType)) {
                ConeClassLikeTypeImpl(
                    lowerBound.lookupTag, lowerBound.typeArguments, lowerBound.isMarkedNullable,
                    lowerBound.attributes.add(CompilerConeAttributes.RawType)
                )
            } else {
                lowerBound
            }

            return ConeRawType(lowerBoundToUse, upperBound)
        }
    }
}

/**
 * This class represents so-called intersection type like T1&T2&T3 [intersectedTypes] = listOf(T1, T2, T3).
 *
 * Contract of the intersection type: it is flat. It means that an intersection type can not contain another intersection type inside it.
 * To comply with this contract, construct new intersection types only via [org.jetbrains.kotlin.fir.types.ConeTypeIntersector].
 *
 * Except for T&Any types, [org.jetbrains.kotlin.fir.types.ConeIntersectionType] is non-denotable.
 * Moreover, it does not have an IR counterpart.
 * This means that approximation is often required, and normally a common supertype of [intersectedTypes] is used for this purpose.
 * In a situation with constraints like A <: T, B <: T, T <: C, [org.jetbrains.kotlin.fir.types.ConeIntersectionType]
 * and commonSupertype(A, B) </: C, an intersection type A&B is created,
 * C is stored as [upperBoundForApproximation] and used when approximation is needed.
 * Without it, we can violate a constraint system while doing intersection type approximation.
 * See also [org.jetbrains.kotlin.resolve.calls.inference.components.ResultTypeResolver.specialResultForIntersectionType]
 *
 * @param intersectedTypes collection of types to be intersected. None of them is allowed to be another intersection type.
 * @param upperBoundForApproximation a super-type (upper bound), if it's known, to be used as an approximation.
 */
class ConeIntersectionType(
    val intersectedTypes: Collection<ConeKotlinType>,
    val upperBoundForApproximation: ConeKotlinType? = null,
) : ConeSimpleKotlinType(), IntersectionTypeConstructorMarker, ConeTypeConstructorMarker {
    // TODO: consider inheriting directly from ConeKotlinType (KT-70049)
    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

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

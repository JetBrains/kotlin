/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
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
 * Make a transformation from marker interface to cone-based type
 *
 * In K2 frontend context such a transformation is normally safe,
 * as K1-based types and IR-based types cannot occur here.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun KotlinTypeMarker.asCone(): ConeKotlinType = this as ConeKotlinType

@Deprecated(message = "This call is redundant, please just drop it", level = DeprecationLevel.ERROR)
fun ConeKotlinType.asCone(): ConeKotlinType = this

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
 * Make a transformation from marker interface to cone-based type
 *
 * In K2 frontend context such a transformation is normally safe,
 * as K1-based types and IR-based types cannot occur here.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun RigidTypeMarker.asCone(): ConeRigidType = this as ConeRigidType

/**
 * Normally should represent a type with one related constructor that does not require unwrapping.
 */
sealed class ConeSimpleKotlinType : ConeRigidType(), SimpleTypeMarker

class ConeClassLikeErrorLookupTag(
    override val classId: ClassId,
    val diagnostic: ConeDiagnostic,
    /**
     * A type the error type is somehow related to, e.g., a type parameter type that is uninferred.
     */
    val delegatedType: ConeKotlinType? = null,
) : ConeClassLikeLookupTag()

class ConeErrorType(
    diagnostic: ConeDiagnostic,
    val isUninferredParameter: Boolean = false,
    delegatedType: ConeKotlinType? = null,
    override val typeArguments: Array<out ConeTypeProjection> = EMPTY_ARRAY,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
    val nullable: Boolean? = null,
    override val lookupTag: ConeClassLikeErrorLookupTag =
        ConeClassLikeErrorLookupTag(delegatedType?.classId ?: ClassId.fromString("<error>"), diagnostic, delegatedType)
) : ConeClassLikeType() {
    override val isMarkedNullable: Boolean
        get() = nullable == true
    val diagnostic: ConeDiagnostic get() = lookupTag.diagnostic
    val delegatedType: ConeKotlinType? get() = lookupTag.delegatedType

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

        if (isTrivial && other.isTrivial) return true

        if (upperBound != other.upperBound) return false

        return true
    }

    final override fun hashCode(): Int {
        var result = lowerBound.hashCode()
        // We don't use `upperBound.hashCode()` because it might lead to performance loss for trivial types.
        // While doing something like `31 * lowerBoundResult + Boolean.hashCode(true/* markedNullable */)`
        // to replicate `upperBound.hashCode()` behavior seems too fragile.
        // But we want the result was different from just lowerBound's one,
        // so we add a beautiful though random prime number.
        result = 31 * result + 2999
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
    val lowerType: ConeKotlinType?,
    val captureStatus: CaptureStatus,
    var supertypes: List<ConeKotlinType>? = null,
    val typeParameterMarker: TypeParameterMarker? = null
) : CapturedTypeConstructorMarker, ConeTypeConstructorMarker

data class ConeCapturedType(
    val isMarkedNullable: Boolean = false,
    val constructor: ConeCapturedTypeConstructor,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeSimpleKotlinType(), CapturedTypeMarker {

    override val typeArguments: Array<out ConeTypeProjection>
        get() = EMPTY_ARRAY

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeCapturedType

        if (constructor != other.constructor) return false
        if (isMarkedNullable != other.isMarkedNullable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 7
        result = 31 * result + constructor.hashCode()
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

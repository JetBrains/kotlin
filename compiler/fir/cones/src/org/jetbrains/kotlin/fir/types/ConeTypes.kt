/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.fir.diagnostics.ConeDiagnostic
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeClassifierLookupTag
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.*
import org.jetbrains.kotlin.utils.addToStdlib.foldMap

enum class ProjectionKind {
    STAR, IN, OUT, INVARIANT;

    operator fun plus(other: ProjectionKind): ProjectionKind {
        return when {
            this == other -> this
            this == STAR || other == STAR -> STAR
            this == INVARIANT -> other
            other == INVARIANT -> this
            else -> STAR
        }
    }
}

sealed class ConeTypeProjection : TypeArgumentMarker {
    abstract val kind: ProjectionKind

    companion object {
        val EMPTY_ARRAY = arrayOf<ConeTypeProjection>()
    }
}

object ConeStarProjection : ConeTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.STAR
}

data class ConeKotlinTypeProjectionIn(override val type: ConeKotlinType) : ConeKotlinTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.IN
}

data class ConeKotlinTypeProjectionOut(override val type: ConeKotlinType) : ConeKotlinTypeProjection() {
    override val kind: ProjectionKind
        get() = ProjectionKind.OUT
}

// We assume type IS an invariant type projection to prevent additional wrapper here
// (more exactly, invariant type projection contains type)
sealed class ConeKotlinType : ConeKotlinTypeProjection(), KotlinTypeMarker, TypeArgumentListMarker {
    override val kind: ProjectionKind
        get() = ProjectionKind.INVARIANT

    abstract val typeArguments: Array<out ConeTypeProjection>

    override val type: ConeKotlinType
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

sealed class ConeKotlinTypeProjection : ConeTypeProjection() {
    abstract val type: ConeKotlinType
}

typealias ConeKotlinErrorType = ConeClassErrorType

class ConeClassLikeErrorLookupTag(override val classId: ClassId) : ConeClassLikeLookupTag()

class ConeClassErrorType(val diagnostic: ConeDiagnostic) : ConeClassLikeType() {
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
    val lowerBound: ConeKotlinType,
    val upperBound: ConeKotlinType
) : ConeKotlinType(), FlexibleTypeMarker {

    init {
        val message = { "Bounds violation: $lowerBound, $upperBound" }
        require(lowerBound is SimpleTypeMarker, message)
        require(upperBound is SimpleTypeMarker, message)
    }

    override val typeArguments: Array<out ConeTypeProjection>
        get() = lowerBound.typeArguments

    override val nullability: ConeNullability
        get() = lowerBound.nullability.takeIf { it == upperBound.nullability } ?: ConeNullability.UNKNOWN

    override val attributes: ConeAttributes
        get() = lowerBound.attributes

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeFlexibleType

        if (lowerBound != other.lowerBound) return false
        if (upperBound != other.upperBound) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lowerBound.hashCode()
        result = 31 * result + upperBound.hashCode()
        return result
    }

}

fun ConeKotlinType.upperBoundIfFlexible() = (this as? ConeFlexibleType)?.upperBound ?: this
fun ConeKotlinType.lowerBoundIfFlexible() = (this as? ConeFlexibleType)?.lowerBound ?: this

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
    override val attributes: ConeAttributes = ConeAttributes.Empty
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

data class ConeTypeVariableType(
    override val nullability: ConeNullability,
    override val lookupTag: ConeClassifierLookupTag
) : ConeLookupTagBasedType() {
    override val typeArguments: Array<out ConeTypeProjection> get() = emptyArray()

    override val attributes: ConeAttributes get() = ConeAttributes.Empty
}

data class ConeDefinitelyNotNullType(val original: ConeKotlinType) : ConeSimpleKotlinType(), DefinitelyNotNullTypeMarker {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = original.typeArguments

    override val nullability: ConeNullability
        get() = ConeNullability.NOT_NULL

    override val attributes: ConeAttributes
        get() = ConeAttributes.Empty

    companion object
}

class ConeRawType(lowerBound: ConeKotlinType, upperBound: ConeKotlinType) : ConeFlexibleType(lowerBound, upperBound), RawTypeMarker

/*
 * Contract of the intersection type: it is flat. It means that
 *   intersection type can not contains another intersection types
 *   inside it. To keep this contract construct new intersection types
 *   only via ConeTypeIntersector
 */
class ConeIntersectionType(
    val intersectedTypes: Collection<ConeKotlinType>
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

fun ConeIntersectionType.mapTypes(func: (ConeKotlinType) -> ConeKotlinType): ConeIntersectionType {
    return ConeIntersectionType(intersectedTypes.map(func))
}

class ConeStubType(val variable: ConeTypeVariable, override val nullability: ConeNullability) : StubTypeMarker, ConeSimpleKotlinType() {
    override val typeArguments: Array<out ConeTypeProjection>
        get() = emptyArray()

    override val attributes: ConeAttributes
        get() = ConeAttributes.Empty

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeStubType

        if (variable != other.variable) return false
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + variable.hashCode()
        result = 31 * result + nullability.hashCode()
        return result
    }
}

open class ConeTypeVariable(name: String) : TypeVariableMarker {
    val typeConstructor = ConeTypeVariableTypeConstructor(name)
    val defaultType = ConeTypeVariableType(ConeNullability.NOT_NULL, typeConstructor)

    override fun toString(): String {
        return defaultType.toString()
    }
}

class ConeTypeVariableTypeConstructor(val debugName: String) : ConeClassifierLookupTag(), TypeVariableTypeConstructorMarker {
    override val name: Name get() = Name.identifier(debugName)
}

abstract class ConeIntegerLiteralType(
    val value: Long,
    val isUnsigned: Boolean,
    override val nullability: ConeNullability
) : ConeSimpleKotlinType(), TypeConstructorMarker {
    abstract val possibleTypes: Collection<ConeClassLikeType>
    abstract val supertypes: List<ConeClassLikeType>

    override val typeArguments: Array<out ConeTypeProjection> = emptyArray()

    abstract fun getApproximatedType(expectedType: ConeKotlinType? = null): ConeClassLikeType

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConeIntegerLiteralType

        if (possibleTypes != other.possibleTypes) return false
        if (nullability != other.nullability) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * possibleTypes.hashCode() + nullability.hashCode()
    }
}

fun ConeIntegerLiteralType.canBeInt(): Boolean {
    return value in Int.MIN_VALUE..Int.MAX_VALUE
}

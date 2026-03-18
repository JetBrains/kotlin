/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.model.CapturedTypeMarker
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.KotlinReflectionInternalError

// Based on NewCapturedType but greatly simplified.
internal class CapturedKType(
    val lowerType: KType?,
    val typeConstructor: CapturedKTypeConstructor,
    override val isMarkedNullable: Boolean,
) : AbstractKType(::javaTypeNotSupported), CapturedTypeMarker {
    override val classifier: KClassifier? = null

    override val arguments: List<KTypeProjection> get() = emptyList()

    override val annotations: List<Annotation> get() = emptyList()

    override fun makeNullableAsSpecified(nullable: Boolean): AbstractKType =
        if (nullable == isMarkedNullable) this else CapturedKType(lowerType, typeConstructor, nullable)

    override fun makeDefinitelyNotNullAsSpecified(isDefinitelyNotNull: Boolean): AbstractKType =
        if (!isDefinitelyNotNull) this
        else throw KotlinReflectionInternalError("Definitely not null captured type is not supported yet: $this")

    override val isDefinitelyNotNullType: Boolean get() = false

    override val abbreviation: KType? get() = null
    override val isNothingType: Boolean get() = false
    override val isSuspendFunctionType: Boolean get() = false
    override val isRawType: Boolean get() = false
    override val mutableCollectionClass: KClass<*>? get() = null
    override fun lowerBoundIfFlexible(): AbstractKType? = null
    override fun upperBoundIfFlexible(): AbstractKType? = null

    override fun equals(other: Any?): Boolean =
        other is CapturedKType && lowerType == other.lowerType && typeConstructor == other.typeConstructor &&
                isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int =
        (lowerType.hashCode() * 31 + typeConstructor.hashCode()) * 31 + isMarkedNullable.hashCode()

    override fun toString(): String = typeConstructor.toString()
}

internal class CapturedKTypeConstructor(val projection: KTypeProjection) : CapturedTypeConstructorMarker {
    lateinit var supertypes: List<KType>

    override fun toString(): String = "CapturedType($projection)"
}

internal fun captureKTypeFromArguments(type: KType): KType? {
    val klass = type.classifier as? KClass<*> ?: return null

    val arguments = type.arguments
    if (arguments.all { it.variance == KVariance.INVARIANT }) return null

    val parameters = klass.allTypeParameters()
    if (parameters.size != arguments.size) return null

    val capturedArguments = arguments.map { projection ->
        if (projection.variance == KVariance.INVARIANT) return@map projection
        val lowerType = projection.type.takeIf { projection.variance == KVariance.IN }
        KTypeProjection.invariant(CapturedKType(lowerType, CapturedKTypeConstructor(projection), isMarkedNullable = false))
    }

    val substitutor = KTypeSubstitutor.create(klass, capturedArguments, isSuspendFunctionType = false, isRaw = false)

    for (index in arguments.indices) {
        val oldProjection = arguments[index]
        if (oldProjection.variance == KVariance.INVARIANT) continue

        val capturedTypeSupertypes = parameters[index].upperBounds.mapTo(mutableListOf()) {
            substitutor.substitute(it).type!!
        }

        if (oldProjection.variance == KVariance.OUT) {
            capturedTypeSupertypes += oldProjection.type!!
        }

        val capturedType = capturedArguments[index].type as CapturedKType
        capturedType.typeConstructor.supertypes = capturedTypeSupertypes
    }

    return SimpleKType(
        klass,
        capturedArguments,
        type.isMarkedNullable,
        type.annotations,
        (type as? AbstractKType)?.abbreviation,
        isDefinitelyNotNullType = false,
        isNothingType = false,
        isSuspendFunctionType = false,
        (type as? AbstractKType)?.mutableCollectionClass,
    )
}

internal fun KClass<*>.allTypeParameters(): List<KTypeParameter> =
    generateSequence(this) { if (it.isInner) it.java.declaringClass?.kotlin else null }.flatMap { it.typeParameters }.toList()

private fun javaTypeNotSupported(): Nothing =
    throw KotlinReflectionInternalError("javaType for captured types is not supported")

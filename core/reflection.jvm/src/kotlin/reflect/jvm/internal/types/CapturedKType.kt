/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.model.CapturedTypeConstructorMarker
import org.jetbrains.kotlin.types.model.CapturedTypeMarker
import kotlin.reflect.*
import kotlin.reflect.full.toDescriptorTypeProjection
import kotlin.reflect.jvm.internal.KTypeParameterImpl

// Based on NewCapturedType but greatly simplified.
internal class CapturedKType(
    val lowerType: KType?,
    val typeConstructor: CapturedKTypeConstructor,
    override val isMarkedNullable: Boolean,
) : KType, CapturedTypeMarker {
    override val classifier: KClassifier? = null

    override val arguments: List<KTypeProjection> get() = emptyList()

    override val annotations: List<Annotation> get() = emptyList()

    override fun equals(other: Any?): Boolean =
        other is CapturedKType && lowerType == other.lowerType && typeConstructor == other.typeConstructor &&
                isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int =
        (lowerType.hashCode() * 31 + typeConstructor.hashCode()) * 31 + isMarkedNullable.hashCode()

    override fun toString(): String = typeConstructor.toString()
}

// Currently, still depends on descriptor-based NewCapturedTypeConstructor to ensure that captured types do not become unequal after
// substitution, but it will be removed in a subsequent commit.
internal class CapturedKTypeConstructor(
    val projection: KTypeProjection,
    val kotlinTypeConstructor: NewCapturedTypeConstructor,
) : CapturedTypeConstructorMarker {
    constructor(projection: KTypeProjection, typeParameter: KTypeParameter) : this(
        projection,
        NewCapturedTypeConstructor(
            projection.toDescriptorTypeProjection(
                (typeParameter as? KTypeParameterImpl)?.descriptor
                    ?: error("Captured type should have a type parameter: $projection ($typeParameter)")
            )
        )
    )

    lateinit var supertypes: List<KType>

    override fun equals(other: Any?): Boolean =
        other is CapturedKTypeConstructor && kotlinTypeConstructor == other.kotlinTypeConstructor

    override fun hashCode(): Int =
        kotlinTypeConstructor.hashCode()

    override fun toString(): String = "CapturedType($projection)"
}

internal fun captureKTypeFromArguments(type: KType): KType? {
    val klass = type.classifier as? KClass<*> ?: return null

    val arguments = type.arguments
    if (arguments.all { it.variance == KVariance.INVARIANT }) return null

    val parameters = klass.allTypeParameters()
    if (parameters.size != arguments.size) return null

    val capturedArguments = parameters.zip(arguments).map { (parameter, projection) ->
        if (projection.variance == KVariance.INVARIANT) return@map projection
        val lowerType = projection.type.takeIf { projection.variance == KVariance.IN }
        KTypeProjection.invariant(CapturedKType(lowerType, CapturedKTypeConstructor(projection, parameter), isMarkedNullable = false))
    }

    val substitutor = KTypeSubstitutor.create(klass, capturedArguments)

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

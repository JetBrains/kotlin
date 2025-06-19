/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class KTypeSubstitutor(private val substitution: Map<KTypeParameter, KTypeProjection>) {
    fun substitute(type: KType): KTypeProjection {
        val lowerBound = (type as? AbstractKType)?.lowerBoundIfFlexible()
        val upperBound = (type as? AbstractKType)?.upperBoundIfFlexible()
        if (lowerBound != null && upperBound != null) {
            val substitutedLower = substitute(lowerBound).lowerBoundIfFlexible()
            val substitutedUpper = substitute(upperBound).upperBoundIfFlexible()
            return KTypeProjection(substitutedLower.variance, createPlatformKType(substitutedLower.type!!, substitutedUpper.type!!))
        }

        val classifier = type.classifier ?: return KTypeProjection.invariant(type)
        substitution[classifier]?.let { result ->
            val (variance, resultingType) = result
            return if (resultingType == null) result else KTypeProjection(
                variance,
                resultingType.withNullabilityOf(type),
            )
        }
        val result = KTypeProjection.invariant(
            classifier.createType(
                type.arguments.map { (_, type) ->
                    type?.let(::substitute) ?: KTypeProjection.STAR
                },
                type.isMarkedNullable,
            )
        )
        return result
    }

    // TODO (KT-77700): also keep annotations of 'other'
    private fun KType.withNullabilityOf(other: KType): KType {
        this as AbstractKType
        return makeNullableAsSpecified(
            other.isMarkedNullable || this.isMarkedNullable
        ).makeDefinitelyNotNullAsSpecified(
            (other as AbstractKType).isDefinitelyNotNullType || (isDefinitelyNotNullType && !other.isMarkedNullable)
        )
    }

    private fun KTypeProjection.lowerBoundIfFlexible(): KTypeProjection =
        (type as? AbstractKType)?.lowerBoundIfFlexible()?.let { KTypeProjection(variance, it) } ?: this

    private fun KTypeProjection.upperBoundIfFlexible(): KTypeProjection =
        (type as? AbstractKType)?.upperBoundIfFlexible()?.let { KTypeProjection(variance, it) } ?: this

    companion object {
        fun create(klass: KClass<*>, arguments: List<KTypeProjection>): KTypeSubstitutor =
            KTypeSubstitutor(klass.typeParameters.zip(arguments).toMap())
    }
}

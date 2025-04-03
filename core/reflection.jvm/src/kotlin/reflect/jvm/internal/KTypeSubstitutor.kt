/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

class KTypeSubstitutor(private val substitution: Map<KTypeParameter, KTypeProjection>) {
    fun substitute(type: KType): KTypeProjection {
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
        val isNullable = other.isMarkedNullable || (isMarkedNullable && !(other as AbstractKType).isDefinitelyNotNullType)
        val isDNN = other.classifier !is KClass<*> && !isNullable && (
                (other as AbstractKType).isDefinitelyNotNullType ||
                        (isDefinitelyNotNullType && !other.isMarkedNullable))
        return makeNullableAsSpecified(isNullable).makeDefinitelyNotNullAsSpecified(isDNN)
    }

    companion object {
        fun create(klass: KClass<*>, arguments: List<KTypeProjection>): KTypeSubstitutor =
            KTypeSubstitutor(klass.typeParameters.zip(arguments).toMap())
    }
}

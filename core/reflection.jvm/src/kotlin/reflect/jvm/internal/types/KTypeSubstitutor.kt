/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.RigidTypeMarker
import kotlin.reflect.*
import kotlin.reflect.full.createTypeImpl
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext.withNullability as withNullabilityFromTypeSystem

internal class KTypeSubstitutor(private val substitution: Map<KTypeParameter, KTypeProjection>) {
    fun substitute(type: KType, variance: KVariance = KVariance.INVARIANT): KTypeProjection {
        // Small optimization
        if (substitution.isEmpty()) return KTypeProjection(variance, type)

        val lowerBound = (type as? AbstractKType)?.lowerBoundIfFlexible()
        val upperBound = (type as? AbstractKType)?.upperBoundIfFlexible()
        if (lowerBound != null && upperBound != null) {
            val substitutedLower = substitute(lowerBound, variance).lowerBoundIfFlexible()
            val substitutedUpper = substitute(upperBound, variance).upperBoundIfFlexible()
            val substitutedUpperType = substitutedUpper.type
            val substitutedLowerType = substitutedLower.type
            return when {
                substitutedUpperType != null && substitutedLowerType != null -> KTypeProjection(
                    substitutedLower.variance,
                    createPlatformKType(substitutedLowerType, substitutedUpperType)
                )
                else -> KTypeProjection.STAR
            }
        }

        val classifier = type.classifier ?: return KTypeProjection(variance, type)
        substitution[classifier]?.let { substitutingProjection ->
            val substitutingType = substitutingProjection.type
            val substitutingVariance = substitutingProjection.variance
            return when {
                substitutingType != null && substitutingVariance != null -> KTypeProjection(
                    substitutingVariance.intersectWith(variance),
                    substitutingType.withNullabilityOf(type),
                )
                else -> substitutingProjection
            }
        }
        val result = KTypeProjection(
            variance,
            when {
                type.arguments.isEmpty() -> type
                else -> classifier.createTypeImpl(
                    type.arguments.map { argumentProjection ->
                        val argumentVariance = argumentProjection.variance
                        val argumentType = argumentProjection.type
                        when {
                            argumentType != null && argumentVariance != null ->
                                substitute(argumentType, argumentVariance)
                            else -> KTypeProjection.STAR
                        }
                    },
                    type.isMarkedNullable,
                    type.annotations,
                    (type as? AbstractKType)?.mutableCollectionClass,
                )
            }
        )
        return result
    }

    fun chainedWith(other: KTypeSubstitutor): KTypeSubstitutor {
        // Optimizations
        if (this.substitution.isEmpty()) return other
        if (other.substitution.isEmpty()) return this

        val map = substitution.mapValues { (_, typeProjection) ->
            val type = typeProjection.type
            val variance = typeProjection.variance
            when {
                type != null && variance != null -> other.substitute(type, variance)
                else -> typeProjection
            }
        }
        return KTypeSubstitutor(map)
    }

    // TODO (KT-77700): also keep annotations of 'other'
    private fun KType.withNullabilityOf(other: KType): KType {
        val thiz = this as RigidTypeMarker
        return with(ReflectTypeSystemContext) {
            val withNullability = withNullabilityFromTypeSystem(other.isMarkedNullable || isMarkedNullable)
            if (withNullability is AbstractKType)
                withNullability.makeDefinitelyNotNullAsSpecified(
                    (other as? AbstractKType)?.isDefinitelyNotNullType == true ||
                            ((thiz as? AbstractKType)?.isDefinitelyNotNullType == true && !other.isMarkedNullable)
                )
            else withNullability
        } as KType
    }

    private fun KTypeProjection.lowerBoundIfFlexible(): KTypeProjection =
        (type as? AbstractKType)?.lowerBoundIfFlexible()?.let { KTypeProjection(variance, it) } ?: this

    private fun KTypeProjection.upperBoundIfFlexible(): KTypeProjection =
        (type as? AbstractKType)?.upperBoundIfFlexible()?.let { KTypeProjection(variance, it) } ?: this

    companion object {
        val EMPTY = KTypeSubstitutor(emptyMap())

        fun create(type: KType): KTypeSubstitutor {
            val parameters = with(ReflectTypeSystemContext) {
                val classifier = (type as AbstractKType).typeConstructor()
                List(classifier.parametersCount()) { classifier.getParameter(it) as KTypeParameter }
            }
            return if (parameters.isNotEmpty()) KTypeSubstitutor(parameters.zip(type.arguments).toMap()) else EMPTY
        }

        fun create(klass: KClass<*>, arguments: List<KTypeProjection>, isSuspendFunctionType: Boolean): KTypeSubstitutor {
            val typeParameters = klass.allTypeParameters().run {
                // For a type `suspend () -> String` (also known as `SuspendFunction0<String>`), the classifier will be `Function1` because
                // suspend functions are mapped to normal functions (with +1 arity) on JVM.
                if (isSuspendFunctionType) drop(1) else this
            }
            return KTypeSubstitutor(typeParameters.zip(arguments).toMap())
        }
    }
}

/**
 * - One can use covariant types only in return positions
 * - One can use contravariant types only in parameter positions
 * - Invariant types can be used in both positions
 *
 * From that perspective, "invariant" concept is a union of "covariant" concept and "contravariant" concepts
 */
private fun KVariance.intersectWith(other: KVariance): KVariance = when {
    this == KVariance.INVARIANT -> other
    other == KVariance.INVARIANT -> this
    this != other -> error("CONFLICTING_PROJECTION") // Empty intersection
    else -> this
}

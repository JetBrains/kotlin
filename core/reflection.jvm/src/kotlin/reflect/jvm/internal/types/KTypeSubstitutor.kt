/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.createTypeImpl
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext.isFlexible
import kotlin.reflect.jvm.internal.types.ReflectTypeSystemContext.withNullability as withNullabilityFromTypeSystem

internal class KTypeSubstitutor(
    private val substitution: Map<KTypeParameter, KTypeProjection>,
    private val eraseToUpperBoundsAfterSubstitution: Boolean = false,
) {
    fun substitute(type: KType, variance: KVariance = KVariance.INVARIANT): KTypeProjection {
        val substituted = substituteWithoutErasureRecursively(type, variance)
        return when (eraseToUpperBoundsAfterSubstitution) {
            true -> substituted.copy(type = substituted.type?.eraseToUpperBoundsAndMakeItRawRecursively())
            false -> substituted
        }
    }

    private fun substituteWithoutErasureRecursively(type: KType, variance: KVariance = KVariance.INVARIANT): KTypeProjection {
        // Small optimization
        if (substitution.isEmpty()) return KTypeProjection(variance, type)

        val lowerBound = (type as? AbstractKType)?.lowerBoundIfFlexible()
        val upperBound = (type as? AbstractKType)?.upperBoundIfFlexible()
        if (lowerBound != null && upperBound != null) {
            val substitutedLower = substituteWithoutErasureRecursively(lowerBound, variance).lowerBoundIfFlexible()
            val substitutedUpper = substituteWithoutErasureRecursively(upperBound, variance).upperBoundIfFlexible()
            val substitutedUpperType = substitutedUpper.type
            val substitutedLowerType = substitutedLower.type
            return when {
                substitutedUpperType != null && substitutedLowerType != null -> KTypeProjection(
                    substitutedLower.variance,
                    createPlatformKType(substitutedLowerType, substitutedUpperType, isRawType = type.isRawType)
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
                    substitutingType.withWorseNullabilityOfBoth(type),
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
                                substituteWithoutErasureRecursively(argumentType, argumentVariance)
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
        // Once erased, all future type parameter substitutions must be noop
        if (this.eraseToUpperBoundsAfterSubstitution) return this

        // Optimizations
        if (this.substitution.isEmpty()) return EMPTY.copy(other.eraseToUpperBoundsAfterSubstitution)
        if (other.substitution.isEmpty()) return this.copy(other.eraseToUpperBoundsAfterSubstitution)

        val map = substitution.mapValues { (_, typeProjection) ->
            val type = typeProjection.type
            val variance = typeProjection.variance
            when {
                type != null && variance != null -> other.substitute(type, variance)
                else -> typeProjection
            }
        }
        return KTypeSubstitutor(map, other.eraseToUpperBoundsAfterSubstitution)
    }

    fun disjointSumWith(other: KTypeSubstitutor, memberNameForDebug: String): KTypeSubstitutor {
        val erase = eraseToUpperBoundsAfterSubstitution || other.eraseToUpperBoundsAfterSubstitution

        // Optimizations
        if (this.substitution.isEmpty()) return other.copy(erase)
        if (other.substitution.isEmpty()) return this.copy(erase)

        val intersection = substitution.keys.intersect(other.substitution.keys)
        check(intersection.isEmpty()) {
            "Substitutors must not have intersecting keys: ${intersection.joinToString()}. Member: $memberNameForDebug"
        }

        return KTypeSubstitutor(substitution + other.substitution, erase)
    }

    private fun copy(eraseToUpperBoundsAfterSubstitution: Boolean): KTypeSubstitutor =
        when {
            eraseToUpperBoundsAfterSubstitution == this.eraseToUpperBoundsAfterSubstitution -> this
            substitution.isEmpty() && !eraseToUpperBoundsAfterSubstitution -> EMPTY
            substitution.isEmpty() && eraseToUpperBoundsAfterSubstitution -> RAW_SUBSTITUTION
            else -> KTypeSubstitutor(substitution, eraseToUpperBoundsAfterSubstitution)
        }

    // TODO (KT-77700): also keep annotations of 'other'
    private fun KType.withWorseNullabilityOfBoth(other: KType): KType {
        check(other is KotlinTypeMarker && !other.isFlexible()) { "'$other' must be non flexible" }
        if (isNullabilityFlexible() && !other.isMarkedNullable) return this
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
        val RAW_SUBSTITUTION = KTypeSubstitutor(emptyMap(), eraseToUpperBoundsAfterSubstitution = true)

        fun create(type: KType): KTypeSubstitutor {
            val isRaw = (type as? AbstractKType)?.isRawType == true
            val type = if (isRaw) type.eraseToUpperBoundsAndMakeItRawRecursively() else type
            val parameters = with(ReflectTypeSystemContext) {
                val classifier = (type as AbstractKType).typeConstructor()
                List(classifier.parametersCount()) { classifier.getParameter(it) as KTypeParameter }
            }
            check(parameters.size == type.arguments.size) {
                "Params vs args count mismatch (${parameters.size} != ${type.arguments.size}) for type '$type'"
            }
            return when {
                parameters.isEmpty() -> EMPTY.copy(isRaw)
                else -> KTypeSubstitutor(parameters.zip(type.arguments).toMap(), eraseToUpperBoundsAfterSubstitution = isRaw)
            }
        }

        fun create(klass: KClass<*>, arguments: List<KTypeProjection>, isSuspendFunctionType: Boolean): KTypeSubstitutor {
            val typeParameters = klass.allTypeParameters().run {
                // For a type `suspend () -> String` (also known as `SuspendFunction0<String>`), the classifier will be `Function1` because
                // suspend functions are mapped to normal functions (with +1 arity) on JVM.
                if (isSuspendFunctionType) drop(1) else this
            }
            check(typeParameters.size == arguments.size) {
                "Params vs args count mismatch (${typeParameters.size} != ${arguments.size}) for class '$klass' with args: ${arguments.joinToString()}"
            }
            return KTypeSubstitutor(typeParameters.zip(arguments).toMap())
        }
    }
}

private fun KType.isNullabilityFlexible(): Boolean =
    this is AbstractKType && lowerBoundIfFlexible()?.isMarkedNullable != upperBoundIfFlexible()?.isMarkedNullable

/**
 * Erase all type arguments to the first upper bound of their respective type parameter and convert all parametrized types to raw types
 *
 * The respective places in the compiler:
 * - K1: TypeParameterUpperBoundEraser
 * - K2: getProjectionForRawType, eraseToUpperBound
 */
private fun KType.eraseToUpperBoundsAndMakeItRawRecursively(seedTypeOfTheRecursionForDebug: KType = this): KType {
    val type = generateSequence(this) { (it.classifier as? KTypeParameter)?.upperBounds?.firstOrNull() }.last()
    with(type) {
        val arguments = arguments
        if (arguments.isEmpty()) return type
        val parameters = with(ReflectTypeSystemContext) {
            val classifier = (type as AbstractKType).typeConstructor()
            List(classifier.parametersCount()) { classifier.getParameter(it) as KTypeParameter }
        }
        check(parameters.size == arguments.size) {
            "Error inside type '$seedTypeOfTheRecursionForDebug'. '$type' params (${parameters.size}) vs args (${arguments.size}) mismatch."
        }

        val newLowerBoundArguments = parameters.map { parameter ->
            val firstUpperBound = parameter.upperBounds.firstOrNull() ?: error(
                "Error inside type '$seedTypeOfTheRecursionForDebug'. " +
                        "Parameter '$parameter' has no upper bounds. " +
                        "There must always be at least the default 'Any?' upper bound"
            )
            val newType = firstUpperBound.eraseToUpperBoundsAndMakeItRawRecursively(seedTypeOfTheRecursionForDebug)
            KTypeProjection.invariant(newType)
        }

        val classifier =
            classifier ?: error("Error inside type '$seedTypeOfTheRecursionForDebug'. The current type '$type' is not denotable")
        val lower = classifier.createType(arguments = newLowerBoundArguments, nullable = isMarkedNullable)
        val upper = classifier.createType(arguments = List(parameters.size) { KTypeProjection.STAR }, nullable = isMarkedNullable)
        return createPlatformKType(lower, upper, isRawType = true)
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

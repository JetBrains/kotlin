/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.EmptyIntersectionTypeKind
import org.jetbrains.kotlin.types.isDefinitelyEmpty
import org.jetbrains.kotlin.types.isPossiblyEmpty
import org.jetbrains.kotlin.types.model.*

internal object EmptyIntersectionTypeChecker {
    fun computeEmptyIntersectionTypeKind(
        context: TypeSystemInferenceExtensionContext,
        types: Collection<KotlinTypeMarker>
    ): EmptyIntersectionTypeKind = with(context) {
        if (types.isEmpty())
            return EmptyIntersectionTypeKind.NOT_EMPTY_INTERSECTION

        @Suppress("NAME_SHADOWING")
        val types = types.toList()
        var possibleEmptyIntersectionKind: EmptyIntersectionTypeKind? = null

        for (i in 0 until types.size) {
            val firstType = types[i]

            if (!mayCauseEmptyIntersection(firstType)) continue

            val firstSubstitutedType by lazy { firstType.eraseContainingTypeParameters() }

            for (j in i + 1 until types.size) {
                val secondType = types[j]

                if (!mayCauseEmptyIntersection(secondType)) continue

                val secondSubstitutedType = secondType.eraseContainingTypeParameters()

                if (!mayCauseEmptyIntersection(secondSubstitutedType) && !mayCauseEmptyIntersection(firstSubstitutedType)) continue

                val kind = computeKindByHavingCommonSubtype(firstSubstitutedType, secondSubstitutedType)

                if (kind.isDefinitelyEmpty())
                    return kind

                if (kind.isPossiblyEmpty())
                    possibleEmptyIntersectionKind = kind
            }
        }

        return possibleEmptyIntersectionKind ?: EmptyIntersectionTypeKind.NOT_EMPTY_INTERSECTION
    }

    private fun TypeSystemInferenceExtensionContext.computeKindByHavingCommonSubtype(
        first: KotlinTypeMarker, second: KotlinTypeMarker
    ): EmptyIntersectionTypeKind {
        fun extractIntersectionComponentsIfNeeded(type: KotlinTypeMarker) =
            if (type.typeConstructor() is IntersectionTypeConstructorMarker) {
                type.typeConstructor().supertypes().toList()
            } else listOf(type)

        val expandedTypes = extractIntersectionComponentsIfNeeded(first) + extractIntersectionComponentsIfNeeded(second)
        val typeCheckerState by lazy { newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true) }
        var possibleEmptyIntersectionKind: EmptyIntersectionTypeKind? = null

        for (i in expandedTypes.indices) {
            val firstType = expandedTypes[i].withNullability(false)
            val firstTypeConstructor = firstType.typeConstructor()

            if (!mayCauseEmptyIntersection(firstType))
                continue

            for (j in i + 1 until expandedTypes.size) {
                val secondType = expandedTypes[j].withNullability(false)
                val secondTypeConstructor = secondType.typeConstructor()

                if (!mayCauseEmptyIntersection(secondType))
                    continue

                if (areEqualTypeConstructors(firstTypeConstructor, secondTypeConstructor) && secondTypeConstructor.parametersCount() == 0)
                    continue

                if (AbstractTypeChecker.areRelatedBySubtyping(this, firstType, secondType))
                    continue

                // If two classes aren't related by subtyping and no need to compare their type arguments, then they can't have a common subtype
                if (
                    firstTypeConstructor.isDefinitelyClassTypeConstructor() && secondTypeConstructor.isDefinitelyClassTypeConstructor()
                    && (firstTypeConstructor.parametersCount() == 0 || secondTypeConstructor.parametersCount() == 0)
                ) {
                    return EmptyIntersectionTypeKind.MULTIPLE_CLASSES
                }

                val superTypeByFirstConstructor = AbstractTypeChecker.findCorrespondingSupertypes(
                    typeCheckerState, firstType.lowerBoundIfFlexible(), secondTypeConstructor
                ).singleOrNull()
                val superTypeBySecondConstructor = AbstractTypeChecker.findCorrespondingSupertypes(
                    typeCheckerState, secondType.lowerBoundIfFlexible(), firstTypeConstructor
                ).singleOrNull()

                val anyInference = firstTypeConstructor.isInterface() || secondTypeConstructor.isInterface()

                // Two classes can't have a common subtype if neither is a subtype of another
                if (superTypeByFirstConstructor == null && superTypeBySecondConstructor == null && !anyInference)
                    return EmptyIntersectionTypeKind.MULTIPLE_CLASSES

                if (anyInference && !canHaveCommonSubtypeWithInterface(firstType, secondType))
                    return EmptyIntersectionTypeKind.INCOMPATIBLE_SUPERTYPES

                if (superTypeByFirstConstructor == null || superTypeBySecondConstructor == null) {
                    // don't have incompatible supertypes so can have a common subtype only if all types are interfaces
                    if (firstTypeConstructor.isFinalClassConstructor() || secondTypeConstructor.isFinalClassConstructor()) {
                        possibleEmptyIntersectionKind = EmptyIntersectionTypeKind.SINGLE_FINAL_CLASS
                    }
                    continue
                }

                val argumentsIntersectionKind =
                    computeKindByCheckingTypeArguments(superTypeByFirstConstructor, superTypeBySecondConstructor)

                if (argumentsIntersectionKind.isDefinitelyEmpty())
                    return argumentsIntersectionKind

                if (possibleEmptyIntersectionKind == null && argumentsIntersectionKind.isPossiblyEmpty())
                    possibleEmptyIntersectionKind = argumentsIntersectionKind
            }
        }

        return possibleEmptyIntersectionKind ?: EmptyIntersectionTypeKind.NOT_EMPTY_INTERSECTION
    }

    private fun TypeSystemInferenceExtensionContext.computeKindByCheckingTypeArguments(
        firstType: KotlinTypeMarker,
        secondType: KotlinTypeMarker,
    ): EmptyIntersectionTypeKind {
        require(firstType.typeConstructor() == secondType.typeConstructor()) {
            "Type constructors of the passed types should be the same to compare their arguments"
        }

        fun isSubtypeOf(firstType: KotlinTypeMarker, secondType: KotlinTypeMarker) =
            AbstractTypeChecker.isSubtypeOf(this, firstType, secondType)

        fun areEqualTypes(firstType: KotlinTypeMarker, secondType: KotlinTypeMarker) =
            AbstractTypeChecker.equalTypes(this, firstType, secondType)

        fun Boolean.toEmptyIntersectionKind() =
            if (this) EmptyIntersectionTypeKind.NOT_EMPTY_INTERSECTION else EmptyIntersectionTypeKind.INCOMPATIBLE_TYPE_ARGUMENTS

        var possibleEmptyIntersectionKind: EmptyIntersectionTypeKind? = null

        for ((i, argumentOfFirst) in firstType.getArguments().withIndex()) {
            @Suppress("NAME_SHADOWING")
            val argumentOfFirst = uncaptureIfNeeded(argumentOfFirst)
            val argumentOfSecond = uncaptureIfNeeded(secondType.getArgument(i))

            if (argumentOfFirst == argumentOfSecond || argumentOfFirst.isStarProjection() || argumentOfSecond.isStarProjection())
                continue

            val argumentTypeOfFirst = argumentOfFirst.getType()
            val argumentTypeOfSecond = argumentOfSecond.getType()
            val intersectionKindOfArguments = when {
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.INV, TypeVariance.INV) ->
                    areEqualTypes(argumentTypeOfFirst, argumentTypeOfSecond).toEmptyIntersectionKind()
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.INV, TypeVariance.OUT) -> {
                    isSubtypeOf(argumentTypeOfFirst, argumentTypeOfSecond).toEmptyIntersectionKind()
                }
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.INV, TypeVariance.IN) -> {
                    isSubtypeOf(argumentTypeOfSecond, argumentTypeOfFirst).toEmptyIntersectionKind()
                }
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.IN, TypeVariance.OUT) -> {
                    if (argumentTypeOfFirst.argumentsCount() == 0 && argumentTypeOfSecond.argumentsCount() == 0) {
                        isSubtypeOf(argumentTypeOfFirst, argumentTypeOfSecond).toEmptyIntersectionKind()
                    } else {
                        computeKindByHavingCommonSubtype(argumentTypeOfFirst, argumentTypeOfSecond)
                    }
                }
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.OUT, TypeVariance.OUT)
                        || areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.IN, TypeVariance.IN) -> {
                    computeKindByHavingCommonSubtype(argumentTypeOfFirst, argumentTypeOfSecond)
                }
                else -> true.toEmptyIntersectionKind()
            }

            if (intersectionKindOfArguments.isDefinitelyEmpty())
                return intersectionKindOfArguments

            if (possibleEmptyIntersectionKind == null && intersectionKindOfArguments.isPossiblyEmpty())
                possibleEmptyIntersectionKind = intersectionKindOfArguments
        }

        return possibleEmptyIntersectionKind ?: EmptyIntersectionTypeKind.NOT_EMPTY_INTERSECTION
    }

    private fun TypeSystemInferenceExtensionContext.canHaveCommonSubtypeWithInterface(
        firstType: KotlinTypeMarker, secondType: KotlinTypeMarker
    ): Boolean {
        require(firstType.typeConstructor().isInterface() || secondType.typeConstructor().isInterface()) {
            "One of the passed type should be an interface"
        }
        @Suppress("NAME_SHADOWING")
        val firstType = firstType.eraseContainingTypeParameters()

        @Suppress("NAME_SHADOWING")
        val secondType = secondType.eraseContainingTypeParameters()

        // interface A<K>
        // interface B: A<String>
        // interface C: A<Int>
        // B & C can't have common subtype due to having incompatible supertypes: A<String> and A<Int>
        return !hasIncompatibleSuperTypes(firstType, secondType)
    }

    private fun TypeSystemInferenceExtensionContext.areIncompatibleSuperTypes(
        firstType: KotlinTypeMarker, secondType: KotlinTypeMarker
    ): Boolean = firstType.typeConstructor() == secondType.typeConstructor()
            && !AbstractTypeChecker.equalTypes(
        newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
        firstType,
        secondType
    )

    // interface A<T>
    // interface B : A<Int>
    // interface C : A<String>
    // => B and C have incompatible supertypes
    private fun TypeSystemInferenceExtensionContext.hasIncompatibleSuperTypes(
        firstType: KotlinTypeMarker, secondType: KotlinTypeMarker
    ): Boolean {
        val superTypesOfFirst = firstType.typeConstructor().supertypes()
        val firstTypeSubstitutor = createSubstitutorForSuperTypes(firstType)
        val superTypesOfSecond = secondType.typeConstructor().supertypes()
        val secondTypeSubstitutor = createSubstitutorForSuperTypes(secondType)

        for (superTypeOfFirst in superTypesOfFirst) {
            @Suppress("NAME_SHADOWING")
            val superTypeOfFirst = firstTypeSubstitutor?.safeSubstitute(superTypeOfFirst) ?: superTypeOfFirst

            if (areIncompatibleSuperTypes(superTypeOfFirst, secondType))
                return true

            for (superTypeOfSecond in superTypesOfSecond) {
                @Suppress("NAME_SHADOWING")
                val superTypeOfSecond = secondTypeSubstitutor?.safeSubstitute(superTypeOfSecond) ?: superTypeOfSecond

                if (
                    areIncompatibleSuperTypes(firstType, superTypeOfSecond)
                    || areIncompatibleSuperTypes(superTypeOfFirst, superTypeOfSecond)
                ) return true

                if (hasIncompatibleSuperTypes(superTypeOfFirst, superTypeOfSecond))
                    return true
            }
        }

        return false
    }

    private fun TypeSystemInferenceExtensionContext.mayCauseEmptyIntersection(type: KotlinTypeMarker): Boolean {
        val typeConstructor = type.typeConstructor()

        if (!typeConstructor.isClassTypeConstructor() && !typeConstructor.isTypeParameterTypeConstructor())
            return false

        // Even two interfaces may be an empty intersection type:
        // interface Inv<K>
        // interface B : Inv<Int>
        // `Inv<String> & B` or `Inv<String> & Inv<Int>` are empty
        // So we don't filter out interfaces here
        return !typeConstructor.isAnyConstructor() && !typeConstructor.isNothingConstructor()
    }

    private fun TypeSystemInferenceExtensionContext.areArgumentsOfSpecifiedVariances(
        firstType: KotlinTypeMarker,
        secondType: KotlinTypeMarker,
        argumentIndex: Int,
        variance1: TypeVariance,
        variance2: TypeVariance,
    ): Boolean {
        fun getEffectiveVariance(type: KotlinTypeMarker): TypeVariance? {
            val argument = uncaptureIfNeeded(type.getArgument(argumentIndex))
            val parameter = type.typeConstructor().getParameter(argumentIndex)
            return AbstractTypeChecker.effectiveVariance(parameter.getVariance(), argument.getVariance())
        }

        val effectiveVariance1 = getEffectiveVariance(firstType)
        val effectiveVariance2 = getEffectiveVariance(secondType)

        return (effectiveVariance1 == variance1 && effectiveVariance2 == variance2)
                || (effectiveVariance1 == variance2 && effectiveVariance2 == variance1)
    }

    private fun TypeSystemInferenceExtensionContext.uncaptureIfNeeded(argument: TypeArgumentMarker): TypeArgumentMarker {
        val type = argument.getType()
        return if (type is CapturedTypeMarker) type.typeConstructorProjection() else argument
    }
}
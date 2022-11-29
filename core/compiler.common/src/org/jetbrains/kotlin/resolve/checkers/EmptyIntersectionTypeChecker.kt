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
    fun computeEmptyIntersectionEmptiness(
        context: TypeSystemInferenceExtensionContext,
        types: Collection<KotlinTypeMarker>
    ): EmptyIntersectionTypeInfo? = with(context) {
        if (types.isEmpty()) return null

        @Suppress("NAME_SHADOWING")
        val types = types.toList()
        var possibleEmptyIntersectionTypeInfo: EmptyIntersectionTypeInfo? = null

        for (i in 0 until types.size) {
            val firstType = types[i]

            if (!mayCauseEmptyIntersection(firstType)) continue

            val firstSubstitutedType by lazy { firstType.eraseContainingTypeParameters() }

            for (j in i + 1 until types.size) {
                val secondType = types[j]

                if (!mayCauseEmptyIntersection(secondType)) continue

                val secondSubstitutedType = secondType.eraseContainingTypeParameters()

                if (!mayCauseEmptyIntersection(secondSubstitutedType) && !mayCauseEmptyIntersection(firstSubstitutedType)) continue

                val typeInfo = computeByHavingCommonSubtype(firstSubstitutedType, secondSubstitutedType) ?: continue

                if (typeInfo.kind.isDefinitelyEmpty())
                    return typeInfo

                if (typeInfo.kind.isPossiblyEmpty())
                    possibleEmptyIntersectionTypeInfo = typeInfo
            }
        }

        return possibleEmptyIntersectionTypeInfo
    }

    private fun TypeSystemInferenceExtensionContext.computeByHavingCommonSubtype(
        first: KotlinTypeMarker, second: KotlinTypeMarker
    ): EmptyIntersectionTypeInfo? {
        fun extractIntersectionComponentsIfNeeded(type: KotlinTypeMarker) =
            if (type.typeConstructor() is IntersectionTypeConstructorMarker) {
                type.typeConstructor().supertypes().toList()
            } else listOf(type)

        val expandedTypes = extractIntersectionComponentsIfNeeded(first) + extractIntersectionComponentsIfNeeded(second)
        val typeCheckerState by lazy { newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true) }
        var possibleEmptyIntersectionKind: EmptyIntersectionTypeInfo? = null

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
                    return EmptyIntersectionTypeInfo(EmptyIntersectionTypeKind.MULTIPLE_CLASSES, firstType, secondType)
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
                    return EmptyIntersectionTypeInfo(EmptyIntersectionTypeKind.MULTIPLE_CLASSES, firstType, secondType)

                if (anyInference) {
                    val incompatibleSupertypes = getIncompatibleSuperTypes(firstType, secondType)
                    if (incompatibleSupertypes != null) {
                        return EmptyIntersectionTypeInfo(EmptyIntersectionTypeKind.INCOMPATIBLE_SUPERTYPES, *incompatibleSupertypes)
                    }
                }

                if (superTypeByFirstConstructor == null || superTypeBySecondConstructor == null) {
                    // don't have incompatible supertypes so can have a common subtype only if all types are interfaces
                    if (firstTypeConstructor.isFinalClassConstructor() || secondTypeConstructor.isFinalClassConstructor()) {
                        possibleEmptyIntersectionKind =
                            EmptyIntersectionTypeInfo(EmptyIntersectionTypeKind.SINGLE_FINAL_CLASS, firstType, secondType)
                    }
                    continue
                }

                val argumentsIntersectionKind =
                    computeByCheckingTypeArguments(superTypeByFirstConstructor, superTypeBySecondConstructor) ?: continue

                if (argumentsIntersectionKind.kind.isDefinitelyEmpty())
                    return argumentsIntersectionKind

                if (possibleEmptyIntersectionKind == null && argumentsIntersectionKind.kind.isPossiblyEmpty())
                    possibleEmptyIntersectionKind = argumentsIntersectionKind
            }
        }

        return possibleEmptyIntersectionKind
    }

    private fun TypeSystemInferenceExtensionContext.computeByCheckingTypeArguments(
        firstType: KotlinTypeMarker,
        secondType: KotlinTypeMarker,
    ): EmptyIntersectionTypeInfo? {
        require(firstType.typeConstructor() == secondType.typeConstructor()) {
            "Type constructors of the passed types should be the same to compare their arguments"
        }

        fun isSubtypeOf(firstType: KotlinTypeMarker, secondType: KotlinTypeMarker) =
            AbstractTypeChecker.isSubtypeOf(this, firstType, secondType)

        fun areEqualTypes(firstType: KotlinTypeMarker, secondType: KotlinTypeMarker) =
            AbstractTypeChecker.equalTypes(this, firstType, secondType)

        fun Boolean.toEmptyIntersectionKind(vararg types: KotlinTypeMarker) =
            if (this) null else EmptyIntersectionTypeInfo(EmptyIntersectionTypeKind.INCOMPATIBLE_TYPE_ARGUMENTS, *types)

        var possibleEmptyIntersectionTypeInfo: EmptyIntersectionTypeInfo? = null

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
                    areEqualTypes(argumentTypeOfFirst, argumentTypeOfSecond)
                        .toEmptyIntersectionKind(argumentTypeOfFirst, argumentTypeOfSecond)
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.INV, TypeVariance.OUT) ->
                    isSubtypeOf(argumentTypeOfFirst, argumentTypeOfSecond)
                        .toEmptyIntersectionKind(argumentTypeOfFirst, argumentTypeOfSecond)
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.INV, TypeVariance.IN) ->
                    isSubtypeOf(argumentTypeOfSecond, argumentTypeOfFirst)
                        .toEmptyIntersectionKind(argumentTypeOfFirst, argumentTypeOfSecond)
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.IN, TypeVariance.OUT) -> {
                    if (argumentTypeOfFirst.argumentsCount() == 0 && argumentTypeOfSecond.argumentsCount() == 0) {
                        isSubtypeOf(argumentTypeOfFirst, argumentTypeOfSecond)
                            .toEmptyIntersectionKind(argumentTypeOfFirst, argumentTypeOfSecond)
                    } else {
                        computeByHavingCommonSubtype(argumentTypeOfFirst, argumentTypeOfSecond)
                    }
                }
                areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.OUT, TypeVariance.OUT)
                        || areArgumentsOfSpecifiedVariances(firstType, secondType, i, TypeVariance.IN, TypeVariance.IN) -> {
                    computeByHavingCommonSubtype(argumentTypeOfFirst, argumentTypeOfSecond)
                }
                else -> true.toEmptyIntersectionKind(argumentTypeOfFirst, argumentTypeOfSecond)
            } ?: continue

            if (intersectionKindOfArguments.kind.isDefinitelyEmpty())
                return intersectionKindOfArguments

            if (possibleEmptyIntersectionTypeInfo == null && intersectionKindOfArguments.kind.isPossiblyEmpty())
                possibleEmptyIntersectionTypeInfo = intersectionKindOfArguments
        }

        return possibleEmptyIntersectionTypeInfo
    }

    private fun TypeSystemInferenceExtensionContext.getIncompatibleSuperTypes(
        firstType: KotlinTypeMarker, secondType: KotlinTypeMarker
    ): Array<KotlinTypeMarker>? {
        @Suppress("NAME_SHADOWING")
        val firstType = firstType.eraseContainingTypeParameters()

        @Suppress("NAME_SHADOWING")
        val secondType = secondType.eraseContainingTypeParameters()

        // interface A<K>
        // interface B: A<String>
        // interface C: A<Int>
        // => B and C have incompatible supertypes
        val superTypesOfFirst = firstType.typeConstructor().supertypes()
        val firstTypeSubstitutor = createSubstitutorForSuperTypes(firstType)
        val superTypesOfSecond = secondType.typeConstructor().supertypes()
        val secondTypeSubstitutor = createSubstitutorForSuperTypes(secondType)

        for (superTypeOfFirst in superTypesOfFirst) {
            @Suppress("NAME_SHADOWING")
            val superTypeOfFirst = firstTypeSubstitutor?.safeSubstitute(superTypeOfFirst) ?: superTypeOfFirst

            if (areIncompatibleSuperTypes(superTypeOfFirst, secondType))
                return arrayOf(superTypeOfFirst, secondType)

            for (superTypeOfSecond in superTypesOfSecond) {
                @Suppress("NAME_SHADOWING")
                val superTypeOfSecond = secondTypeSubstitutor?.safeSubstitute(superTypeOfSecond) ?: superTypeOfSecond

                if (areIncompatibleSuperTypes(firstType, superTypeOfSecond))
                    return arrayOf(firstType, superTypeOfSecond)

                if (areIncompatibleSuperTypes(superTypeOfFirst, superTypeOfSecond))
                    return arrayOf(superTypeOfFirst, superTypeOfSecond)

                getIncompatibleSuperTypes(superTypeOfFirst, superTypeOfSecond)?.let { return it }
            }
        }

        return null
    }

    private fun TypeSystemInferenceExtensionContext.areIncompatibleSuperTypes(
        firstType: KotlinTypeMarker, secondType: KotlinTypeMarker
    ): Boolean = firstType.typeConstructor() == secondType.typeConstructor()
            && !AbstractTypeChecker.equalTypes(
        newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
        firstType, secondType
    )

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

class EmptyIntersectionTypeInfo(val kind: EmptyIntersectionTypeKind, vararg val casingTypes: KotlinTypeMarker)
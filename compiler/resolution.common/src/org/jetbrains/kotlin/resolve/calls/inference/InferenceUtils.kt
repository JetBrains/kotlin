/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.inference.components.extractProjectionsForAllCapturedTypes
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage
import org.jetbrains.kotlin.resolve.calls.inference.model.NewConstraintSystemImpl
import org.jetbrains.kotlin.types.model.*

fun ConstraintStorage.buildCurrentSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    additionalBindings: Map<TypeConstructorMarker, KotlinTypeMarker>
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables.entries.associate { it.key to it.value } + additionalBindings)
}

fun ConstraintStorage.buildAbstractResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): TypeSubstitutorMarker = with(context) {
    if (allTypeVariables.isEmpty()) return createEmptySubstitutor()

    val currentSubstitutorMap = fixedTypeVariables.entries.associate {
        it.key to it.value
    }
    val uninferredSubstitutorMap = if (transformTypeVariablesToErrorTypes) {
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to context.createUninferredType(
                (typeVariable.typeVariable).freshTypeConstructor()
            )
        }
    } else {
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to typeVariable.typeVariable.defaultType(this)
        }
    }
    return context.typeSubstitutorByTypeConstructor(currentSubstitutorMap + uninferredSubstitutorMap)
}

fun ConstraintStorage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(
    context: TypeSystemInferenceExtensionContext
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(
        notFixedTypeVariables.mapValues { context.createStubTypeForTypeVariablesInSubtyping(it.value.typeVariable) }
    )
}

fun TypeSystemInferenceExtensionContext.hasRecursiveTypeParametersWithGivenSelfType(selfTypeConstructor: TypeConstructorMarker): Boolean =
    selfTypeConstructor.getParameters().any { it.hasRecursiveBounds(selfTypeConstructor) } ||
            isK2 && selfTypeConstructor is CapturedTypeConstructorMarker && selfTypeConstructor.supertypes().any { hasRecursiveTypeParametersWithGivenSelfType(it.typeConstructor()) }

fun TypeSystemInferenceExtensionContext.isRecursiveTypeParameter(typeConstructor: TypeConstructorMarker) =
    typeConstructor.getTypeParameterClassifier()?.hasRecursiveBounds() == true

fun TypeSystemInferenceExtensionContext.extractTypeForGivenRecursiveTypeParameter(
    type: KotlinTypeMarker,
    typeParameter: TypeParameterMarker
): KotlinTypeMarker? {
    for (argument in type.getArguments()) {
        if (argument.isStarProjection()) continue
        val typeConstructor = argument.getType().typeConstructor()
        if (typeConstructor is TypeVariableTypeConstructorMarker
            && typeConstructor.typeParameter == typeParameter
            && typeConstructor.typeParameter?.hasRecursiveBounds(type.typeConstructor()) == true
        ) {
            return type
        }
        extractTypeForGivenRecursiveTypeParameter(argument.getType(), typeParameter)?.let { return it }
    }

    return null
}

fun NewConstraintSystemImpl.registerTypeVariableIfNotPresent(
    typeVariable: TypeVariableMarker
) {
    val builder = getBuilder()
    if (typeVariable.freshTypeConstructor(this) !in builder.currentStorage().allTypeVariables.keys) {
        builder.registerVariable(typeVariable)
    }
}

fun TypeSystemInferenceExtensionContext.extractAllContainingTypeVariables(type: KotlinTypeMarker): Set<TypeConstructorMarker> = buildSet {
    extractAllContainingTypeVariablesNoCaptureTypeProcessing(type, this)

    val typeProjections = extractProjectionsForAllCapturedTypes(type)

    typeProjections.forEach { typeProjectionsType ->
        extractAllContainingTypeVariablesNoCaptureTypeProcessing(typeProjectionsType, this)
    }
}

private fun TypeSystemInferenceExtensionContext.extractAllContainingTypeVariablesNoCaptureTypeProcessing(
    type: KotlinTypeMarker,
    result: MutableSet<TypeConstructorMarker>
) {
    type.contains { nestedType ->
        nestedType.typeConstructor().unwrapStubTypeVariableConstructor().let { nestedTypeConstructor ->
            if (nestedTypeConstructor.isTypeVariable()) {
                result.add(nestedTypeConstructor)
            }
        }
        false
    }
}

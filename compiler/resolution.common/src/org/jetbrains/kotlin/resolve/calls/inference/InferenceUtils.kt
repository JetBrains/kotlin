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
    additionalBindings: Map<TypeConstructorMarker, KotlinTypeMarker>,
    errorBindings: Map<TypeConstructorMarker, ErrorTypeMarker>,
): TypeSubstitutorMarker {
    val bindings = with(context) {
        val result = mutableMapOf<TypeConstructorMarker, KotlinTypeMarker>()
        for ((variableConstructor, type) in additionalBindings) {
            val errorType = errorBindings[variableConstructor]
            result[variableConstructor] = if (errorType == null) type else type.addErrorComponent(errorType)
        }
        for ((variableConstructor, type) in fixedTypeVariables) {
            val errorType = errorBindings[variableConstructor]
            result[variableConstructor] = if (errorType == null) type else type.addErrorComponent(errorType)
        }
        result
    }
    return context.typeSubstitutorByTypeConstructor(bindings)
}

fun ConstraintStorage.buildAbstractResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): TypeSubstitutorMarker = with(context) {
    if (allTypeVariables.isEmpty()) return createEmptySubstitutor()

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
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables + uninferredSubstitutorMap)
}

fun ConstraintStorage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(
    context: TypeSystemInferenceExtensionContext
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(
        notFixedTypeVariables.mapValues { context.createStubTypeForTypeVariablesInSubtyping(it.value.typeVariable) }
    )
}

fun TypeSystemInferenceExtensionContext.hasRecursiveTypeParametersWithGivenSelfType(selfTypeConstructor: TypeConstructorMarker): Boolean {
    if (selfTypeConstructor.getParameters().any { it.hasRecursiveBounds(selfTypeConstructor) }) return true
    if (!isK2) return false

    if (selfTypeConstructor is CapturedTypeConstructorMarker || selfTypeConstructor.isIntersection()) {
        return selfTypeConstructor.supertypes().any {
            hasRecursiveTypeParametersWithGivenSelfType(it.typeConstructor())
        }
    }

    return false
}

fun TypeSystemInferenceExtensionContext.isRecursiveTypeParameter(typeConstructor: TypeConstructorMarker) =
    typeConstructor.getTypeParameterClassifier()?.hasRecursiveBounds() == true

fun TypeSystemInferenceExtensionContext.extractTypeForGivenRecursiveTypeParameter(
    type: KotlinTypeMarker,
    typeParameter: TypeParameterMarker
): KotlinTypeMarker? {
    for (argument in type.getArguments()) {
        val argumentType = argument.getType() ?: continue
        val typeConstructor = argumentType.typeConstructor()
        if (typeConstructor is TypeVariableTypeConstructorMarker
            && typeConstructor.typeParameter == typeParameter
            && typeConstructor.typeParameter?.hasRecursiveBounds(type.typeConstructor()) == true
        ) {
            return type
        }
        extractTypeForGivenRecursiveTypeParameter(argumentType, typeParameter)?.let { return it }
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

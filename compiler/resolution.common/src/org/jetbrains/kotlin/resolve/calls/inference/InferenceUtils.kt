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
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables + additionalBindings)
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

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.hasRecursiveTypeParametersWithGivenSelfType(): Boolean {
    if (getParameters().any { it.hasRecursiveBounds(this) }) return true
    if (!c.isK2) return false

    if (this is CapturedTypeConstructorMarker || this.isIntersection()) {
        return supertypes().any {
            it.typeConstructor().hasRecursiveTypeParametersWithGivenSelfType()
        }
    }

    return false
}

context(c: TypeSystemInferenceExtensionContext)
fun TypeConstructorMarker.isRecursiveTypeParameter() =
    getTypeParameterClassifier()?.hasRecursiveBounds() == true

context(context: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.extractTypeForGivenRecursiveTypeParameter(typeParameter: TypeParameterMarker): KotlinTypeMarker? {
    for (argument in getArguments()) {
        val argumentType = argument.getType() ?: continue
        val typeConstructor = argumentType.typeConstructor()
        if (typeConstructor is TypeVariableTypeConstructorMarker
            && typeConstructor.typeParameter == typeParameter
            && typeConstructor.typeParameter?.hasRecursiveBounds(typeConstructor()) == true
        ) {
            return this
        }
        argumentType.extractTypeForGivenRecursiveTypeParameter(typeParameter)?.let { return it }
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

context(c: TypeSystemInferenceExtensionContext)
fun KotlinTypeMarker.extractAllContainingTypeVariables(): Set<TypeConstructorMarker> = buildSet {
    extractAllContainingTypeVariablesNoCaptureTypeProcessing(this)

    val typeProjections = c.extractProjectionsForAllCapturedTypes(this@extractAllContainingTypeVariables)

    typeProjections.forEach { typeProjectionsType ->
        typeProjectionsType.extractAllContainingTypeVariablesNoCaptureTypeProcessing(this)
    }
}

context(context: TypeSystemInferenceExtensionContext)
private fun KotlinTypeMarker.extractAllContainingTypeVariablesNoCaptureTypeProcessing(result: MutableSet<TypeConstructorMarker>) {
    contains { nestedType ->
        nestedType.typeConstructor().unwrapStubTypeVariableConstructor().let { nestedTypeConstructor ->
            if (nestedTypeConstructor.isTypeVariable()) {
                result.add(nestedTypeConstructor)
            }
        }
        false
    }
}

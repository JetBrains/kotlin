/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext) = with(c) { freshTypeConstructor() }
fun TypeSubstitutorMarker.safeSubstitute(
    c: TypeSystemInferenceExtensionContext,
    type: KotlinTypeMarker
): KotlinTypeMarker = with(c) { safeSubstitute(type) }

fun TypeVariableMarker.defaultType(c: TypeSystemInferenceExtensionContext): SimpleTypeMarker = with(c) { defaultType() }

fun KotlinTypeMarker.dependsOnTypeConstructor(c: TypeSystemInferenceExtensionContext, typeConstructors: Set<TypeConstructorMarker>): Boolean =
    with(c) {
        contains { it.typeConstructor() in typeConstructors }
    }

fun KotlinTypeMarker.dependsOnTypeParameters(c: TypeSystemInferenceExtensionContext, typeParameters: Collection<TypeParameterMarker>): Boolean =
    with(c) {
        val typeConstructors = typeParameters.mapTo(mutableSetOf()) { it.getTypeConstructor() }
        dependsOnTypeConstructor(c, typeConstructors)
    }

fun CapturedTypeMarker.captureStatus(c: TypeSystemInferenceExtensionContext): CaptureStatus =
    with(c) {
        captureStatus()
    }

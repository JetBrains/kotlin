/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext) = with(c) { freshTypeConstructor() }
fun TypeSubstitutorMarker.safeSubstitute(
    c: TypeSystemInferenceExtensionContext,
    type: KotlinTypeMarker
) = with(c) { safeSubstitute(type) }

fun TypeVariableMarker.defaultType(c: TypeSystemInferenceExtensionContext) = with(c) { defaultType() }
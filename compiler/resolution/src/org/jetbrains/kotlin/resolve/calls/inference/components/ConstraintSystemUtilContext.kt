/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

/*
 * Functions from this context can not be moved to TypeSystemInferenceExtensionContext, because
 *   it's classic implementation, ClassicTypeSystemContext lays in :core:descriptors,
 *   but we need access classes from :compiler:resolution for this function implementation
 */
interface ConstraintSystemUtilContext {
    fun TypeVariableMarker.shouldBeFlexible(): Boolean
    fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean
    fun KotlinTypeMarker.unCapture(): KotlinTypeMarker
    fun TypeVariableMarker.isReified(): Boolean
    fun KotlinTypeMarker.refineType(): KotlinTypeMarker
}

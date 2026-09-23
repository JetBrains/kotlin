/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation

fun ValueClassRepresentation<*>?.isKotlinValhallaValueClass(languageVersionSettings: LanguageVersionSettings): Boolean = when (this) {
    is InlineClassRepresentation<*>, is FullValueClassRepresentation<*> -> languageVersionSettings.isValhallaSupportEnabled()
    null -> false
}

/**
 * Whether every value class is compiled to and behaves as a Project Valhalla value class, selected via `-Xvalhalla-value-classes`.
 *
 * Requires a Valhalla-compatible JDK: when disabled, no declaration is compiled as a Valhalla value class and regular JVM bytecode
 * is generated.
 */
fun LanguageVersionSettings.isValhallaSupportEnabled(): Boolean = getFlag(JvmAnalysisFlags.valhallaValueClasses)

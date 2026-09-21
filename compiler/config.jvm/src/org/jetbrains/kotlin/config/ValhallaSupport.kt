/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation

/**
 * Whether value classes are compiled to Project Valhalla value classes (`-Xvalhalla-support`).
 *
 * Valhalla value classes are an experimental JVM feature, so this requires a Valhalla-compatible JDK. When the flag is disabled
 * (the default), value classes are compiled to regular JVM bytecode instead.
 */
fun LanguageVersionSettings.isValhallaSupportEnabled(): Boolean = getFlag(JvmAnalysisFlags.valhallaSupport)

/**
 * Whether the declaration with this value class representation is compiled to and behaves as a Valhalla value class.
 *
 * Every Kotlin value class is compiled as a Valhalla value class once [isValhallaSupportEnabled] is `true`.
 */
fun ValueClassRepresentation<*>?.isKotlinValhallaValueClass(languageVersionSettings: LanguageVersionSettings): Boolean = when (this) {
    is InlineClassRepresentation<*>, is FullValueClassRepresentation<*> -> languageVersionSettings.isValhallaSupportEnabled()
    null -> false
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation

/**
 * Whether the declaration with this value class representation is compiled to and behaves as a JVM value class.
 *
 * Every Kotlin value class is compiled as a JVM value class once support for them is enabled via `-Xjvm-value-classes`.
 */
fun ValueClassRepresentation<*>?.isCompiledAsJvmValueClass(languageVersionSettings: LanguageVersionSettings): Boolean = when (this) {
    is InlineClassRepresentation<*>, is FullValueClassRepresentation<*> -> languageVersionSettings.areJvmValueClassesEnabled()
    null -> false
}

/**
 * Whether value classes are compiled to JVM value classes (`-Xjvm-value-classes`).
 *
 * JVM value classes are an experimental JVM feature, so this requires a JDK that supports them.
 */
fun LanguageVersionSettings.areJvmValueClassesEnabled(): Boolean = getFlag(JvmAnalysisFlags.jvmValueClasses)

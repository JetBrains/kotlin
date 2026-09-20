/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.config.JvmValueClassCodegenMode.EFFICIENT
import org.jetbrains.kotlin.config.JvmValueClassCodegenMode.REGULAR
import org.jetbrains.kotlin.descriptors.FullValueClassRepresentation
import org.jetbrains.kotlin.descriptors.InlineClassRepresentation
import org.jetbrains.kotlin.descriptors.ValueClassRepresentation

/**
 * Controls how value classes are compiled on the JVM, selected via `-Xjvm-value-class-codegen`.
 *
 * The mode has to match the capabilities of the target JDK: [REGULAR] means the JDK is not Valhalla-compatible, so no declaration is
 * compiled as a Valhalla value class and regular JVM bytecode is generated. [EFFICIENT] compiles every value class as a Project
 * Valhalla value class and therefore requires a Valhalla-compatible JDK.
 */
enum class JvmValueClassCodegenMode(val description: String) {
    /** The target JDK is not Valhalla-compatible: no declaration is compiled as a Valhalla value class. This is the default. */
    REGULAR("regular"),

    /** Every value class is compiled to and behaves as a Valhalla value class. */
    EFFICIENT("efficient");

    companion object {
        @JvmStatic
        fun fromStringOrNull(string: String?): JvmValueClassCodegenMode? = entries.find { it.description == string }
    }
}

fun ValueClassRepresentation<*>?.isKotlinValhallaValueClass(languageVersionSettings: LanguageVersionSettings): Boolean = when (this) {
    is InlineClassRepresentation<*>, is FullValueClassRepresentation<*> -> languageVersionSettings.isValhallaSupportEnabled()
    null -> false
}

fun LanguageVersionSettings.isValhallaSupportEnabled(): Boolean = when (jvmValueClassCodegenMode) {
    REGULAR, null -> false
    EFFICIENT -> true
}

val LanguageVersionSettings.jvmValueClassCodegenMode: JvmValueClassCodegenMode?
    get() = getFlag(JvmAnalysisFlags.jvmValueClassCodegen)

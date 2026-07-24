/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

/**
 * Controls that declarations are compiled to and behave as Project Valhalla value classes, selected via `-Xvalhalla-support`.
 *
 * The mode has to match the capabilities of the target JDK: [NONE] means the JDK is not Valhalla-compatible, so no declaration is
 * compiled as a Valhalla value class and regular JVM bytecode is generated. Any other mode requires a Valhalla-compatible JDK and
 * selects a progressively larger set of declarations to compile as Valhalla value classes: [PRIMITIVES],
 * [PRIMITIVES_AND_FULL_VALUE_CLASSES], and [ALL_VALUES].
 */
enum class ValhallaSupportMode(val description: String) {
    /** The target JDK is not Valhalla-compatible: no declaration is compiled as a Valhalla value class. This is the default. */
    NONE("none"),

    /** Only built-in primitives are compiled to and behave as Valhalla value classes. */
    PRIMITIVES("primitives"),

    /** Built-in primitives and full value classes are compiled to and behave as Valhalla value classes. */
    PRIMITIVES_AND_FULL_VALUE_CLASSES("primitivesAndFullValueClasses"),

    /** Built-in primitives, full value classes, and inline value classes are compiled to and behave as Valhalla value classes. */
    ALL_VALUES("allValues");

    companion object {
        @JvmStatic
        fun fromStringOrNull(string: String?): ValhallaSupportMode? = entries.find { it.description == string }
    }
}

val LanguageVersionSettings.valhallaSupportMode: ValhallaSupportMode?
    get() = getFlag(JvmAnalysisFlags.valhallaSupport)

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

/**
 * Represents a mode fore debugging tests. Depending on what is tested, can represent different things.
 * For example, in JS IR and WASM tests the selected mode will determine if the test output should include IR dumps.
 */
enum class DebugMode {
    NONE,
    DEBUG,
    SUPER_DEBUG;

    companion object {

        /**
         * Obtains a [DebugMode] from the system property with the given [key].
         * If the property is not defined, returns [NONE].
         */
        fun fromSystemProperty(key: String): DebugMode = when (System.getProperty(key)) {
            "2", "super_debug" -> SUPER_DEBUG
            "1", "true", "debug" -> DEBUG
            "0", "false", "", null -> NONE
            else -> NONE
        }
    }
}

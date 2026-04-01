/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

/**
 * Overrides the default severity of a warning
 */
enum class WarningLevel(val cliOption: String) {
    Error("error"), Warning("warning"), Disabled("disabled");

    companion object {
        fun fromString(value: String): WarningLevel? = entries.firstOrNull { it.cliOption == value }
    }
}

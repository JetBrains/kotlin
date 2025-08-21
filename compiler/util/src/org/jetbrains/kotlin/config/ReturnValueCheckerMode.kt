/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class ReturnValueCheckerMode(val state: String) {
    DISABLED("disable"),
    CHECKER("check"),
    FULL("full");

    companion object {
        fun fromString(string: String): ReturnValueCheckerMode? = entries.find { it.state == string }

        fun availableValues() = entries.joinToString(prefix = "{", postfix = "}") { it.state }
    }
}


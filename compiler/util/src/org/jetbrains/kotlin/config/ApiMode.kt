/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class ApiMode(val state: String) {
    DISABLED("disable"),
    ENABLED("enable"),
    MIGRATION("migration");

    companion object {
        fun fromString(string: String): ApiMode? = values().find { it.state == string }

        fun availableValues() = values().joinToString(prefix = "{", postfix = "}") { it.state }
    }
}
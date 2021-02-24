/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmDefaultMode(val description: String) {
    DISABLE("disable"),
    ENABLE("enable"),
    ENABLE_WITH_DEFAULT_IMPLS("compatibility"),
    ALL_COMPATIBILITY("all-compatibility"),
    ALL_INCOMPATIBLE("all");

    val isEnabled
        get() = this != DISABLE

    val isCompatibility
        get() = this == ENABLE_WITH_DEFAULT_IMPLS || this == ALL_COMPATIBILITY

    val forAllMethodsWithBody
        get() = this == ALL_COMPATIBILITY || this == ALL_INCOMPATIBLE

    companion object {
        @JvmField
        val DEFAULT = DISABLE

        @JvmStatic
        fun fromStringOrNull(string: String?) = values().find { it.description == string }
    }
}

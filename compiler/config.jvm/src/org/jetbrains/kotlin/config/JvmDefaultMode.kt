/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmDefaultMode(val description: String, val oldDescription: String) {
    DISABLE("disable", "disable"),
    ENABLE("enable", "all-compatibility"),
    NO_COMPATIBILITY("no-compatibility", "all");

    val isEnabled: Boolean
        get() = this != DISABLE

    companion object {
        @JvmStatic
        fun fromStringOrNull(string: String?): JvmDefaultMode? = when (string) {
            DISABLE.description -> DISABLE
            ENABLE.description -> ENABLE
            NO_COMPATIBILITY.description -> NO_COMPATIBILITY
            else -> null
        }

        @JvmStatic
        fun fromStringOrNullOld(string: String?): JvmDefaultMode? = when (string) {
            DISABLE.oldDescription -> DISABLE
            ENABLE.oldDescription -> ENABLE
            NO_COMPATIBILITY.oldDescription -> NO_COMPATIBILITY
            else -> null
        }
    }
}

val LanguageVersionSettings.jvmDefaultMode: JvmDefaultMode
    get() = getFlag(JvmAnalysisFlags.jvmDefaultMode)

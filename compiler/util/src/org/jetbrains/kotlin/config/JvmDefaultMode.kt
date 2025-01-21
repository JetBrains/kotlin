/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmDefaultMode(val oldDescription: String) {
    DISABLE("disable"),
    ALL_COMPATIBILITY("all-compatibility"),
    ALL("all");

    val isEnabled: Boolean
        get() = this != DISABLE

    companion object {
        @JvmStatic
        fun fromStringOrNullOld(string: String?): JvmDefaultMode? = when (string) {
            DISABLE.oldDescription -> DISABLE
            ALL_COMPATIBILITY.oldDescription -> ALL_COMPATIBILITY
            ALL.oldDescription -> ALL
            else -> null
        }
    }
}

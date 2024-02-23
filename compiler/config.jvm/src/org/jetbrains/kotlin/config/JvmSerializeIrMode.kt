/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmSerializeIrMode(val description: String) {
    NONE("none"),
    INLINE("inline"),
    ALL("all");

    companion object {
        @JvmStatic
        fun fromStringOrNull(string: String) = entries.find { it.description == string }

        @JvmStatic
        fun fromString(string: String) = fromStringOrNull(string) ?: NONE
    }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmWhenGenerationScheme(val description: String) {
    INLINE("inline"),
    INDY("indy");

    companion object {
        @JvmStatic
        fun fromString(string: String) = entries.find { it.description == string }
    }
}
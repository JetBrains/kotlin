/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmStringConcat(val description: String) {
    INLINE("inline"),
    INDY_WITH_CONSTANTS("indy-with-constants"), // makeConcatWithConstants
    INDY("indy"); // makeConcat

    val isDynamic
        get() = this != INLINE

    companion object {
        @JvmStatic
        fun fromString(string: String) = entries.find { it.description == string }
    }
}
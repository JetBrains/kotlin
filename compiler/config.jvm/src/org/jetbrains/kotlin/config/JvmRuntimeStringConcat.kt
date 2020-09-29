/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmRuntimeStringConcat {
    DISABLE,
    ENABLE, // makeConcatWithConstants
    INDY; // makeConcat

    val isDynamic
        get() = this != DISABLE

    companion object {
        @JvmStatic
        fun fromString(string: String) = values().find { it.name == string.toUpperCase() }
    }
}
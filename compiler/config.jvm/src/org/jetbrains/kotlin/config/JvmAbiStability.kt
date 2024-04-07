/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JvmAbiStability(val description: String) {
    STABLE("stable"),
    UNSTABLE("unstable"),
    ;

    companion object {
        fun fromStringOrNull(string: String?): JvmAbiStability? = entries.find { it.description == string }
    }
}

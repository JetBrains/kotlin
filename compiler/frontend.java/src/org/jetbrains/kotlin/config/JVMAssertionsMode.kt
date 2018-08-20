/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class JVMAssertionsMode(val description: String) {
    ALWAYS_ENABLE("always-enable"),
    ALWAYS_DISABLE("always-disable"),
    JVM("jvm"),
    LEGACY("legacy");

    companion object {
        @JvmField
        val DEFAULT = LEGACY

        @JvmStatic
        fun fromStringOrNull(string: String?) = values().find { it.description == string }

        @JvmStatic
        fun fromString(string: String?) = fromStringOrNull(string) ?: DEFAULT
    }
}
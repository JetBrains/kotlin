/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

enum class PartialLinkageLogLevel {
    INFO, WARNING, ERROR;

    companion object {
        val DEFAULT = WARNING

        fun resolveLogLevel(key: String): PartialLinkageLogLevel = when (key.uppercase()) {
            "INFO" -> INFO
            "WARNING" -> WARNING
            "ERROR" -> ERROR
            else -> error("Unknown partial linkage compile-time log level '$key'")
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

data class PartialLinkageConfig(val logLevel: PartialLinkageLogLevel) {
    companion object {
        val DEFAULT = PartialLinkageConfig(PartialLinkageLogLevel.DEFAULT)
    }
}

enum class PartialLinkageLogLevel {
    SILENT, INFO, WARNING, ERROR;

    companion object {
        val DEFAULT = SILENT

        fun resolveLogLevel(key: String): PartialLinkageLogLevel? =
            entries.firstOrNull { entry -> entry.name.equals(key, ignoreCase = true) }

        fun availableValues() = entries.joinToString(prefix = "{", postfix = "}") { it.name.lowercase() }
    }
}

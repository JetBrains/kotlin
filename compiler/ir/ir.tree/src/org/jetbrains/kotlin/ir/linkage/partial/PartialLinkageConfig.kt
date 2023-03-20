/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage.partial

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

data class PartialLinkageConfig(val isEnabled: Boolean, val logLevel: PartialLinkageLogLevel) {
    companion object {
        val DEFAULT = PartialLinkageConfig(false, PartialLinkageLogLevel.ERROR)

        val KEY = CompilerConfigurationKey.create<PartialLinkageConfig>("partial linkage configuration")
    }
}

enum class PartialLinkageLogLevel {
    INFO, WARNING, ERROR;

    companion object {
        val DEFAULT = WARNING

        fun resolveLogLevel(key: String): PartialLinkageLogLevel =
            values().firstOrNull { entry -> entry.name.equals(key, ignoreCase = true) }
                ?: error("Unknown partial linkage compile-time log level '$key'")
    }
}

val CompilerConfiguration.partialLinkageConfig: PartialLinkageConfig
    get() = this[PartialLinkageConfig.KEY] ?: PartialLinkageConfig.DEFAULT

fun CompilerConfiguration.setupPartialLinkageConfig(isEnabled: Boolean, logLevel: String?) {
    setupPartialLinkageConfig(
        PartialLinkageConfig(isEnabled, logLevel?.let(PartialLinkageLogLevel::resolveLogLevel) ?: PartialLinkageLogLevel.DEFAULT)
    )
}

fun CompilerConfiguration.setupPartialLinkageConfig(config: PartialLinkageConfig) {
    this.put(PartialLinkageConfig.KEY, config)
}

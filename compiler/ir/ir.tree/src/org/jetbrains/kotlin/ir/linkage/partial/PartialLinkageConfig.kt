/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.linkage.partial

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

data class PartialLinkageConfig(val mode: PartialLinkageMode, val logLevel: PartialLinkageLogLevel) {
    val isEnabled get() = mode.isEnabled

    companion object {
        val DEFAULT = PartialLinkageConfig(PartialLinkageMode.DEFAULT, PartialLinkageLogLevel.ERROR)

        val KEY = CompilerConfigurationKey.create<PartialLinkageConfig>("partial linkage configuration")
    }
}

// In the future the set of supported modes can be extended.
enum class PartialLinkageMode(val isEnabled: Boolean) {
    ENABLE(isEnabled = true), DISABLE(isEnabled = false);

    companion object {
        val DEFAULT = DISABLE // TODO: should be changed to `ENABLE` (KT-51447, KT-51443)

        fun resolveMode(key: String): PartialLinkageMode? =
            values().firstOrNull { entry -> key == entry.name.lowercase() }
    }
}

enum class PartialLinkageLogLevel {
    INFO, WARNING, ERROR;

    companion object {
        val DEFAULT = WARNING

        fun resolveLogLevel(key: String): PartialLinkageLogLevel? =
            values().firstOrNull { entry -> entry.name.equals(key, ignoreCase = true) }
    }
}

val CompilerConfiguration.partialLinkageConfig: PartialLinkageConfig
    get() = this[PartialLinkageConfig.KEY] ?: PartialLinkageConfig.DEFAULT

fun CompilerConfiguration.setupPartialLinkageConfig(
    mode: String?,
    logLevel: String?,
    compilerModeAllowsUsingPartialLinkage: Boolean,
    onWarning: (String) -> Unit,
    onError: (String) -> Unit
) {
    val resolvedMode = when {
        mode != null -> {
            val resolvedMode = PartialLinkageMode.resolveMode(mode) ?: return onError("Unknown partial linkage mode '$mode'")
            if (!compilerModeAllowsUsingPartialLinkage && resolvedMode.isEnabled) {
                onWarning("Current compiler configuration does not allow using partial linkage mode '$mode'. The partial linkage will be disabled.")
                PartialLinkageMode.DISABLE
            } else
                resolvedMode
        }
        !compilerModeAllowsUsingPartialLinkage -> PartialLinkageMode.DISABLE
        else -> PartialLinkageMode.DEFAULT
    }

    val resolvedLogLevel = if (logLevel != null)
        PartialLinkageLogLevel.resolveLogLevel(logLevel) ?: return onError("Unknown partial linkage compile-time log level '$logLevel'")
    else
        PartialLinkageLogLevel.DEFAULT

    setupPartialLinkageConfig(PartialLinkageConfig(resolvedMode, resolvedLogLevel))
}

fun CompilerConfiguration.setupPartialLinkageConfig(config: PartialLinkageConfig) {
    this.put(PartialLinkageConfig.KEY, config)
}

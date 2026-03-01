/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.config.*

val PARTIAL_LINKAGE_CONFIGURATION = CompilerConfigurationKey.create<PartialLinkageConfig>("PARTIAL_LINKAGE_CONFIGURATION")

val CompilerConfiguration.partialLinkageConfig: PartialLinkageConfig
    get() = this[PARTIAL_LINKAGE_CONFIGURATION] ?: PartialLinkageConfig.DEFAULT

fun CompilerConfiguration.setupPartialLinkageConfig(
    mode: String?,
    logLevel: String?,
    compilerModeAllowsUsingPartialLinkage: Boolean,
    onWarning: (String) -> Unit,
    onError: (String) -> Unit,
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
        PartialLinkageLogLevel.resolveLogLevel(logLevel)
            ?: return onError("Unknown partial linkage compile-time log level '$logLevel'")
    else
        PartialLinkageLogLevel.DEFAULT

    setupPartialLinkageConfig(PartialLinkageConfig(resolvedMode, resolvedLogLevel))
}

fun CompilerConfiguration.setupPartialLinkageConfig(config: PartialLinkageConfig) {
    this.put(PARTIAL_LINKAGE_CONFIGURATION, config)
}

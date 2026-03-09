/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.linkage.partial

import org.jetbrains.kotlin.cli.common.arguments.CommonKlibBasedCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory

val PARTIAL_LINKAGE_CONFIGURATION = CompilerConfigurationKey.create<PartialLinkageConfig>("PARTIAL_LINKAGE_CONFIGURATION")

val CompilerConfiguration.partialLinkageConfig: PartialLinkageConfig
    get() = this[PARTIAL_LINKAGE_CONFIGURATION] ?: PartialLinkageConfig.DEFAULT

fun CompilerConfiguration.setupPartialLinkageConfig(
    arguments: CommonKlibBasedCompilerArguments,
    warningDiagnosticFactory: KtSourcelessDiagnosticFactory,
    errorDiagnosticFactory: KtSourcelessDiagnosticFactory,
) {
    @Suppress("DEPRECATION")
    if (arguments.partialLinkageMode != null) {
        report(
            warningDiagnosticFactory,
            "The ${CommonKlibBasedCompilerArguments::partialLinkageMode.cliArgument} argument is deprecated. The partial linkage engine is always turned on."
        )
    }

    val logLevel = arguments.partialLinkageLogLevel?.let { rawLogLevel ->
        PartialLinkageLogLevel.resolveLogLevel(rawLogLevel)
            ?: return report(
                errorDiagnosticFactory,
                "Unknown value for parameter -Xpartial-linkage-loglevel: '$rawLogLevel'. Value should be one of ${PartialLinkageLogLevel.availableValues()}"
            )
    } ?: PartialLinkageLogLevel.DEFAULT

    setupPartialLinkageConfig(PartialLinkageConfig(PartialLinkageMode.ENABLE, logLevel))
}

fun CompilerConfiguration.setupPartialLinkageConfig(config: PartialLinkageConfig) {
    this.put(PARTIAL_LINKAGE_CONFIGURATION, config)
}

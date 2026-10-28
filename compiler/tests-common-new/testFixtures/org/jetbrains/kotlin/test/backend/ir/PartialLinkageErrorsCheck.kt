/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageIssueSignificance
import org.jetbrains.kotlin.backend.common.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.PartialLinkageLogLevel

/**
 * Fail the compilation if the partial linkage engine has reported any issues while the ERROR log level is configured
 * for partial linkage (the second-stage configurators of JS and Wasm tests do this to fail any test where partial linkage
 * detected any incompatibilities).
 *
 * The partial linkage engine reports its issues to the diagnostics collector and lets the compilation go on. The test facades
 * run the phases of the compiler pipeline one by one, so they need to check the reported issues explicitly. Otherwise, the test
 * would be run with the partially linked code. All issues are checked, whatever their severity is, just as the IR linker
 * used to abort the compilation on any logged partial linkage issue with the ERROR log level.
 */
fun checkNoPartialLinkageErrors(configuration: CompilerConfiguration) {
    if (configuration.partialLinkageConfig.logLevel != PartialLinkageLogLevel.ERROR) return

    val partialLinkageIssues = configuration.diagnosticsCollector.diagnostics.filter { it.factory in PARTIAL_LINKAGE_ISSUE_FACTORIES }
    if (partialLinkageIssues.isEmpty()) return

    throw PartialLinkageErrorsException(
        "There are linkage errors reported by the partial linkage engine:\n" +
                partialLinkageIssues.joinToString("\n") { it.renderMessage() }
    )
}

/**
 * Thrown by [checkNoPartialLinkageErrors]. Its message lists all the partial linkage issues.
 */
class PartialLinkageErrorsException(message: String) : RuntimeException(message)

private val PARTIAL_LINKAGE_ISSUE_FACTORIES = PartialLinkageIssueSignificance.entries.mapTo(hashSetOf()) { it.toDiagnosticFactory() }

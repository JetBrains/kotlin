/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CLI_COMPILER_PLUGIN_IS_EXPERIMENTAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CLI_NOT_AN_OPT_IN_REQUIREMENT_MARKER
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CLI_OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CLI_OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CLI_OPT_IN_REQUIREMENT_MARKER_IS_UNRESOLVED
import kotlin.getValue

object CliDiagnostics : KtDiagnosticsContainer() {
    val CLI_COMPILER_PLUGIN_IS_EXPERIMENTAL: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val CLI_OPT_IN_REQUIREMENT_MARKER_IS_UNRESOLVED: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val CLI_NOT_AN_OPT_IN_REQUIREMENT_MARKER: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val CLI_OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val CLI_OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDiagnosticMessagesCli
}

object KtDiagnosticMessagesCli : BaseSourcelessDiagnosticFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CLI") { map ->
        map.put(CLI_COMPILER_PLUGIN_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
        map.put(CLI_OPT_IN_REQUIREMENT_MARKER_IS_UNRESOLVED, MESSAGE_PLACEHOLDER)
        map.put(CLI_NOT_AN_OPT_IN_REQUIREMENT_MARKER, MESSAGE_PLACEHOLDER)
        map.put(CLI_OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED, MESSAGE_PLACEHOLDER)
        map.put(CLI_OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED_ERROR, MESSAGE_PLACEHOLDER)
    }
}
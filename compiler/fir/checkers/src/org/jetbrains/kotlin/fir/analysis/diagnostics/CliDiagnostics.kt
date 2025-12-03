/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CONTEXT_PARAMETERS_ARE_DEPRECATED
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.ERROR_SEVERITY_CHANGED
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.MISSING_DIAGNOSTIC_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.NOT_AN_OPT_IN_REQUIREMENT_MARKER
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED_ERROR
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.OPT_IN_REQUIREMENT_MARKER_IS_UNRESOLVED

object CliDiagnostics : KtDiagnosticsContainer() {
    val COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val OPT_IN_REQUIREMENT_MARKER_IS_UNRESOLVED: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val NOT_AN_OPT_IN_REQUIREMENT_MARKER: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val CONTEXT_PARAMETERS_ARE_DEPRECATED: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val MISSING_DIAGNOSTIC_NAME: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val ERROR_SEVERITY_CHANGED: KtSourcelessDiagnosticFactory by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDiagnosticMessagesCli
}

object KtDiagnosticMessagesCli : BaseSourcelessDiagnosticFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CLI") { map ->
        map.put(COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
        map.put(OPT_IN_REQUIREMENT_MARKER_IS_UNRESOLVED, MESSAGE_PLACEHOLDER)
        map.put(NOT_AN_OPT_IN_REQUIREMENT_MARKER, MESSAGE_PLACEHOLDER)
        map.put(OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED, MESSAGE_PLACEHOLDER)
        map.put(OPT_IN_REQUIREMENT_MARKER_IS_DEPRECATED_ERROR, MESSAGE_PLACEHOLDER)
        map.put(CONTEXT_PARAMETERS_ARE_DEPRECATED, MESSAGE_PLACEHOLDER)
        map.put(MISSING_DIAGNOSTIC_NAME, MESSAGE_PLACEHOLDER)
        map.put(ERROR_SEVERITY_CHANGED, MESSAGE_PLACEHOLDER)
    }
}

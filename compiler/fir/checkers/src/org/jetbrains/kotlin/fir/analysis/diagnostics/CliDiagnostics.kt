/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.warningWithoutSource
import org.jetbrains.kotlin.fir.analysis.diagnostics.CliDiagnostics.CLI_COMPILER_PLUGIN_IS_EXPERIMENTAL
import kotlin.getValue

object CliDiagnostics : KtDiagnosticsContainer() {
    val CLI_COMPILER_PLUGIN_IS_EXPERIMENTAL: KtSourcelessDiagnosticFactory by warningWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDiagnosticMessagesCli
}

object KtDiagnosticMessagesCli : BaseSourcelessDiagnosticFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CLI") { map ->
        map.put(CLI_COMPILER_PLUGIN_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
    }
}
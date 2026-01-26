/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.warningWithoutSource

object CliDiagnostics : KtDiagnosticsContainer() {
    val COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL: KtSourcelessDiagnosticFactory by warningWithoutSource()
    val REDUNDANT_CLI_ARG: KtSourcelessDiagnosticFactory by warningWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CLI") { map ->
            map.put(COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
            map.put(REDUNDANT_CLI_ARG, MESSAGE_PLACEHOLDER)
        }
    }
}

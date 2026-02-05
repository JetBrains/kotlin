/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.errorWithoutSource
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.strongWarningWithoutSource

object CliDiagnostics : KtDiagnosticsContainer() {
    val COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val REDUNDANT_CLI_ARG: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLASSPATH_RESOLUTION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val CLASSPATH_RESOLUTION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val JAVA_MODULE_RESOLUTION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()
    val ROOTS_RESOLUTION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val ROOTS_RESOLUTION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val COMPILER_PLUGIN_INITIALIZATION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val COMPILER_PLUGIN_INITIALIZATION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val INITIALIZATION_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    val COMPILER_ARGUMENTS_WARNING: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()
    val COMPILER_ARGUMENTS_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    val JAVAC_INTEGRATION_ERROR: KtSourcelessDiagnosticFactory by errorWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("CLI") { map ->
            map.put(COMPILER_PLUGIN_ARG_IS_EXPERIMENTAL, MESSAGE_PLACEHOLDER)
            map.put(REDUNDANT_CLI_ARG, MESSAGE_PLACEHOLDER)
            map.put(CLASSPATH_RESOLUTION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(CLASSPATH_RESOLUTION_ERROR, MESSAGE_PLACEHOLDER)
            map.put(JAVA_MODULE_RESOLUTION_ERROR, MESSAGE_PLACEHOLDER)
            map.put(ROOTS_RESOLUTION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(ROOTS_RESOLUTION_ERROR, MESSAGE_PLACEHOLDER)

            map.put(COMPILER_PLUGIN_INITIALIZATION_WARNING, MESSAGE_PLACEHOLDER)
            map.put(COMPILER_PLUGIN_INITIALIZATION_ERROR, MESSAGE_PLACEHOLDER)

            map.put(INITIALIZATION_WARNING, MESSAGE_PLACEHOLDER)



            map.put(COMPILER_ARGUMENTS_WARNING, MESSAGE_PLACEHOLDER)
            map.put(COMPILER_ARGUMENTS_ERROR, MESSAGE_PLACEHOLDER)

            map.put(JAVAC_INTEGRATION_ERROR, MESSAGE_PLACEHOLDER)
        }
    }
}

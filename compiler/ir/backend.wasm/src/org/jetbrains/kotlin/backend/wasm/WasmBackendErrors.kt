/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory.Companion.MESSAGE_PLACEHOLDER
import org.jetbrains.kotlin.diagnostics.strongWarningWithoutSource

object WasmBackendErrors : KtDiagnosticsContainer() {
    val WASM_BACKEND_MISSING_CUSTOM_FORMATTERS: KtSourcelessDiagnosticFactory by strongWarningWithoutSource()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultWasmErrorMessages
    }
}

object KtDefaultWasmErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(WasmBackendErrors.WASM_BACKEND_MISSING_CUSTOM_FORMATTERS, MESSAGE_PLACEHOLDER)
    }
}

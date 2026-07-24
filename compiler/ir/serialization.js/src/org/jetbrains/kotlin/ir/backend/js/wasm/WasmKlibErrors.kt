/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderer

context(container: KtDiagnosticsContainer)
private fun exportClashError() =
    error3<PsiElement, String, String, List<WasmKlibExportingDeclaration>>()

object WasmKlibErrors : KtDiagnosticsContainer() {

    val EXPORTING_JS_NAME_CLASH by exportClashError()

    val WASM_EXPORT_CLASH by exportClashError()

    val EXPORTING_JS_NAME_WASM_EXPORT_CLASH by exportClashError()

    val WASM_EXPORT_EXPORTING_JS_NAME_CLASH by exportClashError()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultWasmKlibErrorMessages
    }
}

private object KtDefaultWasmKlibErrorMessages : BaseDiagnosticRendererFactory() {
    @JvmField
    val KLIB_EXPORTS_LIST = Renderer<List<WasmKlibExportingDeclaration>> { exports ->
        if (exports.size == 1) {
            exports.single().render()
        } else {
            exports.sortedBy { it.containingFile }.joinToString("\n", "\n", limit = 10) { "    ${it.render()}" }
        }
    }

    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            WasmKlibErrors.EXPORTING_JS_NAME_CLASH,
            "JsExport exporting name ''{0}'' of ''{1}'' clashes with exporting name(s) of JsExport(s) {2}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
        map.put(
            WasmKlibErrors.WASM_EXPORT_CLASH,
            "WasmExport exporting name ''{0}'' of ''{1}'' clashes with exporting name(s) of WasmExport(s) {2}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
        map.put(
            WasmKlibErrors.EXPORTING_JS_NAME_WASM_EXPORT_CLASH,
            "JsExport exporting name ''{0}'' of ''{1}'' clashes with exporting name(s) of WasmExport(s) {2}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
        map.put(
            WasmKlibErrors.WASM_EXPORT_EXPORTING_JS_NAME_CLASH,
            "WasmExport exporting name ''{0}'' of ''{1}'' clashes with exporting name(s) of JsExport(s) {2}",
            CommonRenderers.STRING,
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
    }
}

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

object WasmKlibErrors : KtDiagnosticsContainer() {

    val EXPORTING_JS_NAME_CLASH by error2<PsiElement, String, List<WasmKlibExportingDeclaration>>()

    val WASM_EXPORT_CLASH by error2<PsiElement, String, List<WasmKlibExportingDeclaration>>()

    val EXPORTING_JS_NAME_WASM_EXPORT_CLASH by error2<PsiElement, String, List<WasmKlibExportingDeclaration>>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultJsKlibErrorMessages
    }
}

private object KtDefaultJsKlibErrorMessages : BaseDiagnosticRendererFactory() {
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
            "Exporting JsExport name ''{0}'' clashes with JsExport {1}",
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
        map.put(
            WasmKlibErrors.WASM_EXPORT_CLASH,
            "Exporting WasmExport name ''{0}'' clashes with WasmExport {1}",
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
        map.put(
            WasmKlibErrors.EXPORTING_JS_NAME_WASM_EXPORT_CLASH,
            "Exporting JsExport name ''{0}'' clashes with WasmExport {1}",
            CommonRenderers.STRING,
            KLIB_EXPORTS_LIST
        )
    }
}

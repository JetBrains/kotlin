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
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibExport

object WasmKlibErrors : KtDiagnosticsContainer() {

    val EXPORTING_JS_NAME_CLASH by error2<PsiElement, String, List<JsKlibExport>>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return KtDefaultJsKlibErrorMessages
    }
}

private object KtDefaultJsKlibErrorMessages : BaseDiagnosticRendererFactory() {
    @JvmField
    val JS_KLIB_EXPORTS = Renderer<List<JsKlibExport>> { exports ->
        if (exports.size == 1) {
            exports.single().render()
        } else {
            exports.sortedBy { it.containingFile }.joinToString("\n", "\n", limit = 10) { "    ${it.render()}" }
        }
    }

    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            WasmKlibErrors.EXPORTING_JS_NAME_CLASH,
            "Exporting name ''{0}'' clashes with {1}",
            CommonRenderers.STRING,
            JS_KLIB_EXPORTS
        )
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.error2
import org.jetbrains.kotlin.diagnostics.rendering.*
import org.jetbrains.kotlin.diagnostics.warning0
import org.jetbrains.kotlin.diagnostics.warning1
import org.jetbrains.kotlin.diagnostics.warning2

object JsKlibErrors {
    val EXPORTING_JS_NAME_CLASH by error2<PsiElement, String, List<JsKlibExport>>()
    val EXPORTING_JS_NAME_CLASH_ES by warning2<PsiElement, String, List<JsKlibExport>>()

    val JSCODE_CAN_NOT_VERIFY_JAVASCRIPT by warning0<PsiElement>()
    val JSCODE_NO_JAVASCRIPT_PRODUCED by error0<PsiElement>()
    val JSCODE_ERROR by error1<PsiElement, String>()
    val JSCODE_WARNING by warning1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultJsKlibErrorMessages)
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

    override val MAP = KtDiagnosticFactoryToRendererMap("KT").also { map ->
        map.put(
            JsKlibErrors.EXPORTING_JS_NAME_CLASH,
            "Exporting name ''{0}'' clashes with {1}",
            CommonRenderers.STRING,
            JS_KLIB_EXPORTS
        )
        map.put(
            JsKlibErrors.EXPORTING_JS_NAME_CLASH_ES,
            "Exporting name ''{0}'' in ES modules may clash with {1}",
            CommonRenderers.STRING,
            JS_KLIB_EXPORTS,
        )
        map.put(
            JsKlibErrors.JSCODE_CAN_NOT_VERIFY_JAVASCRIPT,
            "Cannot verify JavaScript code because the argument is not a constant string"
        )
        map.put(
            JsKlibErrors.JSCODE_NO_JAVASCRIPT_PRODUCED,
            "An argument for the js() function must be non-empty JavaScript code"
        )
        map.put(
            JsKlibErrors.JSCODE_ERROR,
            "JavaScript error: {0}",
            CommonRenderers.STRING
        )
        map.put(
            JsKlibErrors.JSCODE_WARNING,
            "JavaScript warning: {0}",
            CommonRenderers.STRING
        )
    }
}

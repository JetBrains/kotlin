/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.ir.IrDiagnosticRenderers
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration

object JsKlibErrors : KtDiagnosticsContainer() {
    val CLASHED_FILES_IN_CASE_INSENSITIVE_FS by error1<PsiElement, List<IrFileEntry>>()

    val EXPORTING_JS_NAME_CLASH by error2<PsiElement, String, List<JsKlibExport>>()
    val EXPORTING_JS_NAME_CLASH_ES by warning2<PsiElement, String, List<JsKlibExport>>()

    val JSCODE_CAN_NOT_VERIFY_JAVASCRIPT by warning0<PsiElement>()
    val JSCODE_NO_JAVASCRIPT_PRODUCED by error0<PsiElement>()
    val JSCODE_ERROR by error1<PsiElement, String>()
    val JSCODE_WARNING by warning1<PsiElement, String>()
    val JS_CODE_CAPTURES_INLINABLE_FUNCTION by deprecationError1<PsiElement, IrValueDeclaration>(
        LanguageFeature.ForbidCaptureInlinableLambdasInJsCode
    )

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

    @JvmField
    val JS_CLASHED_FILES = Renderer<List<IrFileEntry>> { files ->
        files.joinToString("\n", limit = 10) { "    ${it.name}" }
    }

    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            JsKlibErrors.CLASHED_FILES_IN_CASE_INSENSITIVE_FS,
            "The file has the same package and name (or @JsFileName value) but different casing as the following files:\n{0}\nThis may cause issues in case-insensitive filesystems. To fix it, consider renaming file, adding @JsFileName annotation or changing its package.",
            JS_CLASHED_FILES
        )
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
        map.put(
            JsKlibErrors.JS_CODE_CAPTURES_INLINABLE_FUNCTION,
            "Illegal capturing of inline parameter ''{0}''. Add ''noinline'' modifier to the parameter declaration",
            IrDiagnosticRenderers.DECLARATION_NAME
        )
    }
}

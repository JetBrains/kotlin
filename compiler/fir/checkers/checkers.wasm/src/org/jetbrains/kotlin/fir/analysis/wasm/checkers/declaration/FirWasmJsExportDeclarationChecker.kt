/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.getAnnotationFirstArgument
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.checkNameExportedToJs
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.isJsExportedDeclaration
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.unsupportedExportedFunctionKind
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

/**
 * Reports functions that are marked with `@JsExport` but cannot be exported from a Kotlin/Wasm module.
 *
 * Kotlin/Wasm exports only top-level functions, so this is the subset of `FirJsExportDeclarationChecker`
 * applicable to functions; the types of exported functions are checked by [FirWasmJsInteropTypesChecker].
 *
 * Kotlin/Wasm also adds restrictions on extension functions (except companion extensions) and functions with vararg parameters.
 */
object FirWasmJsExportDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Platform) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration !is FirNamedFunction || !context.isTopLevel || !isJsExportedDeclaration(declaration, context.session)) {
            return
        }

        fun reportWrongExportedDeclaration(kind: String) {
            reporter.reportOn(declaration.source, FirWebCommonErrors.WRONG_EXPORTED_DECLARATION, kind)
        }

        val unsupportedKind = declaration.unsupportedExportedFunctionKind(context.session)
        when {
            unsupportedKind != null -> reportWrongExportedDeclaration(unsupportedKind)
            declaration.valueParameters.any { it.isVararg } -> reportWrongExportedDeclaration("function with vararg parameter")
        }

        checkExportedName(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExportedName(declaration: FirNamedFunction) {

        // already reported FUNCTION_DECLARATION_WITH_NO_NAME.
        if (declaration.nameOrSpecialName.isSpecial) {
            return
        }

        val jsNameArgument = declaration.symbol.getAnnotationFirstArgument(WebCommonStandardClassIds.Annotations.JsName, context.session)
        val name = (jsNameArgument as? FirLiteralExpression)?.value as? String ?: declaration.name.asString()

        checkNameExportedToJs(name, jsNameArgument?.source ?: declaration.source)
    }
}

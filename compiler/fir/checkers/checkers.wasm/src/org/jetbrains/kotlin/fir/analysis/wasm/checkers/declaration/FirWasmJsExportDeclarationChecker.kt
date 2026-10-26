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
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.isJsExportedDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInstanceExtension
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.name.WebCommonStandardClassIds
import org.jetbrains.kotlin.wasm.WasmSpecialExportNames

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

        when {
            declaration.symbol.isEffectivelyExternal(context.session) -> reportWrongExportedDeclaration("external function")
            declaration.isInline && declaration.typeParameters.any { it.symbol.isReified } ->
                reportWrongExportedDeclaration("inline function with reified type parameters")
            declaration.isSuspend -> reportWrongExportedDeclaration("suspend function")
            declaration.isInstanceExtension -> reportWrongExportedDeclaration("extension function")
            declaration.valueParameters.any { it.isVararg } -> reportWrongExportedDeclaration("function with vararg parameter")
        }

        checkExportedName(declaration)
    }

    // Names that are not valid identifiers are exported as string literals, but reserved words are exported as variable names,
    // which makes the generated JS module invalid.
    // Names used by Kotlin/Wasm for its own exports are dropped from the generated JS module.
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExportedName(declaration: FirNamedFunction) {

        // already reported FUNCTION_DECLARATION_WITH_NO_NAME.
        if (declaration.nameOrSpecialName.isSpecial) {
            return
        }

        val jsNameArgument = declaration.symbol.getAnnotationFirstArgument(WebCommonStandardClassIds.Annotations.JsName, context.session)
        val name = (jsNameArgument as? FirLiteralExpression)?.value as? String ?: declaration.name.asString()

        val source = jsNameArgument?.source ?: declaration.source
        when {
            name in RESERVED_KEYWORDS -> reporter.reportOn(source, FirWasmErrors.RESERVED_WORD_AS_JS_EXPORT_NAME, name)
            WasmSpecialExportNames.isInternal(name) ->
                reporter.reportOn(source, FirWasmErrors.INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME, name)
        }
    }
}

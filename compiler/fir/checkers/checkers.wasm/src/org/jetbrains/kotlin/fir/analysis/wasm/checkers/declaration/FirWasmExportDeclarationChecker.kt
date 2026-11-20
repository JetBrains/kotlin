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
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.checkNameExportedToJs
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.hasValidJsCodeBody
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.unsupportedExportedFunctionKind
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.name.WasmStandardClassIds

/**
 * Reports functions that are marked with `@WasmExport` but cannot be exported from a Kotlin/Wasm module.
 */
sealed class FirWasmExportDeclarationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    object Common : FirWasmExportDeclarationChecker() {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun checkExportedFunction(declaration: FirNamedFunction) {
            declaration.unsupportedExportedFunctionKind(context.session)?.let { kind ->
                reporter.reportOn(declaration.source, FirWasmErrors.WASM_EXPORT_UNSUPPORTED_FUNCTION_KIND, kind)
            }
        }
    }

    object WasmJs : FirWasmExportDeclarationChecker() {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun checkExportedFunction(declaration: FirNamedFunction) {
            // already reported FUNCTION_DECLARATION_WITH_NO_NAME.
            if (declaration.name.isSpecial) return

            val nameArgument = declaration.symbol.getAnnotationFirstArgument(WasmStandardClassIds.Annotations.WasmExport, context.session)
            val name = (nameArgument as? FirLiteralExpression)?.value as? String ?: declaration.name.asString()

            checkNameExportedToJs(name, nameArgument?.source ?: declaration.source)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected abstract fun checkExportedFunction(declaration: FirNamedFunction)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration !is FirNamedFunction || !context.isTopLevel) return
        if (!declaration.hasAnnotation(WasmStandardClassIds.Annotations.WasmExport, context.session)) return

        // already reported WASM_EXPORT_ON_EXTERNAL_DECLARATION.
        if (declaration.symbol.isEffectivelyExternal(context.session) || declaration.hasValidJsCodeBody()) return

        checkExportedFunction(declaration)
    }
}

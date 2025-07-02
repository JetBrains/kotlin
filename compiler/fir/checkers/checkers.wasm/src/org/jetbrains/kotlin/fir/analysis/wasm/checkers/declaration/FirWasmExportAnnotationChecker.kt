/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.hasValidJsCodeBody
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.WasmStandardClassIds
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmExportAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val wasmExportAnnotation =
            declaration.annotations.getAnnotationByClassId(WasmStandardClassIds.Annotations.WasmExport, context.session)

        if (wasmExportAnnotation != null) {
            checkWasmExported(declaration, wasmExportAnnotation)
        }

        if (declaration.annotations.hasAnnotation(WebCommonStandardClassIds.Annotations.JsExport, context.session)) {
            checkJsExported(declaration, wasmExportAnnotation != null)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkWasmExported(declaration: FirDeclaration, wasmExportAnnotation: FirAnnotation) {
        if (!context.isTopLevel) {
            reporter.reportOn(wasmExportAnnotation.source, FirWasmErrors.NESTED_WASM_EXPORT)
        }

        if (declaration is FirSimpleFunction) {
            if (declaration.symbol.isEffectivelyExternal(context.session) || declaration.hasValidJsCodeBody()) {
                reporter.reportOn(wasmExportAnnotation.source, FirWasmErrors.WASM_EXPORT_ON_EXTERNAL_DECLARATION)
            }
            if (LanguageFeature.ContextParameters.isEnabled()) {
                if (declaration.contextParameters.isNotEmpty()) {
                    reporter.reportOn(declaration.source, FirWasmErrors.EXPORT_DECLARATION_WITH_CONTEXT_PARAMETERS)
                }
            }
            checkWasmInteropSignature(declaration)
        }
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkJsExported(declaration: FirDeclaration, isWasmExported: Boolean) {
        if (isWasmExported) {
            reporter.reportOn(declaration.source, FirWasmErrors.JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION)
        }

        if (context.isTopLevel && declaration is FirSimpleFunction) {
            val sourceSymbolProvider = context.session.firProvider.symbolProvider
            sourceSymbolProvider.getTopLevelCallableSymbolsWithSimpleName(declaration.name)?.forEach {
                if (it != declaration.symbol
                    && it is FirNamedFunctionSymbol
                    && it.hasAnnotation(WebCommonStandardClassIds.Annotations.JsExport, context.session)
                ) {
                    reporter.reportOn(declaration.source, FirWebCommonErrors.JS_EXPORT_CLASH, it)
                }
            }
        }
    }
}

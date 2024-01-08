/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.hasValidJsCodeBody
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.WasmStandardClassIds
import org.jetbrains.kotlin.name.WebCommonStandardClassIds

object FirWasmExportAnnotationChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val annotation: FirAnnotation =
            declaration.annotations.getAnnotationByClassId(WasmStandardClassIds.Annotations.WasmExport, context.session) ?: return

        if (!context.isTopLevel) {
            reporter.reportOn(annotation.source, FirWasmErrors.NESTED_WASM_EXPORT, context)
        }

        if (declaration.annotations.hasAnnotation(WebCommonStandardClassIds.Annotations.JsExport, context.session)) {
            reporter.reportOn(declaration.source, FirWasmErrors.JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION, context)
        }

        if (declaration is FirSimpleFunction) {
            if (declaration.symbol.isEffectivelyExternal(context.session) || declaration.hasValidJsCodeBody()) {
                reporter.reportOn(annotation.source, FirWasmErrors.WASM_EXPORT_ON_EXTERNAL_DECLARATION, context)
            }

            checkWasmInteropSignature(declaration, context, reporter)
        }
    }
}

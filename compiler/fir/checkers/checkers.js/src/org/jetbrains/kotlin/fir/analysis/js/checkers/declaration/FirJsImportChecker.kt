/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.closestNonLocalWith
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.isNativeObject
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsImport
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsNonModule
import org.jetbrains.kotlin.name.JsStandardClassIds.Annotations.JsQualifier

object FirJsImportChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    private val annotationsForbiddenToUseWithJsImport = setOf(
        JsModule.asSingleFqName(),
        JsNonModule.asSingleFqName(),
        JsQualifier.asSingleFqName()
    )

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasAnnotation(JsImport, context.session)) return

        if (declaration is FirProperty && declaration.isVar) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_PROHIBITED_ON_VAR, context)
        }

        val closestNonLocal = context.closestNonLocalWith(declaration)?.symbol ?: return

        if (!closestNonLocal.isNativeObject(context)) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_PROHIBITED_ON_NON_NATIVE, context)
        }

        if (context.isTopLevel && context.containingFile?.hasAnnotation(JsImport, context.session) == true) {
            reporter.reportOn(declaration.source, FirJsErrors.NESTED_JS_IMPORT_PROHIBITED, context)
        }

        if (
            declaration.hasAnnotation(JsModule, context.session) ||
            context.containingFile?.annotations?.any { it.fqName(context.session) in annotationsForbiddenToUseWithJsImport } == true
        ) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_IMPORT_AND_JS_MODULE_MIX, context)
        }
    }
}

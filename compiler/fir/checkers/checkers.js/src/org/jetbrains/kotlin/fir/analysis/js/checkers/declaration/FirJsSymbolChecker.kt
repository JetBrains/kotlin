/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.name.JsStandardClassIds
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.declarations.utils.isOverride

/**
 * Checks that @JsSymbol is only applied to member declarations (not to top-level declarations).
 */
object FirJsSymbolChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        val annotation = declaration.getAnnotationByClassId(JsStandardClassIds.Annotations.JsSymbol, context.session) ?: return

        if (context.isTopLevel) {
            reporter.reportOn(declaration.source, FirJsErrors.JS_SYMBOL_ON_TOP_LEVEL_DECLARATION)
            return
        }

        val jsSymbolSource = annotation.source ?: declaration.source

        if (
            declaration is FirCallableDeclaration && declaration.isOverride ||
            declaration is FirPropertyAccessor && declaration.propertySymbol.isOverride
        ) {
            reporter.reportOn(jsSymbolSource, FirJsErrors.JS_SYMBOL_PROHIBITED_FOR_OVERRIDE)
        }
    }
}

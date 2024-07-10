/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.FirJsStableName
import org.jetbrains.kotlin.fir.analysis.js.checkers.getJsName
import org.jetbrains.kotlin.fir.analysis.js.checkers.isExportedObject
import org.jetbrains.kotlin.fir.analysis.js.checkers.sanitizeName
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.impl.FirPrimaryConstructor

object FirJsNameCharsChecker : FirBasicDeclarationChecker() {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping)) {
            return
        }

        if ((declaration is FirPropertyAccessor || declaration is FirPrimaryConstructor) &&
            declaration.symbol.getJsName(context.session) == null
        ) {
            return
        }

        if (declaration is FirConstructor &&
            declaration.symbol.getJsName(context.session) == null &&
            declaration.symbol.isExportedObject(context.session)
        ) {
            return
        }

        val stableName = FirJsStableName.createStableNameOrNull(declaration.symbol, context.session) ?: return
        if ((sanitizeName(stableName.name) != stableName.name)) {
            reporter.reportOn(declaration.source, FirJsErrors.NAME_CONTAINS_ILLEGAL_CHARS, context)
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.js.checkers.declaration

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFileChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.forEachChildOfType
import org.jetbrains.kotlin.fir.analysis.js.checkers.sanitizeName
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.text

object FirJsPackageDirectiveChecker: FirFileChecker(MppCheckerKind.Common) {
    // inspired by FirJsNameCharsChecker.check()
    override fun check(declaration: FirFile, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.packageFqName.isRoot) return
        if (context.languageVersionSettings.supportsFeature(LanguageFeature.JsAllowInvalidCharsIdentifiersEscaping)) return

        declaration.packageDirective.source?.forEachChildOfType(setOf(KtNodeTypes.REFERENCE_EXPRESSION)) {
            val name = it.text.toString()
            if (sanitizeName(name) != name) {
                reporter.reportOn(
                    it,
                    FirErrors.INVALID_CHARACTERS,
                    "$name contains illegal characters",
                    context,
                )
            }
        }
    }
}

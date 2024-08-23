/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.resolve.calls.mpp.isIllegalRequiresOptInAnnotation

object FirRequiresOptInOnExpectChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(
        declaration: FirDeclaration,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (context.session.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects) &&
            declaration is FirRegularClass &&
            declaration.isExpect &&
            with(context.session.expectActualMatchingContextFactory.create(context.session, context.scopeSession)) {
                val expectSymbol = declaration.symbol
                isIllegalRequiresOptInAnnotation(on = expectSymbol, expectSymbol, context.languageVersionSettings)
            }
        ) {
            reporter.reportOn(declaration.source, FirErrors.EXPECT_ACTUAL_OPT_IN_ANNOTATION, context)
        }
    }
}

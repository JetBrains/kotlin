/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.areExpectActualClassesStable
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.isEnabled

object FirExpectActualClassifiersAreInBetaChecker : FirClassLikeChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClassLikeDeclaration) {
        if (!LanguageFeature.MultiPlatformProjects.isEnabled()) return
        if (context.languageVersionSettings.areExpectActualClassesStable) return

        if (declaration.isExpect || declaration.isActual) {
            reporter.reportOn(declaration.source, FirErrors.EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING)
        }
    }
}

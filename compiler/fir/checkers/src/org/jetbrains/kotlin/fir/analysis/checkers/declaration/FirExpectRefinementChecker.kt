/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isTopLevel
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.expectForActual
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility

object FirExpectRefinementChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        val hasExpectRefinementAnnotation =
            declaration.hasAnnotation(StandardClassIds.Annotations.ExpectRefinement, context.session)
        val isExpect = declaration is FirMemberDeclaration && declaration.isExpect
        if (hasExpectRefinementAnnotation && (!isExpect || !context.isTopLevel)) {
            reporter.reportOn(
                declaration.source,
                FirErrors.EXPECT_REFINEMENT_ANNOTATION_WRONG_TARGET,
                context
            )
            return
        }
        if (declaration !is FirMemberDeclaration) return
        val matchingData = declaration.symbol.expectForActual.orEmpty()
        val matchedWithAnotherExpect = matchingData.contains(ExpectActualMatchingCompatibility.MatchedSuccessfully)
        if (matchedWithAnotherExpect && declaration.isExpect && !declaration.isActual && context.isTopLevel) {
            if (!hasExpectRefinementAnnotation) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.EXPECT_REFINEMENT_ANNOTATION_MISSING,
                    context
                )
            }
            if (!context.languageVersionSettings.supportsFeature(LanguageFeature.ExpectRefinement)) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.UNSUPPORTED_FEATURE,
                    LanguageFeature.ExpectRefinement to context.languageVersionSettings,
                    context
                )
            }
        }
        if (!matchedWithAnotherExpect && hasExpectRefinementAnnotation) {
            reporter.reportOn(
                declaration.source,
                FirErrors.ACTUAL_WITHOUT_EXPECT,
                declaration.symbol,
                matchingData,
                context
            )
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.diagnostics.requireNotNull
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.isActual
import org.jetbrains.kotlin.fir.expectActualMatchingContextFactory
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualAnnotationMatchChecker

/**
 * This checker runs only in IDE mode. In CLI IR checker runs instead of it.
 */
internal object FirActualAnnotationsMatchExpectChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirDeclaration) {
        if (declaration !is FirMemberDeclaration) return
        if (!LanguageFeature.MultiPlatformProjects.isEnabled()) return
        if (!LanguageFeature.MultiplatformRestrictions.isEnabled()) return
        if (!declaration.isActual) return

        val actualSymbol = declaration.symbol
        val expectSymbol = actualSymbol.getSingleMatchedExpectForActualOrNull() ?: return

        val actualContainingClass = context.containingDeclarations.lastOrNull() as? FirRegularClassSymbol
        val expectContainingClass = actualContainingClass?.getSingleMatchedExpectForActualOrNull() as? FirRegularClassSymbol
        checkAnnotationsMatch(expectSymbol, actualSymbol, expectContainingClass)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAnnotationsMatch(
        expectSymbol: FirBasedSymbol<*>,
        actualSymbol: FirBasedSymbol<*>,
        expectContainingClass: FirRegularClassSymbol?,
    ) {
        val matchingContext = context.session.expectActualMatchingContextFactory.create(context.session, context.scopeSession)
        val incompatibility =
            AbstractExpectActualAnnotationMatchChecker.areAnnotationsCompatible(
                expectSymbol,
                actualSymbol,
                expectContainingClass,
                matchingContext
            ) ?: return
        val actualAnnotationTargetSourceElement = (incompatibility.actualAnnotationTargetElement as FirSourceElement).element

        reporter.reportOn(
            actualSymbol.source.requireNotNull(),
            FirErrors.ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT,
            incompatibility.expectSymbol as FirBasedSymbol<*>,
            incompatibility.actualSymbol as FirBasedSymbol<*>,
            actualAnnotationTargetSourceElement,
            incompatibility.type.mapAnnotationType { it.annotationSymbol as FirAnnotation })
    }
}

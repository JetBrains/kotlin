/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.fir.FirPsiSourceElement
import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator.Companion.withNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameter

object FirTypeParameterSyntaxChecker : FirDeclarationSyntaxChecker<FirTypeParameter, KtTypeParameter>() {

    override fun isApplicable(element: FirTypeParameter, source: FirSourceElement): Boolean =
        element.bounds.size >= 2

    override fun checkPsi(
        element: FirTypeParameter,
        source: FirPsiSourceElement<KtTypeParameter>,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val (constraint, params) = element.bounds.partition { it.psi?.parent is KtTypeConstraint }
        if (params.isNotEmpty() && constraint.isNotEmpty()) {
            reporter.reportOn(source, FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS, context)
        }
    }

    override fun checkLightTree(
        element: FirTypeParameter,
        source: FirSourceElement,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val (constraint, params) = element.withNavigator {
            element.bounds.partition { it.isInTypeConstraint() }
        }
        if (params.isNotEmpty() && constraint.isNotEmpty()) {
            reporter.reportOn(source, FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS, context)
        }
    }
}

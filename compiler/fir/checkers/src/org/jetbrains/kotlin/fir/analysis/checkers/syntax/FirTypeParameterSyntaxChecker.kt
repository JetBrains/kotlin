/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.SourceNavigator.Companion.withNavigator
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameter

object FirTypeParameterSyntaxChecker : FirDeclarationSyntaxChecker<FirTypeParameter, KtTypeParameter>() {

    override fun isApplicable(element: FirTypeParameter, source: KtSourceElement): Boolean =
        element.bounds.size >= 2

    override fun checkPsi(
        element: FirTypeParameter,
        source: KtPsiSourceElement,
        psi: KtTypeParameter,
        context: CheckerContext,
        reporter: DiagnosticReporter
    ) {
        val (constraint, params) = element.bounds.partition { it.psi?.parent is KtTypeConstraint }
        if (params.isNotEmpty() && constraint.isNotEmpty()) {
            reporter.reportOn(source, FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS, context)
        }
    }

    override fun checkPsiOrLightTree(
        element: FirTypeParameter,
        source: KtSourceElement,
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

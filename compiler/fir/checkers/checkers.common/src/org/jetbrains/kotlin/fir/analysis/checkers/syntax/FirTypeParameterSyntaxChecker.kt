/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.syntax

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirTypeParameterChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.utils.isDangling
import org.jetbrains.kotlin.fir.syntax.withSyntax

object FirTypeParameterSyntaxChecker : FirTypeParameterChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirTypeParameter) {
        val source = declaration.source ?: return

        // A dangling type parameter has no KtTypeParameter, and all its bounds come from constraints
        if (declaration.bounds.size < 2 || declaration.isDangling) return

        val [constraint, params] = withSyntax {
            declaration.bounds.partition { it.source?.isInTypeConstraint() == true }
        }
        if (params.isNotEmpty() && constraint.isNotEmpty()) {
            reporter.reportOn(source, FirErrors.MISPLACED_TYPE_PARAMETER_CONSTRAINTS)
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.types.Variance

object FirTypeParameterVarianceChecker : FirTypeParameterChecker() {

    override fun check(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingDeclaration = context.containingDeclarations.lastOrNull()
        if (declaration.variance != Variance.INVARIANT &&
            (containingDeclaration is FirSimpleFunction ||
                    containingDeclaration is FirTypeAlias ||
                    containingDeclaration is FirProperty)
        ) {
            reporter.reportOn(declaration.source, FirErrors.VARIANCE_ON_TYPE_PARAMETER_NOT_ALLOWED, context)
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.declarations.utils.getDanglingTypeConstraintsOrEmpty

object FirTypeConstraintsChecker : FirBasicDeclarationChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirDeclaration, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirTypeParameterRefsOwner) return

        //basically we transfer errors, which were discovered in ast parsers
        declaration.getDanglingTypeConstraintsOrEmpty().forEach { constraint ->
            reporter.reportOn(
                constraint.source,
                FirErrors.NAME_IN_CONSTRAINT_IS_NOT_A_TYPE_PARAMETER,
                constraint.name,
                declaration.symbol,
                context
            )
        }
    }

}

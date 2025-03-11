/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.name.SpecialNames

object FirUnnamedPropertyChecker : FirPropertyChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.name != SpecialNames.UNDERSCORE_FOR_UNUSED_VAR) {
            return
        }

        if (declaration.isVar) {
            reporter.reportOn(declaration.source, FirErrors.UNNAMED_VAR_PROPERTY, context)
        }

        if (declaration.delegate != null) {
            reporter.reportOn(declaration.delegate?.source, FirErrors.UNNAMED_DELEGATED_PROPERTY, context)
        }
    }
}

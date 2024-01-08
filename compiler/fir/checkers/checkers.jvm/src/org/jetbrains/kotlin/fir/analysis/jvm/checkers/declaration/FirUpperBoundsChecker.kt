/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirTypeParameterChecker
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isArrayType

object FirUpperBoundsChecker : FirTypeParameterChecker(MppCheckerKind.Common) {

    override fun check(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.symbol.resolvedBounds.any { it.coneType.isArrayType }) {
            reporter.reportOn(declaration.source, FirJvmErrors.UPPER_BOUND_CANNOT_BE_ARRAY, context)
        }
    }

}

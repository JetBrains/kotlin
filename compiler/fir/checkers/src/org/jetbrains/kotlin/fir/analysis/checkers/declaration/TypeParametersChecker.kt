/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.declaration

import org.jetbrains.kotlin.fir.analysis.checkers.canHaveSubtypes
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.declarations.FirTypeParameterRefsOwner
import org.jetbrains.kotlin.fir.types.coneType

object TypeParametersChecker {

    fun checkUpperBounds(declaration: FirTypeParameterRefsOwner, context: CheckerContext, reporter: DiagnosticReporter) {
        declaration.typeParameters.forEach { param ->
            param.symbol.fir.bounds.forEach { bound ->
                if (!bound.coneType.canHaveSubtypes(context.session)) {
                    reporter.reportOn(bound.source, FirErrors.FINAL_UPPER_BOUND, bound.coneType, context)
                }
            }
        }
    }
}

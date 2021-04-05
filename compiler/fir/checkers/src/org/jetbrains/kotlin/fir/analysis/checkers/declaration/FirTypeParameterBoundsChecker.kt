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
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isExtensionFunctionType

object FirTypeParameterBoundsChecker : FirTypeParameterChecker() {

    override fun check(declaration: FirTypeParameter, context: CheckerContext, reporter: DiagnosticReporter) {
        val containingDeclaration = context.containingDeclarations.lastOrNull()
        if (containingDeclaration is FirConstructor) return
        if (containingDeclaration is FirSimpleFunction && containingDeclaration.isOverride) return
        if (containingDeclaration is FirProperty && containingDeclaration.isOverride) return

        declaration.symbol.fir.bounds.forEach { bound ->
            if (!bound.coneType.canHaveSubtypes(context.session)) {
                reporter.reportOn(bound.source, FirErrors.FINAL_UPPER_BOUND, bound.coneType, context)
            }
            if (bound.isExtensionFunctionType(context.session)) {
                reporter.reportOn(bound.source, FirErrors.UPPER_BOUND_IS_EXTENSION_FUNCTION_TYPE, context)
            }
        }
    }
}

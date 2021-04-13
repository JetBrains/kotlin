/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeNullability
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isNullLiteral

object FirAssignmentTypeMismatchChecker : FirVariableAssignmentChecker() {
    override fun check(expression: FirVariableAssignment, context: CheckerContext, reporter: DiagnosticReporter) {
        val rValue = expression.rValue
        if (rValue.isNullLiteral) {
            val leftType = ((expression.lValue as? FirResolvedNamedReference)?.resolvedSymbol as? FirPropertySymbol)?.fir?.returnTypeRef
            if (leftType?.coneType?.nullability == ConeNullability.NOT_NULL) {
                reporter.reportOn(rValue.source, FirErrors.NULL_FOR_NONNULL_TYPE, context)
            }
        }
    }
}
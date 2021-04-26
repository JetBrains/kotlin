/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirDivisionByZeroChecker : FirFunctionCallChecker() {
    private val defaultPackageName = FqName("kotlin")
    private val defaultDivName = Name.identifier("div")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val firstValue = (expression.arguments.singleOrNull() as? FirConstExpression<*>)?.value
        if (firstValue != null && (firstValue == 0L || firstValue == 0.0f || firstValue == 0.0)) {
            val callableId =
                ((expression.calleeReference as? FirResolvedNamedReference)?.resolvedSymbol as? FirNamedFunctionSymbol)?.callableId
            if (callableId != null && callableId.packageName == defaultPackageName && callableId.callableName == defaultDivName) {
                reporter.reportOn(expression.source, FirErrors.DIVISION_BY_ZERO, context)
            }
        }
    }
}
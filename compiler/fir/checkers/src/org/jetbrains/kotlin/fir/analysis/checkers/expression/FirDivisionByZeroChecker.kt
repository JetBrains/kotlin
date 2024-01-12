/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.references.toResolvedNamedFunctionSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object FirDivisionByZeroChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    private val defaultPackageName = FqName("kotlin")
    private val defaultDivName = Name.identifier("div")

    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        val firstValue = (expression.arguments.singleOrNull() as? FirLiteralExpression<*>)?.value
        if (firstValue != null && (firstValue == 0L || firstValue == 0.0f || firstValue == 0.0)) {
            val callableId = (expression.calleeReference.toResolvedNamedFunctionSymbol())?.callableId
            if (callableId != null && callableId.packageName == defaultPackageName && callableId.callableName == defaultDivName) {
                reporter.reportOn(expression.source, FirErrors.DIVISION_BY_ZERO, context)
            }
        }
    }
}

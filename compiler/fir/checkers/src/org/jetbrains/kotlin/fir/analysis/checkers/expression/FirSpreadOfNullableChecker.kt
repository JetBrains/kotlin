/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirSpreadArgumentExpression
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.resolvedType

object FirSpreadOfNullableChecker : FirFunctionCallChecker() {
    override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
        fun checkAndReport(argument: FirExpression, source: KtSourceElement?) {
            val coneType = argument.resolvedType
            if (argument is FirSpreadArgumentExpression && coneType !is ConeFlexibleType && coneType.canBeNull) {
                reporter.reportOn(source, FirErrors.SPREAD_OF_NULLABLE, context)
            }
        }

        for (argument in expression.argumentList.arguments) {
            if (argument is FirVarargArgumentsExpression) {
                for (subArgument in argument.arguments) {
                    checkAndReport(subArgument, argument.source)
                }
            } else {
                checkAndReport(argument, argument.source)
            }
        }
    }
}

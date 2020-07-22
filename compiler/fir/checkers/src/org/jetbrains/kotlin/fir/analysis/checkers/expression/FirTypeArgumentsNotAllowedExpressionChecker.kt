/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.FirSourceElement
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.ConeClassErrorType
import org.jetbrains.kotlin.fir.types.FirTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.coneType

object FirTypeArgumentsNotAllowedExpressionChecker : FirQualifiedAccessChecker() {
    override fun check(functionCall: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // otherwise there'll be an attempt
        // to access firstChild of some null FirElement
        for (typeArgument in functionCall.typeArguments) {
            if (typeArgument is FirTypeProjectionWithVariance) {
                val typeRef = typeArgument.typeRef
                val type = typeRef.coneType
                if (type is ConeClassErrorType && type.reason == "Type arguments not allowed") {
                    reporter.report(type.source as FirSourceElement)
                }
            }
        }
    }

    private fun DiagnosticReporter.report(source: FirSourceElement?) {
        source?.let {
            report(FirErrors.TYPE_ARGUMENTS_NOT_ALLOWED.on(it))
        }
    }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeImplicitPropertyTypeOnInvokeLikeCallWithExtReceiver

object FirImplicitPropertyTypeOnInvokeLikeCallChecker : FirFunctionCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirFunctionCall) {
        expression.nonFatalDiagnostics.forEach {
            if (it is ConeImplicitPropertyTypeOnInvokeLikeCallWithExtReceiver) {
                reporter.reportOn(
                    expression.source,
                    FirErrors.IMPLICIT_PROPERTY_TYPE_ON_INVOKE_LIKE_CALL,
                    it.property,
                )
            }
        }
    }
}
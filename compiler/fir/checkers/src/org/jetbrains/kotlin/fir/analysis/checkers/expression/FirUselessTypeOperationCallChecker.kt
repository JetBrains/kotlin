/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isRefinementUseless
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.expressions.argument
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.upperBoundIfFlexible

object FirUselessTypeOperationCallChecker : FirTypeOperatorCallChecker(MppCheckerKind.Common) {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.operation !in FirOperation.TYPES) return
        val arg = expression.argument

        val lhsType = arg.resolvedType.upperBoundIfFlexible().fullyExpandedType(context.session)
        if (lhsType is ConeErrorType) return

        val targetType = expression.conversionTypeRef.coneType.fullyExpandedType(context.session)
        if (targetType is ConeErrorType) return

        if (isRefinementUseless(context, lhsType, targetType, expression, arg)) {
            when (expression.operation) {
                FirOperation.IS -> reporter.reportOn(expression.source, FirErrors.USELESS_IS_CHECK, true, context)
                FirOperation.NOT_IS -> reporter.reportOn(expression.source, FirErrors.USELESS_IS_CHECK, false, context)
                FirOperation.AS, FirOperation.SAFE_AS -> {
                    if (!expression.argFromStubType) {
                        reporter.reportOn(expression.source, FirErrors.USELESS_CAST, context)
                    }
                }
                else -> throw AssertionError("Should not be here: ${expression.operation}")
            }
        }
    }
}

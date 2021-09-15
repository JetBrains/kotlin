/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.isCastErased
import org.jetbrains.kotlin.fir.analysis.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.reportOn
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.expressions.FirTypeOperatorCall
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.coneType

object FirCannotCheckForErasedChecker : FirTypeOperatorCallChecker() {
    override fun check(expression: FirTypeOperatorCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.operation != FirOperation.IS) return

        val session = context.session
        val subjectType = expression.argumentList.arguments[0].typeRef.coneType.fullyExpandedType(session)
        val conversionTypeRef = expression.conversionTypeRef
        val targetType = conversionTypeRef.coneType.fullyExpandedType(session)

        if (isCastErased(subjectType, targetType, context)) {
            reporter.reportOn(conversionTypeRef.source, FirErrors.CANNOT_CHECK_FOR_ERASED, targetType, context)
        }
    }
}
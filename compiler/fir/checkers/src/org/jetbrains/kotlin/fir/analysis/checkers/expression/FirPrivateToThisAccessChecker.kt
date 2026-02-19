/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.getSymbolAndQualifierIfInvisibleAccess
import org.jetbrains.kotlin.fir.isEnabled

object FirPrivateToThisAccessChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val (symbol, qualifierReceiverForUnboundReference) = getSymbolAndQualifierIfInvisibleAccess(expression, context.session) ?: return

        val factory = when {
            qualifierReceiverForUnboundReference == null -> FirErrors.INVISIBLE_REFERENCE
            LanguageFeature.ForbidPrivateToThisUnboundCallableReferences.isEnabled() -> FirErrors.INVISIBLE_REFERENCE
            else -> FirErrors.INVISIBLE_REFERENCE_WARNING
        }
        reporter.reportOn(
            source = expression.source,
            factory = factory,
            a = symbol,
            b = Visibilities.PrivateToThis,
            c = symbol.callableId!!.classId
        )
    }
}

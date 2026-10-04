/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnstableSmartCastDueToOldBareInference

/**
 * Reports a migration warning for the places that type-check only thanks to a smart cast to the type
 * inferred by the deprecated (old) bare type argument inference algorithm,
 * see [org.jetbrains.kotlin.types.SmartcastStability.OLD_BARE_INFERENCE].
 */
object FirUnstableSmartCastDueToOldBareInferenceChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        expression.nonFatalDiagnostics.filterIsInstance<ConeUnstableSmartCastDueToOldBareInference>().forEach { diagnostic ->
            reporter.reportOn(
                diagnostic.argument.source ?: expression.source,
                FirErrors.UNSTABLE_SMART_CAST_DUE_TO_OLD_BARE_INFERENCE,
                diagnostic.targetType,
            )
        }
    }
}

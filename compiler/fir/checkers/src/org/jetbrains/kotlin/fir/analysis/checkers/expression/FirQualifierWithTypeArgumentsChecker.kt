/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeTypeArgumentsForOuterClass

object FirQualifierWithTypeArgumentsChecker : FirResolvedQualifierChecker(MppCheckerKind.Common) {
    override fun check(
        expression: FirResolvedQualifier,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        expression.nonFatalDiagnostics.filterIsInstance<ConeTypeArgumentsForOuterClass>().forEach { diagnostic ->
            if (expression.symbol?.isInner == true) return@forEach
            reporter.reportOn(
                expression.source,
                FirErrors.TYPE_ARGUMENTS_FOR_OUTER_CLASS_WHEN_NESTED_REFERENCED,
                context
            )
        }
    }
}

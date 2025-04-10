/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.EqualsOverrideContract
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.computeEqualsOverrideContract
import org.jetbrains.kotlin.fir.expressions.ExhaustivenessStatus
import org.jetbrains.kotlin.fir.expressions.FirWhenExpression
import org.jetbrains.kotlin.fir.hasElseBranch
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol

object FirWhenEqualsOverrideChecker : FirWhenExpressionChecker(mppKind = MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirWhenExpression) {
        if (expression.hasElseBranch()) return
        val exhaustivenessStatus = expression.exhaustivenessStatus as? ExhaustivenessStatus.ProperlyExhaustive ?: return

        for (symbolEqualsCheck in exhaustivenessStatus.symbolsNotCoveredByUnsafeEquals) {
            val symbol = symbolEqualsCheck.classId.toSymbol(context.session) as? FirClassSymbol<*> ?: continue

            val contract = computeEqualsOverrideContract(
                symbolsForType = listOf(symbol),
                session = context.session,
                scopeSession = context.scopeSession,
                visitedSymbols = mutableSetOf(),
                // During metadata compilation of common code, we receive `expect open class Any`,
                // meaning that matching anything by `equals()` becomes unsafe.
                // While this is true, we choose to trust the code until we see an explicit `equals()`
                // override - for historical reasons.
                trustExpectClasses = true,
            )

            if (contract < EqualsOverrideContract.TRUSTED_FOR_EXHAUSTIVENESS) {
                reporter.reportOn(
                    source = symbolEqualsCheck.source ?: expression.subjectVariable?.source,
                    factory = FirErrors.UNSAFE_EXHAUSTIVENESS,
                    symbolEqualsCheck.classId,
                )
            }
        }
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.analysis.checkers.checkCondition
import org.jetbrains.kotlin.fir.analysis.checkers.classKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.diagnostics.withSuppressedDiagnostics
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.expressions.impl.FirElseIfTrueCondition
import org.jetbrains.kotlin.fir.symbols.impl.FirEnumEntrySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.coneType

object FirWhenConditionChecker : FirWhenExpressionChecker() {
    override fun check(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        for (branch in expression.branches) {
            val condition = branch.condition
            if (condition is FirElseIfTrueCondition) continue
            withSuppressedDiagnostics(condition, context) {
                checkCondition(condition, it, reporter)
            }
        }
        if (expression.subject != null) {
            checkDuplicatedLabels(expression, context, reporter)
        }
    }

    private fun checkDuplicatedLabels(expression: FirWhenExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        // The second part of each pair indicates whether the `is` check is positive or negated.
        val checkedTypes = hashSetOf<Pair<ConeKotlinType, FirOperation>>()
        val checkedConstants = hashSetOf<Any?>()
        for (branch in expression.branches) {
            when (val condition = branch.condition) {
                is FirEqualityOperatorCall -> {
                    val arguments = condition.arguments
                    if (arguments.size == 2 && arguments[0] is FirWhenSubjectExpression) {
                        val value = when (val targetExpression = arguments[1]) {
                            is FirConstExpression<*> -> targetExpression.value
                            is FirQualifiedAccessExpression -> targetExpression.calleeReference.toResolvedCallableSymbol() as? FirEnumEntrySymbol
                                ?: continue
                            is FirResolvedQualifier -> {
                                val classSymbol = targetExpression.symbol ?: continue
                                if (classSymbol.classKind != ClassKind.OBJECT) continue
                                classSymbol.classId
                            }
                            else -> continue
                        }
                        if (!checkedConstants.add(value)) {
                            reporter.reportOn(condition.source, FirErrors.DUPLICATE_LABEL_IN_WHEN, context)
                        }
                    }
                }
                is FirTypeOperatorCall -> {
                    if (!checkedTypes.add(condition.conversionTypeRef.coneType to condition.operation)) {
                        reporter.reportOn(condition.conversionTypeRef.source, FirErrors.DUPLICATE_LABEL_IN_WHEN, context)
                    }
                }
            }
        }
    }
}

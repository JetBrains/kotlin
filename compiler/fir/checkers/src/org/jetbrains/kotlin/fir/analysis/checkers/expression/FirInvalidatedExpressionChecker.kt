/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.DomainStatus
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.types.hasResolvedType
import org.jetbrains.kotlin.fir.types.isPrimitiveNumberOrNullableType
import org.jetbrains.kotlin.fir.types.resolvedType

object FirInvalidatedExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (LanguageFeature.ImprovedAliasTracking.isDisabled()) return

        when (val status = expression.domainStatus) {
            null, DomainStatus.OK -> {}
            else -> reporter.reportOn(
                expression.source,
                FirErrors.INVALIDATED_REFERENCE,
                status,
                context
            )
        }

        if (!expression.hasResolvedType || !expression.resolvedType.isPrimitiveNumberOrNullableType) {
            val references = expression.domainReferences
            if (references != null && references.size > 1) {
                reporter.reportOn(
                    expression.source,
                    FirErrors.MULTIPLE_REFERENCES,
                    references.map { it.first },
                    context
                )
            }
        }
    }

}

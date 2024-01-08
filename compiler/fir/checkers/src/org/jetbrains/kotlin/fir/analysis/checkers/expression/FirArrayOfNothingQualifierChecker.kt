/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.types.*

object FirArrayOfNothingQualifierChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val resolvedType = expression.resolvedType
        checkTypeAndTypeArguments(resolvedType, expression.calleeReference.source, context, reporter)
    }

    fun ConeKotlinType.isArrayOfNothing(): Boolean {
        if (!this.isArrayTypeOrNullableArrayType) return false
        val typeParameterType = type.typeArguments.firstOrNull()?.type ?: return false
        return typeParameterType.isNothingOrNullableNothing
    }

    private fun checkTypeAndTypeArguments(
        type: ConeKotlinType,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (type.isArrayOfNothing()) {
            reporter.reportOn(source, FirErrors.UNSUPPORTED, "Array<Nothing> is illegal", context)
        } else {
            for (typeArg in type.typeArguments) {
                val typeArgType = typeArg.type ?: continue
                checkTypeAndTypeArguments(typeArgType, source, context, reporter)
            }
        }
    }
}

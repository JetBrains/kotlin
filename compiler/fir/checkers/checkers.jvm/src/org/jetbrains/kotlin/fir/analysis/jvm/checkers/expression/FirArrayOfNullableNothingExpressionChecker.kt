/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.isEnabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.isArrayTypeOrNullableArrayType
import org.jetbrains.kotlin.fir.types.isNullableNothing
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.fir.types.type

object FirArrayOfNullableNothingExpressionChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (!LanguageFeature.NullableNothingInReifiedPosition.isEnabled()) return
        val resolvedType = expression.resolvedType
        checkTypeAndTypeArguments(resolvedType, expression.calleeReference.source)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTypeAndTypeArguments(
        type: ConeKotlinType,
        source: KtSourceElement?,
    ) {
        val fullyExpandedType = type.fullyExpandedType()
        if (fullyExpandedType.isArrayOfNullableNothing()) {
            reporter.reportOn(source, FirErrors.UNSUPPORTED, "'Array<Nothing?>' is not supported on the JVM.")
        } else {
            for (typeArg in fullyExpandedType.typeArguments) {
                val typeArgType = typeArg.type ?: continue
                checkTypeAndTypeArguments(typeArgType, source)
            }
        }
    }
}

internal fun ConeKotlinType.isArrayOfNullableNothing(): Boolean {
    if (!this.isArrayTypeOrNullableArrayType) return false
    val typeParameterType = typeArguments.firstOrNull()?.type ?: return false
    return typeParameterType.isNullableNothing
}


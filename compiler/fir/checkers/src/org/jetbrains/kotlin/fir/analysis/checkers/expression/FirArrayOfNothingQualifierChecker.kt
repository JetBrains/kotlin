/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirArrayOfNothingQualifierChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override fun check(expression: FirQualifiedAccessExpression, context: CheckerContext, reporter: DiagnosticReporter) {
        val resolvedType = expression.resolvedType
        checkTypeAndTypeArguments(resolvedType, expression.calleeReference.source, context, reporter)
    }

    private fun checkTypeAndTypeArguments(
        type: ConeKotlinType,
        source: KtSourceElement?,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val fullyExpandedType = type.fullyExpandedType(context.session)
        if (fullyExpandedType.isArrayOfNothing(context.languageVersionSettings)) {
            reporter.reportOn(source, FirErrors.UNSUPPORTED, "Array<Nothing> is illegal", context)
        } else {
            for (typeArg in fullyExpandedType.typeArguments) {
                val typeArgType = typeArg.type ?: continue
                checkTypeAndTypeArguments(typeArgType, source, context, reporter)
            }
        }
    }
}

internal fun ConeKotlinType.isArrayOfNothing(languageVersionSettings: LanguageVersionSettings): Boolean {
    if (!this.isArrayTypeOrNullableArrayType) return false
    val typeParameterType = typeArguments.firstOrNull()?.type ?: return false
    return typeParameterType.isUnsupportedNothingAsReifiedOrInArray(languageVersionSettings)
}

internal fun ConeKotlinType.isUnsupportedNothingAsReifiedOrInArray(languageVersionSettings: LanguageVersionSettings): Boolean {
    return when {
        isNothing -> true
        isNullableNothing -> !languageVersionSettings.supportsFeature(LanguageFeature.NullableNothingInReifiedPosition)
        else -> false
    }
}
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
        val arrayOfNothingKind = fullyExpandedType.unsupportedArrayOfNothingKind(context.languageVersionSettings)
        if (arrayOfNothingKind != null) {
            reporter.reportOn(source, FirErrors.UNSUPPORTED, "Expression cannot have a type of '${arrayOfNothingKind.representation}'.", context)
        } else {
            for (typeArg in fullyExpandedType.typeArguments) {
                val typeArgType = typeArg.type ?: continue
                checkTypeAndTypeArguments(typeArgType, source, context, reporter)
            }
        }
    }
}

internal fun ConeKotlinType.unsupportedArrayOfNothingKind(languageVersionSettings: LanguageVersionSettings): ArrayOfNothingKind? {
    if (!this.isArrayTypeOrNullableArrayType) return null
    val typeParameterType = typeArguments.firstOrNull()?.type ?: return null
    return typeParameterType.unsupportedKindOfNothingAsReifiedOrInArray(languageVersionSettings)
}

enum class ArrayOfNothingKind(val representation: String) {
    ArrayOfNothing("Array<Nothing>"),
    ArrayOfNullableNothing("Array<Nothing?>"),
}

internal fun ConeKotlinType.unsupportedKindOfNothingAsReifiedOrInArray(languageVersionSettings: LanguageVersionSettings): ArrayOfNothingKind? {
    return when {
        isNothing -> ArrayOfNothingKind.ArrayOfNothing
        isNullableNothing && !languageVersionSettings.supportsFeature(LanguageFeature.NullableNothingInReifiedPosition) -> ArrayOfNothingKind.ArrayOfNullableNothing
        else -> null
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirArrayOfNothingClassLiteralChecker : FirGetClassCallChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirGetClassCall) {
        val resolvedType = expression.resolvedType
        val resolvedQualifier = expression.argument.unwrapSmartcastExpression() as? FirResolvedQualifier ?: return
        val deprecation = ArrayOfNothingDeprecation(
            LanguageFeature.ForbidArrayOfNothingInLhsOfClassLiteral,
            FirErrors.UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS,
        )
        checkTypeAndTypeArguments(resolvedType, resolvedQualifier.source, deprecation)
    }
}

object FirArrayOfNothingQualifiedChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        val resolvedType = expression.resolvedType
        checkTypeAndTypeArguments(resolvedType, expression.calleeReference.source)
    }
}

context(context: CheckerContext)
internal fun ConeKotlinType.unsupportedArrayOfNothingKind(languageVersionSettings: LanguageVersionSettings): ArrayOfNothingKind? {
    if (!this.isArrayTypeOrNullableArrayType) return null
    val typeParameterType = typeArguments.firstOrNull()?.type?.fullyExpandedType() ?: return null
    return typeParameterType.unsupportedKindOfNothingAsReifiedOrInArray(languageVersionSettings)
}

internal enum class ArrayOfNothingKind(val representation: String) {
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

class ArrayOfNothingDeprecation(
    val deprecatingFeature: LanguageFeature,
    val warningFactory: KtDiagnosticFactory1<String>,
)

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTypeAndTypeArguments(
    type: ConeKotlinType,
    source: KtSourceElement?,
    deprecation: ArrayOfNothingDeprecation? = null,
) {
    val fullyExpandedType = type.fullyExpandedType()
    val arrayOfNothingKind = fullyExpandedType.unsupportedArrayOfNothingKind(context.languageVersionSettings)
    if (arrayOfNothingKind != null) {
        val diagnosticFactory = deprecation?.takeIf { it.deprecatingFeature.isDisabled() }?.warningFactory ?: FirErrors.UNSUPPORTED
        reporter.reportOn(
            source,
            diagnosticFactory,
            "Expression cannot have a type of '${arrayOfNothingKind.representation}'."
        )
    } else {
        for (typeArg in fullyExpandedType.typeArguments) {
            val typeArgType = typeArg.type ?: continue
            checkTypeAndTypeArguments(typeArgType, source, deprecation)
        }
    }
}

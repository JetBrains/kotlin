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
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ArrayOfNothingDeprecation
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirGetClassCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirQualifiedAccessExpressionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.unwrapSmartcastExpression
import org.jetbrains.kotlin.fir.isDisabled
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.*

object FirArrayOfNullableNothingClassLiteralChecker : FirGetClassCallChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirGetClassCall) {
        val type = expression.resolvedType
        val qualifier = expression.argument.unwrapSmartcastExpression() as? FirResolvedQualifier ?: return
        val deprecation = ArrayOfNothingDeprecation(
            LanguageFeature.ForbidArrayOfNothingInLhsOfClassLiteral,
            FirErrors.UNSUPPORTED_ARRAY_OF_NOTHING_IN_CLASS_LITERAL_LHS,
        )
        checkTypeAndTypeArguments(type, qualifier.source, deprecation)
    }
}

object FirArrayOfNullableNothingQualifiedChecker : FirQualifiedAccessExpressionChecker(MppCheckerKind.Common) {
    override val platformSpecificCheckerEnabledInMetadataCompilation: Boolean
        get() = true

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: FirQualifiedAccessExpression) {
        if (LanguageFeature.NullableNothingInReifiedPosition.isDisabled()) return
        val resolvedType = expression.resolvedType
        checkTypeAndTypeArguments(resolvedType, expression.calleeReference.source)
    }
}

context(context: CheckerContext)
internal fun ConeKotlinType.isArrayOfNullableNothing(): Boolean {
    if (!this.isArrayTypeOrNullableArrayType) return false
    val typeParameterType = typeArguments.firstOrNull()?.type?.fullyExpandedType() ?: return false
    return typeParameterType.isNullableNothing
}

context(context: CheckerContext, reporter: DiagnosticReporter)
private fun checkTypeAndTypeArguments(
    type: ConeKotlinType,
    source: KtSourceElement?,
    deprecation: ArrayOfNothingDeprecation? = null,
) {
    val fullyExpandedType = type.fullyExpandedType()
    if (fullyExpandedType.isArrayOfNullableNothing()) {
        val factory = deprecation?.takeIf { it.deprecatingFeature.isDisabled() }?.warningFactory ?: FirErrors.UNSUPPORTED
        reporter.reportOn(source, factory, "'Array<Nothing?>' is not supported on the JVM.")
    } else {
        for (typeArg in fullyExpandedType.typeArguments) {
            val typeArgType = typeArg.type ?: continue
            checkTypeAndTypeArguments(typeArgType, source, deprecation)
        }
    }
}

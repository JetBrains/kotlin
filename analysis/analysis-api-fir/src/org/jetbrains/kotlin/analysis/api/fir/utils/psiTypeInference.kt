/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.builder.parseCharacter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.inferClassIdByPsi
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.psi.utils.parseNumericLiteral

/**
 * Optimization: try to determine the return type of the function by inspecting its expression body if it has an implicit return type,
 * so FIR doesn't have to be built and resolved.
 *
 * A block body is not supported, as it needs no inspection: such a function returns `Unit` unless it declares the return type.
 *
 * An anonymous function is not supported, as it takes its return type from the expected type, like a lambda.
 */
internal fun KtNamedFunction.inferReturnTypeByPsi(analysisSession: KaFirSession): KaType? {
    if (isAnonymous || hasDeclaredReturnType() || hasBlockBody()) return null
    return bodyExpression?.getDeclarationTypeByPsiOrNull(analysisSession)
}

/**
 * Optimization: try to determine the type of the property by inspecting its initializer or its getter body if it has an implicit type,
 * so FIR doesn't have to be built and resolved.
 *
 * The getter of such a property has the same type.
 */
internal fun KtProperty.inferReturnTypeByPsi(analysisSession: KaFirSession): KaType? {
    if (typeReference != null || getter?.typeReference != null) return null

    // FIR takes the type of the initializer first
    val expression = initializer ?: getter?.bodyExpression
    return expression?.getDeclarationTypeByPsiOrNull(analysisSession)
}

private fun KtExpression.getDeclarationTypeByPsiOrNull(analysisSession: KaFirSession): KaType? {
    // A loop is not an expression, so FIR infers an error type for a declaration with such a body
    if (unwrapParenthesesLabelsAndAnnotations() is KtLoopExpression) return null

    // The body of a declaration with an implicit type has no expected type
    return getExpressionTypeByPsiOrNull(analysisSession, noExpectedType = true)
}

/**
 * Optimization: try to determine the type of the expression by its PSI, so FIR doesn't have to be built.
 *
 * @param noExpectedType whether the expression definitely has no expected type, which defines the type of some literals.
 */
internal fun KtExpression.getExpressionTypeByPsiOrNull(analysisSession: KaFirSession, noExpectedType: Boolean): KaType? {
    val unwrappedExpression = unwrapParenthesesLabelsAndAnnotations()
    return with(analysisSession.builtinTypes) {
        when (unwrappedExpression) {
            is KtStringTemplateExpression -> string
            is KtConstructorDelegationReferenceExpression, is KtLoopExpression -> unit
            is KtBinaryExpression -> {
                when (unwrappedExpression.operationToken) {
                    // Not overloadable, always boolean
                    KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ, KtTokens.EQEQ, KtTokens.EXCLEQ,
                    KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ,
                        -> boolean
                    // Only used for boolean operations. Bitwise operations use infix functions
                    KtTokens.ANDAND, KtTokens.OROR -> boolean
                    else -> null
                }
            }
            is KtIsExpression -> boolean
            is KtContinueExpression, is KtBreakExpression, is KtThrowExpression, is KtReturnExpression, is KtLabelReferenceExpression,
                -> nothing
            is KtConstantExpression -> {
                if (unwrappedExpression.isMalformedLiteral()) return null

                val classId = unwrappedExpression.inferClassIdByPsi()
                /**
                 * Type of [int] and `UInt` literals is defined by their expected types:
                 * ```kotlin
                 * fun main() {
                 *     val number: Byte = 1 // Byte, not Int
                 *     val number2: Long = 1 // Long, not Int
                 *     val number3: ULong = 1u // ULong, not UInt
                 * }
                 * ```
                 *
                 * Calculating the expected type might require building FIR.
                 * If so, this PSI optimization doesn't make sense.
                 * But if there is no any expected type ([noExpectedType] == `true`), then
                 * it's safe to return just [int] or `UInt`
                 */
                if (!noExpectedType && (classId == StandardClassIds.Int || classId == StandardClassIds.UInt)) return null
                when (classId) {
                    StandardClassIds.Int -> int
                    StandardClassIds.Long -> long
                    StandardClassIds.Float -> float
                    StandardClassIds.Double -> double
                    StandardClassIds.Char -> char
                    StandardClassIds.Boolean -> boolean
                    StandardClassIds.UInt, StandardClassIds.ULong -> analysisSession.buildClassType(classId)
                    else -> null
                }
            }
            else -> null
        }
    }
}

/**
 * Whether the raw FIR builder converts this literal into an error expression, while [inferClassIdByPsi] still infers some type for it.
 *
 * See [org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder.generateConstantExpressionByLiteral].
 */
private fun KtConstantExpression.isMalformedLiteral(): Boolean {
    val text = text
    return when (iElementType) {
        KtNodeTypes.INTEGER_CONSTANT -> {
            val hasLeadingZeros = text.length > 1 && text[0] == '0' && (text[1].isDigit() || text[1] == '_')
            hasLeadingZeros || text.endsWith('l')
        }
        KtNodeTypes.FLOAT_CONSTANT -> parseNumericLiteral(text, isFloatingPointLiteral = true) == null
        KtNodeTypes.CHARACTER_CONSTANT -> text.parseCharacter().value == null
        else -> false
    }
}

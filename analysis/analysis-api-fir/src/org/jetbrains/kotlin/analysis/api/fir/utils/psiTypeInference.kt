/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.builder.parseCharacter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.inferClassIdByPsi
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations

/**
 * Optimization: try to determine the return type of the function or the property by inspecting its expression body, initializer or
 * getter body if it has an implicit return type, so FIR doesn't have to be built and resolved.
 */
internal fun KtDeclarationWithReturnType.inferReturnTypeByPsi(analysisSession: KaFirSession): KaType? {
    val singleExpression = when (this) {
        // A function with a block body returns `Unit` unless it declares the return type, which `createReturnType` already checks by PSI
        is KtNamedFunction -> if (hasDeclaredReturnType() || hasBlockBody()) null else bodyExpression

        // FIR takes the type of the initializer first
        is KtProperty -> if (typeReference != null || getter?.typeReference != null) null else initializer ?: getter?.bodyExpression

        else -> return null
    }

    // Depending on the loop, FIR infers an error type or Unit for a declaration with a loop body
    if (singleExpression?.unwrapParenthesesLabelsAndAnnotations() is KtLoopExpression) return null

    // The body of a declaration with an implicit type has no expected type
    return singleExpression?.getExpressionTypeByPsiOrNull(analysisSession, noExpectedType = true)
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

            // A loop statement has the Unit type, but a declaration with a loop body is left to FIR
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
                val classId = unwrappedExpression.inferClassIdByPsi()

                // The raw FIR builder converts a malformed character literal, such as `'ab'`, into an error expression
                if (classId == StandardClassIds.Char && unwrappedExpression.text.parseCharacter().value == null) return null

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
                val dependsOnExpectedType = when (classId) {
                    StandardClassIds.Int, StandardClassIds.UInt -> true
                    else -> false
                }
                if (dependsOnExpectedType && !noExpectedType) return null
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

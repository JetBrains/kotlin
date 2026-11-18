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
 * Optimization: try to determine the return type of the declaration by its PSI if the type is implicit, so FIR doesn't have to be
 * built and resolved.
 */
context(analysisSession: KaFirSession)
internal fun KtDeclaration.inferReturnTypeByPsi(): KaType? {
    val singleExpression = when (this) {
        is KtNamedFunction -> when {
            hasDeclaredReturnType() -> return null

            // A function with a block body returns `Unit` unless it declares the return type
            hasBlockBody() -> return analysisSession.builtinTypes.unit

            // An anonymous function takes its return type from the expected type, like a lambda
            isAnonymous -> return null

            else -> bodyExpression
        }

        // FIR takes the type of the initializer first
        is KtProperty -> when {
            typeReference != null || getter?.typeReference != null -> return null
            else -> initializer ?: getter?.bodyExpression
        }

        // A setter parameter without a declared type has the type of its property
        is KtParameter -> return when (val accessor = ownerFunction) {
            is KtPropertyAccessor if typeReference == null -> accessor.property.inferReturnTypeByPsi()
            else -> null
        }

        else -> return null
    }

    // Depending on the loop, FIR infers an error type or Unit for a declaration with a loop body
    if (singleExpression?.unwrapParenthesesLabelsAndAnnotations() is KtLoopExpression) return null

    // The body of a declaration with an implicit type has no expected type
    return singleExpression?.inferExpressionTypeByPsi(mayHaveExpectedType = false)
}

/**
 * Optimization: try to determine the type of the expression by its PSI, so FIR doesn't have to be built.
 *
 * @param mayHaveExpectedType whether the expression may have an expected type, which defines the type of some literals.
 */
context(analysisSession: KaFirSession)
internal fun KtExpression.inferExpressionTypeByPsi(mayHaveExpectedType: Boolean): KaType? {
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
                 * An integer literal that fits into `Int` and an unsigned literal take their type from the expected type:
                 * ```kotlin
                 * val number: Byte = 1 // Byte, not Int
                 * val number2: Long = 1 // Long, not Int
                 * val number3: ULong = 1u // ULong, not UInt
                 * ```
                 *
                 * Computing the expected type may require building FIR, which defeats this optimization, so the inference returns
                 * [int] only when [mayHaveExpectedType] is `false`. Unsigned literals are always left to FIR, which also infers an
                 * error type for them if any unsigned type is missing.
                 */
                when (classId) {
                    StandardClassIds.Int -> int.takeUnless { mayHaveExpectedType }
                    StandardClassIds.Long -> long
                    StandardClassIds.Float -> float
                    StandardClassIds.Double -> double
                    StandardClassIds.Char -> char
                    StandardClassIds.Boolean -> boolean
                    else -> null
                }
            }

            else -> null
        }
    }
}

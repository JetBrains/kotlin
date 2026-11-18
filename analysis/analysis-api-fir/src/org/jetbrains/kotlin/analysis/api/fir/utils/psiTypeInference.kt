/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.utils

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.fir.builder.appliesToPrimaryConstructorParameter
import org.jetbrains.kotlin.fir.builder.parseCharacter
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.inferClassIdByPsi
import org.jetbrains.kotlin.psi.psiUtil.isExpectDeclaration
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations

/**
 * Optimization: try to determine the return type of the declaration by its PSI if the type is implicit, so FIR doesn't have to be
 * built and resolved.
 */
context(analysisSession: KaFirSession)
internal fun KtDeclaration.inferReturnTypeByPsi(): KaType? {
    val singleExpression = when (this) {
        // A function with a block body returns `Unit` unless it declares the return type
        is KtNamedFunction if !hasDeclaredReturnType() && hasBlockBody() -> return analysisSession.builtinTypes.unit

        // A setter parameter without a declared type has the type of its property
        is KtParameter -> return when (val accessor = ownerFunction) {
            is KtPropertyAccessor if typeReference == null -> accessor.property.inferReturnTypeByPsi()
            else -> null
        }

        else -> implicitTypeSource ?: return null
    }

    // Depending on the loop, FIR infers an error type or Unit for a declaration with a loop body
    if (singleExpression.unwrapParenthesesLabelsAndAnnotations() is KtLoopExpression) return null

    // The body of a declaration with an implicit type has no expected type
    return singleExpression.inferExpressionTypeByPsi(mayHaveExpectedType = false)
}

/**
 * The initializer or the body that defines the implicit type of this declaration.
 */
internal val KtDeclaration.implicitTypeSource: KtExpression?
    get() = when (this) {
        is KtNamedFunction -> when {
            hasDeclaredReturnType() -> null
            hasBlockBody() -> null

            // An anonymous function takes its return type from the expected type, like a lambda
            isAnonymous -> null

            else -> bodyExpression
        }

        // FIR takes the type of the initializer first
        is KtProperty -> when {
            typeReference != null || getter?.typeReference != null -> null
            else -> initializer ?: getter?.bodyExpression
        }

        else -> null
    }

/**
 * Optimization: try to determine the type of the expression by its PSI, so FIR doesn't have to be built.
 *
 * @param mayHaveExpectedType whether the expression may have an expected type, which defines the type of some literals.
 */
context(analysisSession: KaFirSession)
internal fun KtExpression.inferExpressionTypeByPsi(mayHaveExpectedType: Boolean): KaType? {
    val unwrappedExpression = unwrapParenthesesLabelsAndAnnotations()
    val type = with(analysisSession.builtinTypes) {
        when (unwrappedExpression) {
            is KtStringTemplateExpression -> string

            // A loop statement has the Unit type, but a declaration with a loop body is left to FIR
            is KtLoopExpression -> unit

            is KtBinaryExpression -> {
                when (unwrappedExpression.operationToken) {
                    // Not overloadable, always boolean, although FIR gives `Any` for an equality in a call whose inference fails (KT-91816)
                    KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ, KtTokens.EQEQ, KtTokens.EXCLEQ,
                    KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ,
                        -> boolean
                    // Only used for boolean operations. Bitwise operations use infix functions
                    KtTokens.ANDAND, KtTokens.OROR -> boolean
                    else -> null
                }
            }

            is KtIsExpression -> boolean

            is KtContinueExpression, is KtBreakExpression, is KtThrowExpression, is KtReturnExpression -> nothing

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

    return type?.takeUnless { mayBeDroppedByRawFir }
}

/**
 * Whether this expression may be in code that FIR doesn't keep, such as invalid code that raw FIR drops or the effects of
 * `contract [...]`.
 *
 * The check is conservative: it is also `true` for some parts of the enclosing declarations that FIR keeps.
 */
internal val KtExpression.mayBeDroppedByRawFir: Boolean
    get() = parents.any(PsiElement::mayHaveInvalidCodeDroppedByRawFir)

private val PsiElement.mayHaveInvalidCodeDroppedByRawFir: Boolean
    get() = when (this) {
        is KtParameter -> when {
            // Raw FIR keeps only the name and the type of a function type parameter, including a context one
            isFunctionTypeParameter -> true

            // Raw FIR drops the default value of a catch or context parameter
            isCatchParameter -> true
            isContextParameter -> true

            // Raw FIR drops the annotations of the parameter of a setter without a body
            ownerFunction.let { it is KtPropertyAccessor && !it.hasBody() } -> true

            // A primary constructor parameter without `val` or `var` has no property, so raw FIR drops its annotations for one, such
            // as `@field:` ones (KT-91817)
            ownerFunction is KtPrimaryConstructor && !hasValOrVar() -> annotationEntries.any {
                val useSiteTarget = it.useSiteTarget?.getAnnotationUseSiteTarget()
                !useSiteTarget.appliesToPrimaryConstructorParameter()
            }

            else -> false
        }

        // Raw FIR drops the arguments of the entries of an `expect` enum class
        is KtEnumEntry -> isExpectDeclaration()

        // Raw FIR drops a `where` constraint without a type parameter name
        is KtTypeConstraint -> subjectTypeParameterName == null

        // FIR resolves the effects of `contract [...]` in a synthetic `contract { ... }` call that isn't part of the declaration
        is KtContractEffectList -> true

        // Raw FIR drops the annotations of a placeholder type argument, `_`
        is KtTypeReference -> isPlaceholder

        else -> false
    }

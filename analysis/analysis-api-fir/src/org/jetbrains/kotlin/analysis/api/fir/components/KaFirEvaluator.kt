/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.fir.KaConstantValueFactory
import org.jetbrains.kotlin.analysis.api.fir.KaErrorConstantValueImpl
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.KaNullConstantValueImpl
import org.jetbrains.kotlin.analysis.api.fir.KaStringConstantValueImpl
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.fir.utils.implicitTypeSource
import org.jetbrains.kotlin.analysis.api.fir.utils.mayBeDroppedByRawFir
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsEvaluator
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.escapedStringToCharacter
import org.jetbrains.kotlin.fir.builder.parseCharacter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.psi.psiUtil.inferClassIdByPsi
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.psi.utils.parseBooleanLiteral
import org.jetbrains.kotlin.psi.utils.parseNumericLiteral

internal class KaFirEvaluator(
    override val analysisSessionProvider: () -> KaFirSession
) : KaFirSessionComponent(), KaInternalsEvaluator {
    override fun evaluate(expression: KtExpression): KaConstantValue? = expression.withPsiValidityAssertion {
        return expression.evaluateByPsi() ?: expression.evaluateByFir()
    }

    /**
     * Returns the FIR-based evaluation of this expression, which [evaluate] skips when the PSI-based one succeeds.
     */
    internal fun KtExpression.evaluateByFir(): KaConstantValue? =
        evaluateFir(getOrBuildFir(resolutionFacade), this)

    override fun evaluateAsAnnotationValue(expression: KtExpression): KaAnnotationValue? = expression.withPsiValidityAssertion {
        return (expression.getOrBuildFir(resolutionFacade) as? FirExpression)?.let {
            FirAnnotationValueConverter.toConstantValue(it, analysisSession.firSymbolBuilder)
        }
    }

    private fun evaluateFir(
        fir: FirElement?,
        sourcePsi: KtExpression,
    ): KaConstantValue? {
        return when {
            fir is FirPropertyAccessExpression || fir is FirExpression || fir is FirNamedReference -> {
                try {
                    FirCompileTimeConstantEvaluator.evaluateAsKtConstantValue(fir, analysisSession.firSession)
                } catch (e: ArithmeticException) {
                    KaErrorConstantValueImpl(e.localizedMessage, sourcePsi)
                }
            }
            // For invalid code like the following,
            // ```
            // when {
            //   true, false -> {}
            // }
            // ```
            // `false` does not have a corresponding elements on the FIR side and hence the containing `FirWhenBranch` is returned. In this
            // case, we simply report null since FIR does not know about it.
            else -> null
        }
    }

}

/**
 * Optimization: try to evaluate a literal by its PSI, so FIR doesn't have to be built.
 *
 * Like FIR, the evaluation skips parentheses, labels and annotations around the literal, which becomes the source of the constant.
 */
internal fun KtExpression.evaluateByPsi(): KaConstantValue? {
    val constantValue = when (val literal = unwrapParenthesesLabelsAndAnnotations()) {
        is KtConstantExpression -> literal.evaluateByPsi()
        is KtStringTemplateExpression -> literal.evaluateByPsi()
        else -> null
    }

    return constantValue?.takeUnless { isAssignmentTarget || mayBeDroppedByRawFir }
}

private fun KtConstantExpression.evaluateByPsi(): KaConstantValue? {
    if (iElementType == KtNodeTypes.NULL) return KaNullConstantValueImpl(this)

    val value: Any = when (val classId = inferClassIdByPsi()) {
        StandardClassIds.Boolean -> parseBooleanLiteral(text)

        // The raw FIR builder converts a malformed character literal, such as `'ab'`, into an error expression
        StandardClassIds.Char -> text.parseCharacter().value

        StandardClassIds.Float, StandardClassIds.Double -> parseNumericLiteral(text, isFloatingPointLiteral = true)

        StandardClassIds.Int, StandardClassIds.Long, StandardClassIds.UInt, StandardClassIds.ULong -> {
            val number = parseNumericLiteral(text, isFloatingPointLiteral = false)?.toLong()
            when (classId) {
                // An `Int` or `UInt` literal takes another kind from an expected type, as in `val number: Byte = 1`,
                // and the source of an implicit type has no expected type
                StandardClassIds.Int if isImplicitTypeSource -> number?.toInt()
                StandardClassIds.UInt if isImplicitTypeSource -> number?.toUInt()

                StandardClassIds.Long -> number
                StandardClassIds.ULong -> number?.toULong()
                else -> null
            }
        }

        else -> null
    } ?: return null

    return KaConstantValueFactory.createConstantValue(value, this)
}

private fun KtStringTemplateExpression.evaluateByPsi(): KaConstantValue? {
    val value = StringBuilder()
    for (entry in entries) {
        when (entry) {
            is KtLiteralStringTemplateEntry -> value.append(entry.text)

            // The raw FIR builder converts a malformed escape, such as `\q`, into an error expression
            is KtEscapeStringTemplateEntry -> {
                val character = escapedStringToCharacter(entry.text).value ?: return null
                value.append(character)
            }

            // A template with an expression requires resolution
            else -> return null
        }
    }

    return KaStringConstantValueImpl(value.toString(), this)
}

/**
 * Whether this expression, possibly in parentheses, labels or annotations, is assigned to, as in `"a" = "b"`, `"a" += "b"` or `++1L`.
 * FIR doesn't evaluate such invalid code.
 */
private val KtExpression.isAssignmentTarget: Boolean
    get() {
        val outermostExpression = getOutermostParenthesizerOrThis()
        return when (val parent = outermostExpression.parent) {
            // FIR evaluates the operand of a postfix increment, as in `1L++`, but not of a prefix one
            is KtPrefixExpression -> parent.operationToken in KtTokens.INCREMENT_AND_DECREMENT
            else -> outermostExpression.getAssignmentByLHS() != null
        }
    }

/**
 * Whether this expression, possibly in parentheses, labels or annotations, is the [implicitTypeSource] of its declaration, so it has
 * no expected type.
 */
private val KtExpression.isImplicitTypeSource: Boolean
    get() {
        val outermostExpression = getOutermostParenthesizerOrThis()
        val declaration = when (val parent = outermostExpression.parent) {
            // The type of a property can come from the body of its getter
            is KtPropertyAccessor -> parent.property
            is KtDeclaration -> parent
            else -> return false
        }

        return declaration.implicitTypeSource == outermostExpression
    }

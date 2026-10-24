/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.fir.KaFirSession
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirAnnotationValueConverter
import org.jetbrains.kotlin.analysis.api.fir.evaluate.FirCompileTimeConstantEvaluator
import org.jetbrains.kotlin.analysis.api.fir.utils.isMalformedLiteral
import org.jetbrains.kotlin.analysis.api.impl.base.KaConstantValueFactory
import org.jetbrains.kotlin.analysis.api.impl.base.KaErrorConstantValueImpl
import org.jetbrains.kotlin.analysis.api.impl.base.KaNullConstantValueImpl
import org.jetbrains.kotlin.analysis.api.impl.base.KaStringConstantValueImpl
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.impl.base.components.withPsiValidityAssertion
import org.jetbrains.kotlin.analysis.api.internals.KaInternalsEvaluator
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFir
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.builder.escapedStringToCharacter
import org.jetbrains.kotlin.fir.builder.parseCharacter
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.psi.psiUtil.inferClassIdByPsi
import org.jetbrains.kotlin.psi.psiUtil.unwrapParenthesesLabelsAndAnnotations
import org.jetbrains.kotlin.psi.utils.hasLongNumericLiteralSuffix
import org.jetbrains.kotlin.psi.utils.parseBooleanLiteral
import org.jetbrains.kotlin.psi.utils.parseNumericLiteral

internal class KaFirEvaluator(
    override val analysisSessionProvider: () -> KaFirSession
) : KaBaseSessionComponent<KaFirSession>(), KaInternalsEvaluator, KaFirSessionComponent {
    override fun evaluate(expression: KtExpression): KaConstantValue? = expression.withPsiValidityAssertion {
        expression.evaluateByPsi()?.let { return it }
        return evaluateFir(expression.getOrBuildFir(resolutionFacade), expression)
    }

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
 * Optimization: evaluate a literal by its PSI, so FIR doesn't have to be built.
 *
 * Like FIR, the evaluation skips parentheses, labels and annotations around the literal,
 * and the literal becomes the source of the constant.
 */
private fun KtExpression.evaluateByPsi(): KaConstantValue? = when (val literal = unwrapParenthesesLabelsAndAnnotations()) {
    is KtConstantExpression -> literal.evaluateByPsi()
    is KtStringTemplateExpression -> literal.evaluateByPsi()
    else -> null
}

private fun KtConstantExpression.evaluateByPsi(): KaConstantValue? {
    if (iElementType == KtNodeTypes.NULL) return KaNullConstantValueImpl(this)

    val classId = inferClassIdByPsi() ?: return null
    if (isMalformedLiteral()) return null

    val text = text
    val value: Any = when (classId) {
        StandardClassIds.Boolean -> parseBooleanLiteral(text)
        StandardClassIds.Char -> text.parseCharacter().value
        StandardClassIds.Float, StandardClassIds.Double -> parseNumericLiteral(text, isFloatingPointLiteral = true)
        else -> {
            /**
             * An integer literal without the `L` suffix takes its kind from the expected type (`val number: Byte = 1`),
             * and raw FIR folds a unary minus or plus around it into the literal.
             * The body of a declaration with an implicit type is free of both.
             */
            if (!hasLongNumericLiteralSuffix(text) && !isBodyOfDeclarationWithImplicitType()) return null

            val number = parseNumericLiteral(text, isFloatingPointLiteral = false) as? Long ?: return null
            when (classId) {
                StandardClassIds.Int -> number.toInt()
                StandardClassIds.Long -> number
                StandardClassIds.UInt -> number.toUInt()
                StandardClassIds.ULong -> number.toULong()
                else -> null
            }
        }
    } ?: return null

    return KaConstantValueFactory.createConstantValue(value, this)
}

private fun KtStringTemplateExpression.evaluateByPsi(): KaConstantValue? {
    val value = buildString {
        for (entry in entries) {
            when (entry) {
                is KtLiteralStringTemplateEntry -> append(entry.text)
                is KtEscapeStringTemplateEntry -> append(escapedStringToCharacter(entry.text).value ?: return null)
                // A template with an expression requires resolution
                else -> return null
            }
        }
    }

    return KaStringConstantValueImpl(value, this)
}

/**
 * Whether the expression is the initializer or the expression body of a declaration with an implicit type, so it has no expected type.
 *
 * The getter of a property with an initializer doesn't count, as its body is expected to have the type of the initializer.
 */
private fun KtExpression.isBodyOfDeclarationWithImplicitType(): Boolean {
    val expression = getOutermostParenthesizerOrThis()
    return when (val parent = expression.parent) {
        is KtProperty -> parent.initializer == expression && parent.typeReference == null && parent.getter?.typeReference == null
        is KtNamedFunction -> parent.bodyExpression == expression && !parent.isAnonymous && parent.typeReference == null
        is KtPropertyAccessor -> {
            val property = parent.property
            parent.isGetter && parent.bodyExpression == expression && parent.typeReference == null &&
                    property.typeReference == null && property.initializer == null
        }
        else -> false
    }
}

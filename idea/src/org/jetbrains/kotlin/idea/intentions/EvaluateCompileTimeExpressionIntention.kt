/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class EvaluateCompileTimeExpressionIntention : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java,
    KotlinBundle.lazyMessage("evaluate.compile.time.expression")
) {
    companion object {
        val constantNodeTypes = listOf(KtNodeTypes.FLOAT_CONSTANT, KtNodeTypes.CHARACTER_CONSTANT, KtNodeTypes.INTEGER_CONSTANT)
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        if (element.getStrictParentOfType<KtBinaryExpression>() != null || !element.isConstantExpression()) return false
        val constantValue = element.getConstantValue() ?: return false
        setTextGetter { KotlinBundle.message("replace.with.0", constantValue) }
        return true
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val constantValue = element.getConstantValue() ?: return
        element.replace(KtPsiFactory(element).createExpression(constantValue))
    }

    private fun KtExpression?.isConstantExpression(): Boolean {
        return when (val expression = KtPsiUtil.deparenthesize(this)) {
            is KtConstantExpression -> expression.elementType in constantNodeTypes
            is KtPrefixExpression -> expression.baseExpression.isConstantExpression()
            is KtBinaryExpression -> expression.left.isConstantExpression() && expression.right.isConstantExpression()
            else -> false
        }
    }

    private fun KtBinaryExpression.getConstantValue(): String? {
        val context = analyze(BodyResolveMode.PARTIAL)
        val type = getType(context) ?: return null
        val constantValue = ConstantExpressionEvaluator.getConstant(this, context)?.toConstantValue(type) ?: return null
        return when (val value = constantValue.value) {
            is Char -> "'${StringUtil.escapeStringCharacters(value.toString())}'"
            is Long -> "${value}L"
            is Float -> when {
                value.isNaN() -> "Float.NaN"
                value.isInfinite() -> if (value > 0.0f) "Float.POSITIVE_INFINITY" else "Float.NEGATIVE_INFINITY"
                else -> "${value}f"
            }
            is Double -> when {
                value.isNaN() -> "Double.NaN"
                value.isInfinite() -> if (value > 0.0) "Double.POSITIVE_INFINITY" else "Double.NEGATIVE_INFINITY"
                else -> value.toString()
            }
            else -> value.toString()
        }
    }
}

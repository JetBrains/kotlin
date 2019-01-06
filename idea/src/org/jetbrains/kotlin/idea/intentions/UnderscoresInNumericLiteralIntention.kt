/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.elements.KtConstantExpressionElementType

class AddUnderscoresToNumericLiteralIntention : SelfTargetingIntention<KtConstantExpression>(
    KtConstantExpression::class.java, "Add underscores"
) {
    override fun isApplicableTo(element: KtConstantExpression, caretOffset: Int): Boolean {
        val text = element.text
        return element.isNumeric() && !text.hasUnderscore() && text.takeWhile { it.isDigit() }.length > 3
    }

    override fun applyTo(element: KtConstantExpression, editor: Editor?) {
        val text = element.text
        val digits = text.takeWhile { it.isDigit() }
        element.replace(
            KtPsiFactory(element).createExpression(
                digits.reversed().chunked(3).joinToString(separator = "_").reversed() + text.removePrefix(digits)
            )
        )
    }
}

class RemoveUnderscoresFromNumericLiteralIntention : SelfTargetingIntention<KtConstantExpression>(
    KtConstantExpression::class.java, "Remove underscores"
) {
    override fun isApplicableTo(element: KtConstantExpression, caretOffset: Int): Boolean {
        return element.isNumeric() && element.text.hasUnderscore()
    }

    override fun applyTo(element: KtConstantExpression, editor: Editor?) {
        element.replace(KtPsiFactory(element).createExpression(element.text.replace("_", "")))
    }
}

private fun KtConstantExpression.isNumeric(): Boolean = elementType in numericConstantKinds

private val numericConstantKinds = listOf(
    KtConstantExpressionElementType.kindToConstantElementType(ConstantValueKind.INTEGER_CONSTANT),
    KtConstantExpressionElementType.kindToConstantElementType(ConstantValueKind.FLOAT_CONSTANT)
)

private fun String.hasUnderscore(): Boolean = indexOf('_') != -1
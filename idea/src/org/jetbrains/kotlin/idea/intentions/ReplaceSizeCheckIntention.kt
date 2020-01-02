/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

abstract class ReplaceSizeCheckIntention(text: String) : SelfTargetingOffsetIndependentIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java, text
) {

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val target = getTargetExpression(element)
        if (target !is KtDotQualifiedExpression) return
        val createExpression = KtPsiFactory(element).createExpression("${target.receiverExpression.text}.${getGenerateMethodSymbol()}")
        element.replaced(createExpression)
    }

    override fun isApplicableTo(element: KtBinaryExpression): Boolean {
        val targetExpression = getTargetExpression(element) ?: return false
        return targetExpression.isSizeOrLength()
    }

    abstract fun getTargetExpression(element: KtBinaryExpression): KtExpression?

    abstract fun getGenerateMethodSymbol(): String
}
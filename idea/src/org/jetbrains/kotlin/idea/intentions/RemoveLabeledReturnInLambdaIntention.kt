/*
 * Copyright 2000-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveLabeledReturnInLambdaIntention : SelfTargetingIntention<KtReturnExpression>(
    KtReturnExpression::class.java,
    KotlinBundle.lazyMessage("remove.labeled.return.from.last.expression.in.a.lambda")
), LowPriorityAction {
    override fun isApplicableTo(element: KtReturnExpression, caretOffset: Int): Boolean {
        val labelName = element.getLabelName() ?: return false
        val block = element.getStrictParentOfType<KtBlockExpression>() ?: return false
        if (block.statements.lastOrNull() != element) return false
        val callName = block.getParentLambdaLabelName() ?: return false
        if (labelName != callName) return false
        text = KotlinBundle.message("remove.return.0", labelName)
        return true
    }

    override fun applyTo(element: KtReturnExpression, editor: Editor?) {
        val returnedExpression = element.returnedExpression
        if (returnedExpression == null) {
            element.delete()
        } else {
            element.replace(returnedExpression)
        }
    }
}
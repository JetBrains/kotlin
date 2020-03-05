/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.quickfix.RemoveReturnLabelFix
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.returnExpressionVisitor

class RedundantReturnLabelInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = returnExpressionVisitor(
        fun(returnExpression) {
            val label = returnExpression.getTargetLabel() ?: return
            val function = returnExpression.getParentOfType<KtNamedFunction>(true, KtLambdaExpression::class.java) ?: return
            if (function.name == null) return
            val labelName = label.getReferencedName()
            holder.registerProblem(
                label,
                "Redundant '@$labelName'",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                IntentionWrapper(RemoveReturnLabelFix(returnExpression, labelName), returnExpression.containingKtFile),
            )
        },
    )
}

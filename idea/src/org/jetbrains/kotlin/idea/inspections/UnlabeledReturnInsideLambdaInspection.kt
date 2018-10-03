/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.returnExpressionVisitor

class UnlabeledReturnInsideLambdaInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        returnExpressionVisitor(fun(returnExpression: KtReturnExpression) {
            if (returnExpression.labelQualifier != null) return
            if (returnExpression.getParentOfType<KtLambdaExpression>(true, KtNamedFunction::class.java) == null) return
            holder.registerProblem(
                returnExpression,
                "Unlabeled return inside lambda",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        })
}
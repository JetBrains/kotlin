/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.inspections.branchedTransformations

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.idea.intentions.SelfTargetingOffsetIndependentIntention
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression

class IfThenToSafeAccessInspection : AbstractApplicabilityBasedInspection<KtIfExpression>() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): KtVisitorVoid =
            object : KtVisitorVoid() {
                override fun visitIfExpression(expression: KtIfExpression) {
                    super.visitIfExpression(expression)
                    visitTargetElement(expression, holder, isOnTheFly)
                }
            }

    override fun isApplicable(element: KtIfExpression): Boolean {
        val ifThenToSelectData = element.buildSelectTransformationData() ?: return false
        if (!ifThenToSelectData.receiverExpression.isStable(ifThenToSelectData.context)) return false

        return ifThenToSelectData.clausesReplaceableBySafeCall()
    }

    override fun inspectionTarget(element: KtIfExpression) = element.ifKeyword

    override fun inspectionText(element: KtIfExpression) = "Foldable if-then"

    override fun inspectionHighlightType(element: KtIfExpression): ProblemHighlightType =
            if (element.shouldBeTransformed()) ProblemHighlightType.GENERIC_ERROR_OR_WARNING else ProblemHighlightType.INFORMATION

    override val defaultFixText = "Simplify foldable if-then"

    override fun fixText(element: KtIfExpression) : String {
        val ifThenToSelectData = element.buildSelectTransformationData()
        return if (ifThenToSelectData?.baseClauseEvaluatesToReceiver() == true) {
            if (ifThenToSelectData.condition is KtIsExpression) {
                "Replace 'if' expression with safe cast expression"
            }
            else {
                "Remove redundant 'if' expression"
            }
        }
        else {
            "Replace 'if' expression with safe access expression"
        }
    }

    override val startFixInWriteAction = false

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val ifExpression = element.getParentOfType<KtIfExpression>(true) ?: return
        val ifThenToSelectData = ifExpression.buildSelectTransformationData() ?: return

        val factory = KtPsiFactory(ifExpression)
        val resultExpr = runWriteAction {
            val replacedBaseClause = ifThenToSelectData.replacedBaseClause(factory)
            val newExpr = ifExpression.replaced(replacedBaseClause)
            KtPsiUtil.deparenthesize(newExpr)
        }

        if (editor != null) {
            (resultExpr as? KtSafeQualifiedExpression)?.inlineReceiverIfApplicableWithPrompt(editor)
        }
    }

    private fun IfThenToSelectData.clausesReplaceableBySafeCall(): Boolean = when {
        baseClause == null -> false
        negatedClause == null && baseClause.isUsedAsExpression(context) -> false
        negatedClause != null && !negatedClause.isNullExpression() -> false
        else -> baseClause.evaluatesTo(receiverExpression) || baseClause.hasFirstReceiverOf(receiverExpression) ||
                receiverExpression is KtThisExpression && hasImplicitReceiver()
    }
}

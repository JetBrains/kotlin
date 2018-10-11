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

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.*
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

class IfThenToSafeAccessInspection : AbstractApplicabilityBasedInspection<KtIfExpression>(KtIfExpression::class.java) {

    override fun isApplicable(element: KtIfExpression): Boolean {
        val ifThenToSelectData = element.buildSelectTransformationData() ?: return false
        if (!ifThenToSelectData.receiverExpression.isStableSimpleExpression(ifThenToSelectData.context)) return false

        return ifThenToSelectData.clausesReplaceableBySafeCall()
    }

    override fun inspectionTarget(element: KtIfExpression) = element.ifKeyword

    override fun inspectionText(element: KtIfExpression) = "Foldable if-then"

    override fun inspectionHighlightType(element: KtIfExpression): ProblemHighlightType =
        if (element.shouldBeTransformed()) ProblemHighlightType.GENERIC_ERROR_OR_WARNING else ProblemHighlightType.INFORMATION

    override val defaultFixText = "Simplify foldable if-then"

    override fun fixText(element: KtIfExpression): String {
        val ifThenToSelectData = element.buildSelectTransformationData()
        return if (ifThenToSelectData?.baseClauseEvaluatesToReceiver() == true) {
            if (ifThenToSelectData.condition is KtIsExpression) {
                "Replace 'if' expression with safe cast expression"
            } else {
                "Remove redundant 'if' expression"
            }
        } else {
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

        if (editor != null && resultExpr is KtSafeQualifiedExpression) {
            resultExpr.inlineReceiverIfApplicableWithPrompt(editor)
            resultExpr.renameLetParameter(editor)
        }
    }

    private fun IfThenToSelectData.clausesReplaceableBySafeCall(): Boolean = when {
        baseClause == null -> false
        negatedClause == null && baseClause.isUsedAsExpression(context) -> false
        negatedClause != null && !negatedClause.isNullExpression() -> false
        baseClause.evaluatesTo(receiverExpression) -> true
        baseClause.hasFirstReceiverOf(receiverExpression) -> true
        baseClause.anyArgumentEvaluatesTo(receiverExpression) -> true
        receiverExpression is KtThisExpression -> getImplicitReceiver()?.let { it.type == receiverExpression.getType(context) } == true
        else -> false
    }

    private fun KtSafeQualifiedExpression.renameLetParameter(editor: Editor) {
        val callExpression = selectorExpression as? KtCallExpression ?: return
        if (callExpression.calleeExpression?.text != "let") return
        val parameter = callExpression.lambdaArguments.singleOrNull()?.getLambdaExpression()?.valueParameters?.singleOrNull() ?: return
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
        editor.caretModel.moveToOffset(parameter.startOffset)
        KotlinVariableInplaceRenameHandler().doRename(parameter, editor, null)
    }
}

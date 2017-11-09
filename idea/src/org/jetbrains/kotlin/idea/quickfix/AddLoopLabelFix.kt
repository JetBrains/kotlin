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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents

class AddLoopLabelFix(
        loop: KtLoopExpression,
        private val jumpExpression: KtExpressionWithLabel
): KotlinQuickFixAction<KtLoopExpression>(loop) {

    private val existingLabelName = (loop.parent as? KtLabeledExpression)?.getLabelName()

    private val description =
            if (existingLabelName != null) "Add '@$existingLabelName' to ${jumpExpression.text}"
            else "Add label to loop"

    override fun getText() = description
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file)
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val labelName = existingLabelName ?: getUniqueLabelName(collectUsedLabels(element))

        val jumpWithLabel = KtPsiFactory(project).createExpression(jumpExpression.text + "@" + labelName)
        jumpExpression.replace(jumpWithLabel)

        // TODO(yole) use createExpressionByPattern() once it's available
        if (existingLabelName == null) {
            val labeledLoopExpression = KtPsiFactory(project).createLabeledExpression(labelName)
            labeledLoopExpression.baseExpression!!.replace(element)
            element.replace(labeledLoopExpression)
        }

        // TODO(yole) We should initiate in-place rename for the label here, but in-place rename for labels is not yet implemented
    }

    private fun collectUsedLabels(element: KtElement): Set<String> {
        val usedLabels = hashSetOf<String>()
        element.acceptChildren(object : KtTreeVisitorVoid() {
            override fun visitLabeledExpression(expression: KtLabeledExpression) {
                super.visitLabeledExpression(expression)
                usedLabels.add(expression.getLabelName()!!)
            }
        })
        element.parents.forEach {
            if (it is KtLabeledExpression) {
                usedLabels.add(it.getLabelName()!!)
            }
        }
        return usedLabels
    }

    private fun getUniqueLabelName(existingNames: Collection<String>): String {
        var index = 0
        var result = "loop"
        while (result in existingNames) {
            result = "loop${++index}"
        }
        return result
    }

    companion object: KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement as? KtExpressionWithLabel
            assert(element is KtBreakExpression || element is KtContinueExpression)
            assert((element as? KtLabeledExpression)?.getLabelName() == null)
            val loop = element?.getStrictParentOfType<KtLoopExpression>() ?: return null
            return AddLoopLabelFix(loop, element)
        }
    }
}
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
import java.util.*

public class AddLoopLabelFix(loop: JetLoopExpression, val jumpExpression: JetElement): JetIntentionAction<JetLoopExpression>(loop) {
    override fun getText() = "Add label to loop"
    override fun getFamilyName() = getText()

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        return super.isAvailable(project, editor, file)
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val usedLabels = collectUsedLabels(element)
        val labelName = getUniqueLabelName(usedLabels)

        val jumpWithLabel = JetPsiFactory(project).createExpression(jumpExpression.getText() + "@" + labelName)
        jumpExpression.replace(jumpWithLabel)

        // TODO(yole) use createExpressionByPattern() once it's available
        val labeledLoopExpression = JetPsiFactory(project).createLabeledExpression(labelName)
        labeledLoopExpression.getBaseExpression()!!.replace(element)
        element.replace(labeledLoopExpression)

        // TODO(yole) We should initiate in-place rename for the label here, but in-place rename for labels is not yet implemented
    }

    private fun collectUsedLabels(element: JetElement): Set<String> {
        val usedLabels = hashSetOf<String>()
        element.acceptChildren(object : JetTreeVisitorVoid() {
            override fun visitLabeledExpression(expression: JetLabeledExpression) {
                super.visitLabeledExpression(expression)
                usedLabels.add(expression.getLabelName()!!)
            }
        })
        element.parents.forEach {
            if (it is JetLabeledExpression) {
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

    companion object: JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.getPsiElement() as? JetElement
            assert(element is JetBreakExpression || element is JetContinueExpression)
            assert((element as? JetLabeledExpression)?.getLabelName() == null)
            val loop = element?.getStrictParentOfType<JetLoopExpression>() ?: return null
            return AddLoopLabelFix(loop, element!!)
        }
    }
}
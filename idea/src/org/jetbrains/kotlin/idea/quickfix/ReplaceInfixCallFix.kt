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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.psi.JetBinaryExpression
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetPsiFactory

public class ReplaceInfixCallFix(element: JetBinaryExpression) : JetIntentionAction<JetBinaryExpression>(element) {

    override fun getText() = "Replace with safe (?.) call"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val left = element.getLeft()
        val right = element.getRight()
        assert(left != null && right != null, "Preconditions checked by factory")
        val newText = left!!.getText() + "?." + element.getOperationReference().getText() + "(" + right!!.getText() + ")"
        element.replace(JetPsiFactory(file).createExpression(newText))
    }

    override fun startInWriteAction() = true

    companion object {
        public fun createFactory(): JetSingleIntentionActionFactory {
            return object : JetSingleIntentionActionFactory() {
                override fun createAction(diagnostic: Diagnostic): IntentionAction? {
                    val expression = QuickFixUtil.getParentElementOfType<JetBinaryExpression>(diagnostic, javaClass<JetBinaryExpression>()) ?: return null
                    if (expression.getLeft() == null) return null
                    if (expression.getRight() == null) return null
                    return ReplaceInfixCallFix(expression)
                }
            }
        }
    }
}

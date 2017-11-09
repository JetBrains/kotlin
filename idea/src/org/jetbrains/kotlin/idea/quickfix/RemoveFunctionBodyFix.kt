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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveFunctionBodyFix(element: KtFunction) : KotlinQuickFixAction<KtFunction>(element) {
    override fun getFamilyName() = "Remove function body"

    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        val element = element ?: return false
        return super.isAvailable(project, editor, file) && element.hasBody()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val bodyExpression = element.bodyExpression!!
        val equalsToken = element.equalsToken
        if (equalsToken != null) {
            val commentSaver = CommentSaver(PsiChildRange(equalsToken.nextSibling, bodyExpression.prevSibling), true)
            element.deleteChildRange(equalsToken, bodyExpression)
            commentSaver.restore(element)
        }
        else {
            bodyExpression.delete()
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtFunction>? {
            val function = diagnostic.psiElement.getNonStrictParentOfType<KtFunction>() ?: return null
            return RemoveFunctionBodyFix(function)
        }
    }
}

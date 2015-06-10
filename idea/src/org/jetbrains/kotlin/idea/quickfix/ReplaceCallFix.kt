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
import com.intellij.extapi.psi.ASTDelegatePsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.JetBundle
import org.jetbrains.kotlin.psi.*

public abstract class ReplaceCallFix protected constructor(val psiElement: PsiElement) : IntentionAction {

    override fun getFamilyName(): String = getText()

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (file is JetFile) {
            return getCallExpression() != null
        }
        return false
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val callExpression = getCallExpression() ?: return

        val selector = callExpression.getSelectorExpression()
        if (selector != null) {
            val newElement = JetPsiFactory(callExpression).createExpression(
                    callExpression.getReceiverExpression().getText() + operation + selector.getText()) as JetQualifiedExpression

            callExpression.replace(newElement)
        }
    }

    override fun startInWriteAction(): Boolean = true

    private fun getCallExpression(): JetQualifiedExpression? {
        return PsiTreeUtil.getParentOfType(psiElement, classToReplace)
    }

    abstract val operation: String
    abstract val classToReplace: Class<out JetQualifiedExpression>
}

public class ReplaceWithSafeCallFix(psiElement: PsiElement): ReplaceCallFix(psiElement) {
    override fun getText(): String = JetBundle.message("replace.with.safe.call")
    override val operation: String get() = "?."
    override val classToReplace: Class<out JetQualifiedExpression> get() = javaClass<JetDotQualifiedExpression>()

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction
                = ReplaceWithSafeCallFix(diagnostic.getPsiElement())
    }
}

public class ReplaceWithDotCallFix(psiElement: PsiElement): ReplaceCallFix(psiElement), CleanupFix {
    override fun getText(): String = JetBundle.message("replace.with.dot.call")
    override val operation: String get() = "."
    override val classToReplace: Class<out JetQualifiedExpression> get() = javaClass<JetSafeQualifiedExpression>()

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction
                = ReplaceWithDotCallFix(diagnostic.getPsiElement())
    }
}

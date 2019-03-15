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

import com.intellij.codeInsight.CodeInsightUtilCore
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddWhenElseBranchFix(element: KtWhenExpression) : KotlinQuickFixAction<KtWhenExpression>(element) {
    override fun getFamilyName() = "Add else branch"
    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.closeBrace != null
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val psiFactory = KtPsiFactory(file)
        val entry = psiFactory.createWhenEntry("else ->")
        val whenCloseBrace = element.closeBrace ?: error("isAvailable should check if close brace exist")
        val insertedWhenEntry =
            CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(element.addBefore(entry, whenCloseBrace)) as KtWhenEntry
        val endOffset = insertedWhenEntry.endOffset
        editor?.document?.insertString(endOffset, " ")
        editor?.caretModel?.moveToOffset(endOffset + 1)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): AddWhenElseBranchFix? {
            return diagnostic.psiElement.getNonStrictParentOfType<KtWhenExpression>()?.let(::AddWhenElseBranchFix)
        }
    }
}

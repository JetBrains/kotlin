/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.cfg.WhenChecker
import org.jetbrains.kotlin.cfg.hasUnknown
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class AddWhenRemainingBranchesFix(expression: KtWhenExpression) : KotlinQuickFixAction<KtWhenExpression>(expression) {

    override fun getFamilyName() = "Add remaining branches"

    override fun getText() = "Add remaining branches"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean =
            super.isAvailable(project, editor, file) && element.closeBrace != null &&
            with(WhenChecker.getNecessaryCases(element, element.analyze())) {
                isNotEmpty() && !hasUnknown
            }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val necessaryCases = WhenChecker.getNecessaryCases(element, element.analyze())

        val whenCloseBrace = element.closeBrace
        assert(whenCloseBrace != null) { "isAvailable should check if close brace exist" }
        val psiFactory = KtPsiFactory(file)

        for (case in necessaryCases) {
            val entry = psiFactory.createWhenEntry("${case.branchConditionText} -> TODO()")
            val insertedBranch = element.addBefore(entry, whenCloseBrace)
            element.addAfter(psiFactory.createNewLine(), insertedBranch)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<out PsiElement>? {
            val whenExpression = diagnostic.psiElement.getNonStrictParentOfType<KtWhenExpression>() ?: return null
            return AddWhenRemainingBranchesFix(whenExpression)
        }
    }
}
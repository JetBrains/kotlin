/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveUselessIsCheckFixForWhen(element: KtWhenConditionIsPattern) : KotlinQuickFixAction<KtWhenConditionIsPattern>(element) {
    override fun getFamilyName() = "Remove useless is check"

    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val condition = element ?: return
        val whenEntry = condition.parent as KtWhenEntry
        val whenExpression = whenEntry.parent as KtWhenExpression

        if (condition.isNegated) {
            condition.parent.delete()
        } else {
            whenExpression.entries.dropWhile { it != whenEntry }.forEach { it.delete() }
            val newEntry = KtPsiFactory(project).createWhenEntry("else -> ${whenEntry.expression!!.text}")
            whenExpression.addBefore(newEntry, whenExpression.closeBrace)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtWhenConditionIsPattern>? {
            val expression = diagnostic.psiElement.getNonStrictParentOfType<KtWhenConditionIsPattern>() ?: return null
            return RemoveUselessIsCheckFixForWhen(expression)
        }
    }
}

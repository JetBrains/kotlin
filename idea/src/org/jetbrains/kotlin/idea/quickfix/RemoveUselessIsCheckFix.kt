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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveUselessIsCheckFix(element: KtIsExpression) : KotlinQuickFixAction<KtIsExpression>(element) {
    override fun getFamilyName() = "Remove useless is check"

    override fun getText(): String = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.run {
            val expressionsText = this.isNegated.not().toString()
            val newExpression = KtPsiFactory(project).createExpression(expressionsText)
            replace(newExpression)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtIsExpression>? {
            val expression = diagnostic.psiElement.getNonStrictParentOfType<KtIsExpression>() ?: return null
            return RemoveUselessIsCheckFix(expression)
        }
    }
}

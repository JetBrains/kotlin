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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class WrongLongSuffixFix(element: KtConstantExpression) : KotlinQuickFixAction<KtConstantExpression>(element) {
    private val corrected = element.text.trimEnd('l') + 'L'

    override fun getText() = "Change to '$corrected'"
    override fun getFamilyName() = "Change to correct long suffix 'L'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.replace(KtPsiFactory(project).createExpression(corrected))
    }

    companion object Factory: KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction {
            val casted = Errors.WRONG_LONG_SUFFIX.cast(diagnostic)
            return WrongLongSuffixFix(casted.psiElement)
        }
    }
}
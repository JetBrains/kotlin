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
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

public class ReplaceInfixCallFix(element: KtBinaryExpression) : KotlinQuickFixAction<KtBinaryExpression>(element) {

    override fun getText() = "Replace with safe (?.) call"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val newExpression = KtPsiFactory(file).createExpressionByPattern(
                "$0?.$1($2)", element.left!!, element.operationReference, element.right!!)
        element.replace(newExpression)
    }

    override fun startInWriteAction() = true

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement.getNonStrictParentOfType<KtBinaryExpression>()!!
            if (expression.left == null || expression.right == null) return null
            return ReplaceInfixCallFix(expression)
        }
    }
}

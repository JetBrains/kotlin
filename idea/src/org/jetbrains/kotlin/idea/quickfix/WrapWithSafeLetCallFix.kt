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
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getLastParentOfTypeInRow
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.types.typeUtil.isNullabilityMismatch

class WrapWithSafeLetCallFix(
        expression: KtExpression,
        nullableExpression: KtExpression
) : KotlinQuickFixAction<KtExpression>(expression) {
    private val nullableExpressionPointer = nullableExpression.createSmartPointer()

    override fun getFamilyName() = text

    override fun getText() = "Wrap with '?.let { ... }' call"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val nullableExpression = nullableExpressionPointer.element ?: return
        val factory = KtPsiFactory(element)
        val nullableText = nullableExpression.text
        val validator = NewDeclarationNameValidator(element, nullableExpression, NewDeclarationNameValidator.Target.VARIABLES)
        val name = KotlinNameSuggester.suggestNameByName("it", validator)
        nullableExpression.replace(factory.createExpression(name))
        val wrapped = when (name) {
            "it" -> factory.createExpressionByPattern("$0?.let { $1 }", nullableText, element)
            else -> factory.createExpressionByPattern("$0?.let { $1 -> $2 }", nullableText, name, element)
        }
        element.replace(wrapped)
    }

    object UnsafeFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = diagnostic.psiElement
            val expression = element.getParentOfType<KtExpression>(true) ?: return null

            val parent = element.parent
            val nullableExpression = (parent as? KtCallExpression)?.calleeExpression ?: return null

            return WrapWithSafeLetCallFix(expression, nullableExpression)
        }
    }

    object TypeMismatchFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val typeMismatch = Errors.TYPE_MISMATCH.cast(diagnostic)
            val argument = typeMismatch.psiElement.parent as? KtValueArgument ?: return null
            val call = argument.getParentOfType<KtCallExpression>(true) ?: return null

            if (!isNullabilityMismatch(expected = typeMismatch.a, actual = typeMismatch.b)) return null

            return WrapWithSafeLetCallFix(call.getLastParentOfTypeInRow<KtQualifiedExpression>() ?: call, typeMismatch.psiElement)
        }
    }
}
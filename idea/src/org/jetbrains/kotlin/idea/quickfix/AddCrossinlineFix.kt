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
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class AddInlineModifierFix(
        element: KtNameReferenceExpression,
        private val modifier: KtModifierKeywordToken
) : KotlinQuickFixAction<KtNameReferenceExpression>(element) {
    override fun getText() = element?.let { "Add '${modifier.value}' to parameter '${it.getReferencedName()}'" } ?: ""
    override fun getFamilyName() = "Add '${modifier.value}' to parameter"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val parameter = element.findParameterWithName(element.getReferencedName()) ?: return
        if (!parameter.hasModifier(modifier)) {
            parameter.addModifier(modifier)
        }
    }

    private fun KtElement.findParameterWithName(name: String): KtParameter? {
        val function = getStrictParentOfType<KtFunction>() ?: return null
        return function.valueParameters.firstOrNull { it.name == name } ?: function.findParameterWithName(name)
    }

    object CrossInlineFactory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val casted = Errors.NON_LOCAL_RETURN_NOT_ALLOWED.cast(diagnostic)
            val reference = casted.a as? KtNameReferenceExpression ?: return emptyList()
            return listOf(AddInlineModifierFix(reference, KtTokens.CROSSINLINE_KEYWORD))
        }
    }

    object NoInlineFactory : KotlinIntentionActionsFactory() {
        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val casted = Errors.USAGE_IS_NOT_INLINABLE.cast(diagnostic)
            val reference = casted.a as? KtNameReferenceExpression ?: return emptyList()
            return listOf(AddInlineModifierFix(reference, KtTokens.NOINLINE_KEYWORD))
        }
    }
}
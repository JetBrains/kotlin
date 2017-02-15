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
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class AddInlineModifierFix(
        parameter: KtParameter,
        modifier: KtModifierKeywordToken
) : AddModifierFix(parameter, modifier) {

    override fun getText() = element?.let { "Add '${modifier.value}' to parameter '${it.name}'" } ?: ""
    override fun getFamilyName() = "Add '${modifier.value}' to parameter"

    companion object {
        private fun KtElement.findParameterWithName(name: String): KtParameter? {
            val function = getStrictParentOfType<KtFunction>() ?: return null
            return function.valueParameters.firstOrNull { it.name == name } ?: function.findParameterWithName(name)
        }
    }

    object CrossInlineFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val casted = Errors.NON_LOCAL_RETURN_NOT_ALLOWED.cast(diagnostic)
            val reference = casted.a as? KtNameReferenceExpression ?: return null
            val parameter = reference.findParameterWithName(reference.getReferencedName()) ?: return null
            return AddInlineModifierFix(parameter, KtTokens.CROSSINLINE_KEYWORD)
        }
    }

    object NoInlineFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val casted = Errors.USAGE_IS_NOT_INLINABLE.cast(diagnostic)
            val reference = casted.a as? KtNameReferenceExpression ?: return null
            val parameter = reference.findParameterWithName(reference.getReferencedName()) ?: return null
            return AddInlineModifierFix(parameter, KtTokens.NOINLINE_KEYWORD)
        }
    }

    object SuspendFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val parameter = diagnostic.psiElement as? KtParameter ?: return null
            return AddInlineModifierFix(parameter, KtTokens.NOINLINE_KEYWORD)
        }
    }

}
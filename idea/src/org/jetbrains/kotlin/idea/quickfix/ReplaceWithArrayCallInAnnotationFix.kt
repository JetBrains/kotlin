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
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class ReplaceWithArrayCallInAnnotationFix(argument: KtExpression) : KotlinQuickFixAction<KtExpression>(argument) {
    override fun getText() = KotlinBundle.message("replace.with.array.call")

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val argument = element?.getParentOfType<KtValueArgument>(false) ?: return
        val spreadElement = argument.getSpreadElement()
        if (spreadElement != null)
            spreadElement.delete()
        else
            surroundWithArrayLiteral(argument)
    }

    private fun surroundWithArrayLiteral(argument: KtValueArgument) {
        val argumentExpression = argument.getArgumentExpression() ?: return
        val factory = KtPsiFactory(argumentExpression)
        val surrounded = factory.createExpressionByPattern("[$0]", argumentExpression)

        argumentExpression.replace(surrounded)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val element = diagnostic.psiElement.safeAs<KtExpression>() ?: return null
            return ReplaceWithArrayCallInAnnotationFix(element)
        }
    }
}

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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class RemoveSingleLambdaParameterFix(element: KtParameter) : KotlinQuickFixAction<KtParameter>(element) {
    override fun getFamilyName() = "Remove single lambda parameter declaration"
    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val parameterList = element.parent as? KtParameterList ?: return
        val ownerFunction = parameterList.ownerFunction ?: return
        val arrow = ownerFunction.node.findChildByType(KtTokens.ARROW) ?: return

        parameterList.delete()
        ownerFunction.node.removeChild(arrow)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val parameter = diagnostic.psiElement as? KtParameter ?: return null
            val parameterList = parameter.parent as? KtParameterList ?: return null

            if (parameterList.parameters.size != 1) return null

            val lambda = parameterList.parent.parent as? KtLambdaExpression ?: return null

            val property = lambda.getStrictParentOfType<KtProperty>()
            if (property != null && property.typeReference == null) return null

            return RemoveSingleLambdaParameterFix(parameter)
        }
    }
}
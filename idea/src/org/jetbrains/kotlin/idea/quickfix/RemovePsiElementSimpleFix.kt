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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

open class RemovePsiElementSimpleFix(element: PsiElement, private val text: String) : KotlinQuickFixAction<PsiElement>(element) {
    override fun getFamilyName() = "Remove element"

    override fun getText() = text

    public override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.delete()
    }

    object RemoveImportFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<PsiElement>? {
            val directive = diagnostic.psiElement.getNonStrictParentOfType<KtImportDirective>() ?: return null
            val refText = directive.importedReference?.let { "for '${it.text}'" } ?: ""
            return RemovePsiElementSimpleFix(directive, "Remove conflicting import $refText")
        }
    }

    object RemoveSpreadFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<PsiElement>? {
            val element = diagnostic.psiElement
            if (element.node.elementType != KtTokens.MUL) return null
            return RemovePsiElementSimpleFix(element, "Remove '*'")
        }
    }

    object RemoveTypeArgumentsFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<PsiElement>? {
            val element = diagnostic.psiElement.getNonStrictParentOfType<KtTypeArgumentList>() ?: return null
            return RemovePsiElementSimpleFix(element, "Remove type arguments")
        }
    }

    object RemoveVariableFactory : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<PsiElement>? {
            val expression = diagnostic.psiElement.getNonStrictParentOfType<KtProperty>() ?: return null
            return object : RemovePsiElementSimpleFix(expression, "Remove variable '${expression.name}'") {
                override fun invoke(project: Project, editor: Editor?, file: KtFile) {
                    val initializer = expression.initializer
                    if (initializer != null && initializer !is KtConstantExpression) {
                        expression.replace(initializer)
                    }
                    else {
                        expression.delete()
                    }
                }
            }
        }
    }
}

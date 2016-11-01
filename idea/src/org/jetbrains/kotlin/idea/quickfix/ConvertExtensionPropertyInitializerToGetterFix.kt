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
import org.jetbrains.kotlin.idea.intentions.ConvertPropertyInitializerToGetterIntention
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class ConvertExtensionPropertyInitializerToGetterFix(element: KtExpression) : KotlinQuickFixAction<KtExpression>(element) {
    override fun getText(): String = "Convert extension property initializer to getter"

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val property = element.getParentOfType<KtProperty>(true) ?: return
        ConvertPropertyInitializerToGetterIntention.convertPropertyInitializerToGetter(property, editor)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val expression = diagnostic.psiElement as? KtExpression ?: return null
            val property = expression.getParentOfType<KtProperty>(true)!!

            if (property.getter != null) {
                return null
            }

            return ConvertExtensionPropertyInitializerToGetterFix(expression)
        }
    }
}
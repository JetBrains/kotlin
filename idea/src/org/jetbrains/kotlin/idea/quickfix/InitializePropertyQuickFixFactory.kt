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
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

object InitializePropertyQuickFixFactory : KotlinIntentionActionsFactory() {
    class AddInitializerFix(property: KtProperty) : KotlinQuickFixAction<KtProperty>(property) {
        override fun getText() = "Add initializer"
        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val descriptor = element.resolveToDescriptorIfAny() as? PropertyDescriptor ?: return
            val initializerText = CodeInsightUtils.defaultInitializer(descriptor.type) ?: "null"
            val initializer = element.setInitializer(KtPsiFactory(project).createExpression(initializerText))!!
            if (editor != null) {
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                editor.selectionModel.setSelection(initializer.startOffset, initializer.endOffset)
            }
        }
    }

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val property = diagnostic.psiElement as? KtProperty ?: return emptyList()

        if (property.receiverTypeReference == null) {
            return listOf(AddInitializerFix(property))
        }

        return emptyList()
    }
}

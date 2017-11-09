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

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class MissingConstructorBracketsFix(element: KtPrimaryConstructor) : KotlinQuickFixAction<KtPrimaryConstructor>(element), CleanupFix {
    override fun getFamilyName(): String = text
    override fun getText(): String = "Add empty brackets after primary constructor"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val constructor = element ?: return
        val constructorKeyword = constructor.getConstructorKeyword() ?: return
        if (constructor.valueParameterList != null) return

        editor?.run {
            val endOffset = constructorKeyword.endOffset
            document.insertString(endOffset, "()")
            caretModel.moveToOffset(endOffset + 1)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? =
                diagnostic.createIntentionForFirstParentOfType(::MissingConstructorBracketsFix)
    }
}

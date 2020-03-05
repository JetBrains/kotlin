/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
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

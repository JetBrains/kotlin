/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.createIntentionForFirstParentOfType
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory

class AddNewLineAfterAnnotationsFix(element: KtAnnotatedExpression) : KotlinQuickFixAction<KtAnnotatedExpression>(element) {
    override fun getText() = "Add new line after annotations"
    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val baseExpression = element.baseExpression ?: return
        val annotationsText = element.text.substring(0, baseExpression.startOffsetInParent)
        val newExpression = KtPsiFactory(project).createBlock(annotationsText + "\n" + baseExpression.text).statements[0]
        element.replace(newExpression)
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = diagnostic.createIntentionForFirstParentOfType(::AddNewLineAfterAnnotationsFix)
    }
}

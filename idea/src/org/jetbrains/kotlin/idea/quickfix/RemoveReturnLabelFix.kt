/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtReturnExpression

class RemoveReturnLabelFix(element: KtReturnExpression, private val labelName: String) : KotlinQuickFixAction<KtReturnExpression>(element) {
    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.labeledExpression?.delete()
    }

    override fun getFamilyName(): String = "Remove redundant label"

    override fun getText(): String = "Remove redundant '@$labelName'"

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtReturnExpression>? {
            val returnExpression = diagnostic.psiElement as? KtReturnExpression ?: return null
            val labelName = returnExpression.getLabelName() ?: return null
            return RemoveReturnLabelFix(returnExpression, labelName)
        }
    }
}
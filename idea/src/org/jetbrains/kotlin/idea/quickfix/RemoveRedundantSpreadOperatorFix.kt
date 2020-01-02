/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class RemoveRedundantSpreadOperatorFix(argument: KtExpression) : KotlinQuickFixAction<KtExpression>(argument) {
    override fun getText(): String = "Remove redundant *"

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val argument = element?.getParentOfType<KtValueArgument>(false) ?: return
        val spreadElement = argument.getSpreadElement() ?: return
        spreadElement.delete()
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtExpression>? {
            val element = diagnostic.psiElement.safeAs<KtExpression>() ?: return null
            return RemoveRedundantSpreadOperatorFix(element)
        }
    }
}
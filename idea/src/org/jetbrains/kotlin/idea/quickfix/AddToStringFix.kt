/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class AddToStringFix(element: KtExpression, private val nullable: Boolean) : KotlinQuickFixAction<KtExpression>(element),
    LowPriorityAction {
    override fun getFamilyName() = "Add 'toString()' call"
    override fun getText() = if (nullable) "Add safe '?.toString()' call" else "Add 'toString()' call"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val pattern = if (nullable) "$0?.toString()" else "$0.toString()"
        val expressionToInsert = KtPsiFactory(file).createExpressionByPattern(pattern, element)
        val newExpression = element.replaced(expressionToInsert)
        editor?.caretModel?.moveToOffset(newExpression.endOffset)
    }
}
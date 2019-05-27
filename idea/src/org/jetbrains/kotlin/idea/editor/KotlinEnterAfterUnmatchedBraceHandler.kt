/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.editorActions.enter.EnterAfterUnmatchedBraceHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.inspections.collections.isCalling
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinEnterAfterUnmatchedBraceHandler : EnterAfterUnmatchedBraceHandler() {
    override fun getRBraceOffset(file: PsiFile, editor: Editor, caretOffset: Int): Int {
        val element = file.findElementAt(caretOffset - 1)
        val endOffset = when (val parent = element?.parent) {
            is KtFunctionLiteral -> {
                val call = parent.getStrictParentOfType<KtCallExpression>()
                if (call?.isPropertyInitializer() == true && call.isCalling(FqName("kotlin.run"))) {
                    (parent.parent as? KtLambdaExpression)?.bodyExpression?.statements?.firstOrNull()?.endOffset
                } else {
                    null
                }
            }
            is KtWhenExpression -> {
                if (parent.isPropertyInitializer()) {
                    (parent.entries.firstOrNull()?.conditions?.firstOrNull() as? KtWhenConditionWithExpression)?.endOffset
                } else {
                    null
                }
            }
            else -> null
        }
        return endOffset ?: super.getRBraceOffset(file, editor, caretOffset)
    }

    private fun KtExpression.isPropertyInitializer(): Boolean {
        val property = this.parent as? KtProperty ?: return false
        return property.initializer == this && !property.isLocal 
    }
}
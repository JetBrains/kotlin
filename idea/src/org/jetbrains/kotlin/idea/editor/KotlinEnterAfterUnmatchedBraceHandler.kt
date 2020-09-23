/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor

import com.intellij.codeInsight.editorActions.enter.EnterAfterUnmatchedBraceHandler
import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.EditorActionHandler
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.core.util.CodeInsightUtils
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinEnterAfterUnmatchedBraceHandler : EnterAfterUnmatchedBraceHandler() {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffsetRef: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        val caretOffset = caretOffsetRef.get() - 1
        val element = file.findElementAt(caretOffset)
        if (element?.node?.elementType == KtTokens.LBRACE) {
            return super.preprocessEnter(file, editor, caretOffsetRef, caretAdvance, dataContext, originalHandler)
        }
        if (element !is PsiWhiteSpace) {
            return EnterHandlerDelegate.Result.Continue
        }
        val prevElement = CodeInsightUtils.getElementAtOffsetIgnoreWhitespaceAfter(file, caretOffset)
        if (prevElement != null && prevElement.node.elementType == KtTokens.LBRACE) {
            return super.preprocessEnter(file, editor, Ref(prevElement.startOffset + 1), caretAdvance, dataContext, originalHandler)
        }
        return EnterHandlerDelegate.Result.Continue
    }

    override fun getRBraceOffset(file: PsiFile, editor: Editor, caretOffset: Int): Int {
        val element = file.findElementAt(caretOffset - 1)
        val endOffset = when (val parent = element?.parent) {
            is KtFunctionLiteral -> {
                val call = parent.getStrictParentOfType<KtCallExpression>()
                if (call?.isDeclarationInitializer() == true) {
                    (parent.parent as? KtLambdaExpression)?.bodyExpression?.statements?.firstOrNull()?.endOffset
                } else {
                    null
                }
            }
            is KtWhenExpression -> {
                if (parent.isDeclarationInitializer()) {
                    (parent.entries.firstOrNull()?.conditions?.firstOrNull() as? KtWhenConditionWithExpression)?.endOffset
                } else {
                    null
                }
            }
            else -> null
        }
        return endOffset ?: super.getRBraceOffset(file, editor, caretOffset)
    }

    private fun KtExpression.isDeclarationInitializer(): Boolean {
        return (parent as? KtDeclarationWithInitializer)?.initializer == this
    }
}
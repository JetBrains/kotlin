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

package org.jetbrains.kotlin.idea.quickfix.createFromUsage.callableBuilder

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ScrollType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

public fun setupEditorSelection(editor: Editor, declaration: JetNamedDeclaration) {
    val caretModel = editor.caretModel
    val selectionModel = editor.selectionModel

    if (declaration is JetSecondaryConstructor) {
        caretModel.moveToOffset(declaration.getConstructorKeyword().endOffset)
    }
    else {
        caretModel.moveToOffset(declaration.nameIdentifier!!.endOffset)
    }

    fun positionBetween(left: PsiElement, right: PsiElement) {
        val from = left.siblings(withItself = false, forward = true).firstOrNull { it !is PsiWhiteSpace } ?: return
        val to = right.siblings(withItself = false, forward = false).firstOrNull { it !is PsiWhiteSpace } ?: return
        val startOffset = from.startOffset
        val endOffset = to.endOffset
        caretModel.moveToOffset(endOffset)
        selectionModel.setSelection(startOffset, endOffset)
    }

    when (declaration) {
        is JetNamedFunction, is JetSecondaryConstructor -> {
            ((declaration as JetFunction).bodyExpression as? JetBlockExpression)?.let {
                positionBetween(it.lBrace!!, it.rBrace!!)
            }
        }
        is JetClassOrObject -> {
            caretModel.moveToOffset(declaration.startOffset)
        }
        is JetProperty -> {
            caretModel.moveToOffset(declaration.endOffset)
        }
    }
    editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
}

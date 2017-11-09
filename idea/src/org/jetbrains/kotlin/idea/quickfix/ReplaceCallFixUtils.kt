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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

fun elvisOrEmpty(notNullNeeded: Boolean): String = if (notNullNeeded) "?:" else ""

fun KtExpression.shouldHaveNotNullType(): Boolean {
    val parent = parent
    val type = when (parent) {
                   is KtBinaryExpression -> parent.left?.let { it.getType(it.analyze()) }
                   is KtProperty -> parent.typeReference?.let { it.analyze()[BindingContext.TYPE, it] }
                   else -> null
               } ?: return false
    return !type.isMarkedNullable
}

fun PsiElement.moveCaretToEnd(editor: Editor?, project: Project) {
    editor?.run {
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
        val endOffset = if (text.endsWith(")")) endOffset - 1 else endOffset
        document.insertString(endOffset, " ")
        caretModel.moveToOffset(endOffset + 1)
    }
}

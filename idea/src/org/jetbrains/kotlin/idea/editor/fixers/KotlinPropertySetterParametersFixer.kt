/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.editor.fixers

import com.intellij.lang.ASTNode
import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.psiUtil.endOffset


class KotlinPropertySetterParametersFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is KtPropertyAccessor) return

        if (!psiElement.isSetter) return

        val parameter = psiElement.parameter

        if (!parameter?.text.isNullOrBlank() && psiElement.rightParenthesis != null) return

        //setter without parameter and body is valid
        if (psiElement.namePlaceholder.endOffset == psiElement.endOffset) return

        val doc = editor.document

        val parameterOffset = (psiElement.leftParenthesis?.startOffset ?: return) + 1

        if (parameter?.text.isNullOrBlank()) {
            if (psiElement.rightParenthesis == null) {
                doc.insertString(parameterOffset, "value)")
            } else {
                doc.insertString(parameterOffset, "value")
            }
        } else if (psiElement.rightParenthesis == null) {
            doc.insertString(parameterOffset + parameter!!.text.length, ")")
        }
    }

    private val KtPropertyAccessor.leftParenthesis: ASTNode?
        get() = node.findChildByType(KtTokens.LPAR)

}


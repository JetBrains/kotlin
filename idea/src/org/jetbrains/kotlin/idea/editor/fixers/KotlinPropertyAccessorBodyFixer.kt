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

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class KotlinPropertyAccessorBodyFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is KtPropertyAccessor) return

        if (psiElement.bodyExpression != null || psiElement.equalsToken != null) return

        val parentDeclaration = psiElement.getStrictParentOfType<KtDeclaration>()
        if (parentDeclaration is KtClassOrObject) {
            if (KtPsiUtil.isTrait(parentDeclaration) || psiElement.hasModifier(KtTokens.ABSTRACT_KEYWORD)) {
                return
            }
        }

        // accessor without parameter and body is valid
        if (psiElement.namePlaceholder.endOffset == psiElement.endOffset) return

        val doc = editor.document
        var endOffset = psiElement.range.end

        if (psiElement.isGetter && psiElement.rightParenthesis == null) {
            doc.insertString(endOffset, ")")
            endOffset++
        }

        if (psiElement.text?.last() == ';') {
            doc.deleteString(endOffset - 1, endOffset)
            endOffset--
        }

        doc.insertString(endOffset, "{\n}")
    }
}
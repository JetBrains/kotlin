/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.editor.fixers

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import org.jetbrains.jet.plugin.editor.KotlinSmartEnterHandler
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetFunction
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.JetPsiUtil
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetNamedFunction


public class KotlinFunctionDeclarationBodyFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is JetNamedFunction) return
        if (psiElement.getBodyExpression() != null|| psiElement.getEqualsToken() != null) return

        val parentDeclaration = PsiTreeUtil.getParentOfType(psiElement, javaClass<JetDeclaration>())
        if (parentDeclaration is JetClassOrObject) {
            if (JetPsiUtil.isTrait(parentDeclaration) || psiElement.hasModifier(JetTokens.ABSTRACT_KEYWORD)) {
                return
            }
        }

        val doc = editor.getDocument()
        var endOffset = psiElement.range.end

        if (psiElement.getText()?.last() == ';') {
            doc.deleteString(endOffset - 1, endOffset)
            endOffset--
        }

        doc.insertString(endOffset, "{}")
    }
}
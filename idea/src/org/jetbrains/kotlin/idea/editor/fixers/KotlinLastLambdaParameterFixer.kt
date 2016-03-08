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
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinLastLambdaParameterFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, element: PsiElement) {
        if (element is KtCallExpression) {
            addBracesIfNecessary(editor, element, processor)
        }
        else if (element is LeafPsiElement) {
            registerErrorIfNecessary(element, processor)
        }
    }

    private fun addBracesIfNecessary(editor: Editor, element: KtCallExpression, processor: KotlinSmartEnterHandler) {
        val bindingContext = element.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = element.getResolvedCall(bindingContext) ?: return

        val valueParameters = resolvedCall.candidateDescriptor.valueParameters

        if (resolvedCall.valueArguments.size == valueParameters.size - 1) {
            val type = valueParameters.last().type
            if (KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type)) {
                val doc = editor.document

                var offset = element.endOffset
                if (element.valueArgumentList?.rightParenthesis == null) {
                    doc.insertString(offset, ")")
                    offset++
                }

                doc.insertString(offset, "{  }")
                processor.registerUnresolvedError(offset + 2)
            }
        }
    }

    private fun registerErrorIfNecessary(element: LeafPsiElement, processor: KotlinSmartEnterHandler) {
        if(element.elementType != KtTokens.LBRACE) return
        if(element.parent?.parent?.parent?.parent !is KtCallExpression) return

        processor.registerUnresolvedError(element.endOffset + 1)
    }
}
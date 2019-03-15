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

package org.jetbrains.kotlin.idea.editor.fixers

import com.intellij.lang.SmartEnterProcessorWithFixers
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.editor.KotlinSmartEnterHandler
import org.jetbrains.kotlin.psi.KtNamedFunction


class KotlinFunctionParametersFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is KtNamedFunction) return

        val parameterList = psiElement.valueParameterList
        if (parameterList == null) {
            val identifier = psiElement.nameIdentifier ?: return

            // Insert () after name or after type parameters list when it placed after name
            val offset = Math.max(identifier.range.end, psiElement.typeParameterList?.range?.end ?: psiElement.range.start)
            editor.document.insertString(offset, "()")
            processor.registerUnresolvedError(offset + 1)
        } else {
            val rParen = parameterList.lastChild ?: return

            if (")" != rParen.text) {
                val params = parameterList.parameters
                val offset = if (params.isEmpty()) parameterList.range.start + 1 else params.last().range.end
                editor.document.insertString(offset, ")")
            }
        }
    }
}

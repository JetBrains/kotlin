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
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KotlinCatchParameterFixer : SmartEnterProcessorWithFixers.Fixer<KotlinSmartEnterHandler>() {
    override fun apply(editor: Editor, processor: KotlinSmartEnterHandler, psiElement: PsiElement) {
        if (psiElement !is KtCatchClause) return

        val catchEnd = psiElement.node.findChildByType(KtTokens.CATCH_KEYWORD)!!.textRange!!.endOffset

        val parameterList = psiElement.parameterList
        if (parameterList == null || parameterList.node.findChildByType(KtTokens.RPAR) == null) {
            val endOffset = Math.min(psiElement.endOffset, psiElement.catchBody?.startOffset ?: Int.MAX_VALUE)
            val parameter = parameterList?.parameters?.firstOrNull()?.text ?: ""
            editor.document.replaceString(catchEnd, endOffset, "($parameter)")
            processor.registerUnresolvedError(endOffset - 1)
        } else if (parameterList.parameters.firstOrNull()?.text.isNullOrBlank()) {
            processor.registerUnresolvedError(parameterList.startOffset + 1)
        }
    }
}
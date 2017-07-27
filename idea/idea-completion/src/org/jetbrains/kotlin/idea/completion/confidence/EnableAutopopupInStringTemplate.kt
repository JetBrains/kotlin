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

package org.jetbrains.kotlin.idea.completion.confidence

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf

class EnableAutopopupInStringTemplate : CompletionConfidence() {
    override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
        val stringTemplate = contextElement.prevLeaf()?.getParentOfType<KtSimpleNameStringTemplateEntry>(strict = false) ?: return ThreeState.UNSURE
        val textRange = TextRange.create(stringTemplate.endOffset, offset)
        val containsWhitespaces = textRange.substring(psiFile.text).any { it.isWhitespace() }
        return if (containsWhitespaces) ThreeState.UNSURE else ThreeState.NO
    }
}
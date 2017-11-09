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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ToFromOriginalFileMapper private constructor(
        val originalFile: KtFile,
        val syntheticFile: KtFile,
        val completionOffset: Int
) {
    companion object {
        fun create(parameters: CompletionParameters): ToFromOriginalFileMapper {
            val originalFile = parameters.originalFile as KtFile
            val syntheticFile = parameters.position.containingFile as KtFile
            return ToFromOriginalFileMapper(originalFile, syntheticFile, parameters.offset)
        }
    }

    private val syntheticLength: Int
    private val originalLength: Int
    private val tailLength: Int
    private val shift: Int

    //TODO: lazy initialization?

    init {
        val originalText = originalFile.text
        val syntheticText = syntheticFile.text
        assert(originalText.subSequence(0, completionOffset) == syntheticText.subSequence(0, completionOffset))

        syntheticLength = syntheticText.length
        originalLength = originalText.length
        val minLength = Math.min(originalLength, syntheticLength)
        tailLength = (0..minLength-1).firstOrNull {
            syntheticText[syntheticLength - it - 1] != originalText[originalLength - it - 1]
        } ?: minLength
        shift = syntheticLength - originalLength
    }

    private fun toOriginalFile(offset: Int): Int? = when {
        offset <= completionOffset -> offset
        offset >= syntheticLength - tailLength -> offset - shift
        else -> null
    }

    private fun toSyntheticFile(offset: Int): Int? = when {
        offset <= completionOffset -> offset
        offset >= originalLength - tailLength -> offset + shift
        else -> null
    }

    fun <TElement : PsiElement> toOriginalFile(element: TElement): TElement? {
        if (element.containingFile != syntheticFile) return element
        val offset = toOriginalFile(element.startOffset) ?: return null
        return PsiTreeUtil.findElementOfClassAtOffset(originalFile, offset, element::class.java, true)
    }

    fun <TElement : PsiElement> toSyntheticFile(element: TElement): TElement? {
        if (element.containingFile != originalFile) return element
        val offset = toSyntheticFile(element.startOffset) ?: return null
        return PsiTreeUtil.findElementOfClassAtOffset(syntheticFile, offset, element::class.java, true)
    }
}

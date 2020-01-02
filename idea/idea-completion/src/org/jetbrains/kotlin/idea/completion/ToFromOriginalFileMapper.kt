/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.utils.checkWithAttachment
import kotlin.math.min

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
        val originalSubSequence = originalText.take(completionOffset)
        val syntheticSubSequence = syntheticText.take(completionOffset)
        checkWithAttachment(originalSubSequence == syntheticSubSequence, {
            "original subText [len: ${originalSubSequence.length}]" +
                    " does not match synthetic subText [len: ${syntheticSubSequence.length}]"
        }) {
            it.withAttachment("original subText", originalSubSequence.toString())
                .withAttachment("synthetic subText", syntheticSubSequence.toString())
        }

        syntheticLength = syntheticText.length
        originalLength = originalText.length
        val minLength = min(originalLength, syntheticLength)
        tailLength = (0 until minLength).firstOrNull {
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

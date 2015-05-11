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

import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.psi.JetDeclaration
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class ToFromOriginalFileMapper(
        val originalFile: JetFile,
        val syntheticFile: JetFile,
        val completionOffset: Int
) {
    private val syntheticLength: Int
    private val originalLength: Int
    private val tailLength: Int
    private val shift: Int

    //TODO: lazy initialization?

    init {
        val originalText = originalFile.getText()
        val syntheticText = syntheticFile.getText()
        assert(originalText.subSequence(0, completionOffset) == syntheticText.subSequence(0, completionOffset)) //TODO: drop it

        syntheticLength = syntheticText.length
        originalLength = originalText.length
        val minLength = Math.min(originalLength, syntheticLength)
        tailLength = (0..minLength-1).firstOrNull {
            syntheticText[syntheticLength - it - 1] != originalText[originalLength - it - 1]
        } ?: minLength
        shift = syntheticLength - originalLength
    }

    public fun toOriginalFile(offset: Int): Int? {
        return when {
            offset <= completionOffset -> offset
            offset >= syntheticLength - tailLength -> offset - shift
            else -> null
        }
    }

    public fun toSyntheticFile(offset: Int): Int? {
        return when {
            offset <= completionOffset -> offset
            offset >= originalLength - tailLength -> offset + shift
            else -> null
        }
    }

    public fun toOriginalFile(declaration: JetDeclaration): JetDeclaration? {
        if (declaration.getContainingFile() != syntheticFile) return declaration
        val offset = toOriginalFile(declaration.startOffset) ?: return null
        return PsiTreeUtil.findElementOfClassAtOffset(originalFile, offset, javaClass<JetDeclaration>(), true)
    }

    public fun toSyntheticFile(declaration: JetDeclaration): JetDeclaration? {
        if (declaration.getContainingFile() != originalFile) return declaration
        val offset = toSyntheticFile(declaration.startOffset) ?: return null
        return PsiTreeUtil.findElementOfClassAtOffset(syntheticFile, offset, javaClass<JetDeclaration>(), true)
    }
}

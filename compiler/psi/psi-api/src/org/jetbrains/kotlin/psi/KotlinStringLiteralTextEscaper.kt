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

package org.jetbrains.kotlin.psi

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import it.unimi.dsi.fastutil.ints.IntArrayList
import org.jetbrains.kotlin.psi.psiUtil.getContentRange
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import kotlin.math.min

class KotlinStringLiteralTextEscaper(host: KtStringTemplateExpression) : LiteralTextEscaper<KtStringTemplateExpression>(host) {
    private var sourceOffsets: IntArray? = null

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val sourceOffsetsList = IntArrayList()
        var sourceOffset = 0

        for (child in myHost.entries) {
            val childRange = TextRange.from(child.startOffsetInParent, child.textLength)
            if (rangeInsideHost.endOffset <= childRange.startOffset) {
                break
            }
            if (childRange.endOffset <= rangeInsideHost.startOffset) {
                continue
            }
            when (child) {
                is KtEscapeStringTemplateEntry -> {
                    if (!rangeInsideHost.contains(childRange)) {
                        //don't allow injection if its range starts or ends inside escaped sequence
                        //but still process offsets for the already decoded part
                        sourceOffsetsList.add(sourceOffset)
                        sourceOffsets = sourceOffsetsList.toIntArray()
                        return false
                    }
                    val unescaped = child.unescapedValue
                    outChars.append(unescaped)
                    repeat(unescaped.length) {
                        sourceOffsetsList.add(sourceOffset)
                    }
                    sourceOffset += child.getTextLength()
                }
                else -> {
                    val textRange = rangeInsideHost.intersection(childRange)!!.shiftRight(-childRange.startOffset)
                    outChars.append(child.text, textRange.startOffset, textRange.endOffset)
                    repeat(textRange.length) {
                        sourceOffsetsList.add(sourceOffset++)
                    }
                }
            }
        }
        sourceOffsetsList.add(sourceOffset)
        sourceOffsets = sourceOffsetsList.toIntArray()
        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val offsets = sourceOffsets
        if (offsets == null || offsetInDecoded >= offsets.size) return -1
        return min(offsets[offsetInDecoded], rangeInsideHost.length) + rangeInsideHost.startOffset
    }

    override fun getRelevantTextRange(): TextRange {
        return myHost.getContentRange()
    }

    override fun isOneLine(): Boolean {
        return myHost.isSingleQuoted()
    }
}

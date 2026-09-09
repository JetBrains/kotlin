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
import org.jetbrains.kotlin.psi.psiUtil.getContentRange
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted
import kotlin.math.min

/**
 * A [LiteralTextEscaper] for Kotlin string literals, enabling language injection into them.
 *
 * It decodes the raw literal text of a [KtStringTemplateExpression] into the string value it denotes (resolving escape sequences) and
 * maintains a mapping between offsets in the decoded value and offsets in the host PSI, so that an injected language can be edited and its
 * ranges translated back to the original source.
 */
class KotlinStringLiteralTextEscaper(host: KtStringTemplateExpression) : LiteralTextEscaper<KtStringTemplateExpression>(host) {
    private var sourceOffsets: IntArray? = null

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        // Each decoded character contributes exactly one offset, and decoding never lengthens the text,
        // so `rangeInsideHost.length + 1` (the extra slot is for the final offset) is an upper bound.
        val offsets = IntArray(rangeInsideHost.length + 1)
        var decodedOffset = 0

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
                        offsets[decodedOffset++] = sourceOffset
                        sourceOffsets = offsets.copyOf(decodedOffset)
                        return false
                    }
                    val unescaped = child.unescapedValue
                    outChars.append(unescaped)
                    repeat(unescaped.length) {
                        offsets[decodedOffset++] = sourceOffset
                    }
                    sourceOffset += child.getTextLength()
                }
                else -> {
                    val textRange = rangeInsideHost.intersection(childRange)!!.shiftRight(-childRange.startOffset)
                    outChars.append(child.text, textRange.startOffset, textRange.endOffset)
                    repeat(textRange.length) {
                        offsets[decodedOffset++] = sourceOffset++
                    }
                }
            }
        }
        offsets[decodedOffset++] = sourceOffset
        sourceOffsets = offsets.copyOf(decodedOffset)
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

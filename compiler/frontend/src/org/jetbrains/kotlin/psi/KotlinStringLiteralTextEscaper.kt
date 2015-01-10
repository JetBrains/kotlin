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

import com.intellij.psi.LiteralTextEscaper
import com.intellij.openapi.util.TextRange
import gnu.trove.TIntArrayList
import org.jetbrains.kotlin.psi.psiUtil.getContentRange
import org.jetbrains.kotlin.psi.psiUtil.isSingleQuoted

public class KotlinStringLiteralTextEscaper(host: JetStringTemplateExpression): LiteralTextEscaper<JetStringTemplateExpression>(host) {
    private var sourceOffsets: IntArray? = null

    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val sourceOffsetsList = TIntArrayList()
        var sourceOffset = 0

        for (child in myHost.getEntries()) {
            val childRange = TextRange.from(child.getStartOffsetInParent(), child.getTextLength())
            if (rangeInsideHost.getEndOffset() <= childRange.getStartOffset()) {
                break
            }
            if (childRange.getEndOffset() <= rangeInsideHost.getStartOffset()) {
                continue
            }
            when (child) {
                is JetLiteralStringTemplateEntry -> {
                    val textRange = rangeInsideHost.intersection(childRange).shiftRight(-childRange.getStartOffset())
                    outChars.append(child.getText(), textRange.getStartOffset(), textRange.getEndOffset())
                    textRange.getLength().times {
                        sourceOffsetsList.add(sourceOffset++)
                    }
                }
                is JetEscapeStringTemplateEntry -> {
                    if (!rangeInsideHost.contains(childRange)) {
                        //don't allow injection if its range starts or ends inside escaped sequence
                        return false
                    }
                    val unescaped = child.getUnescapedValue()
                    outChars.append(unescaped)
                    unescaped.length().times {
                        sourceOffsetsList.add(sourceOffset)
                    }
                    sourceOffset += child.getTextLength()
                }
                else -> return false
            }
        }
        sourceOffsetsList.add(sourceOffset)
        sourceOffsets = sourceOffsetsList.toNativeArray()
        return true
    }

    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val offsets = sourceOffsets
        if (offsets == null || offsetInDecoded >= offsets.size()) return -1
        return Math.min(offsets[offsetInDecoded], rangeInsideHost.getLength()) + rangeInsideHost.getStartOffset()
    }

    override fun getRelevantTextRange(): TextRange {
        return myHost.getContentRange()
    }

    override fun isOneLine(): Boolean {
        return myHost.isSingleQuoted()
    }
}

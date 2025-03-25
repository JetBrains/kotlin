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
package org.jetbrains.kotlin.parsing

import com.intellij.psi.tree.IElementType

class TruncatedSemanticWhitespaceAwarePsiBuilder(builder: SemanticWhitespaceAwarePsiBuilder, private val myEOFPosition: Int) :
    SemanticWhitespaceAwarePsiBuilderAdapter(builder) {
    override fun eof(): Boolean {
        return super.eof() || isOffsetBeyondEof(getCurrentOffset())
    }

    override fun getTokenText(): String? {
        if (eof()) return null
        return super.getTokenText()
    }

    override fun getTokenType(): IElementType? {
        if (eof()) return null
        return super.getTokenType()
    }

    override fun lookAhead(steps: Int): IElementType? {
        if (eof()) return null

        val rawLookAheadSteps = rawLookAhead(steps)
        if (isOffsetBeyondEof(rawTokenTypeStart(rawLookAheadSteps))) return null

        return super.rawLookup(rawLookAheadSteps)
    }

    private fun rawLookAhead(steps: Int): Int {
        // This code reproduces the behavior of PsiBuilderImpl.lookAhead(), but returns a number of raw steps instead of a token type
        // This is required for implementing truncated builder behavior
        var steps = steps
        var cur = 0
        while (steps > 0) {
            cur++

            var rawTokenType = rawLookup(cur)
            while (rawTokenType != null && isWhitespaceOrComment(rawTokenType)) {
                cur++
                rawTokenType = rawLookup(cur)
            }

            steps--
        }
        return cur
    }

    private fun isOffsetBeyondEof(offsetFromCurrent: Int): Boolean {
        return myEOFPosition >= 0 && offsetFromCurrent >= myEOFPosition
    }
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser

import fleet.com.intellij.platform.syntax.SyntaxElementType

class TruncatedSemanticWhitespaceAwareSyntaxBuilder(builder: SemanticWhitespaceAwareSyntaxBuilder, private val myEOFPosition: Int) :
    SemanticWhitespaceAwareSyntaxBuilderAdapter(builder) {
    override fun eof(): Boolean {
        return super.eof() || isOffsetBeyondEof(currentOffset)
    }

    override val tokenText: String?
        get() {
            if (eof()) return null
            return super.tokenText
        }

    override val tokenType: SyntaxElementType?
        get() {
            if (eof()) return null
            return super.tokenType
        }

    override fun lookAhead(steps: Int): SyntaxElementType? {
        if (eof()) return null

        val rawLookAheadSteps = rawLookAhead(steps)
        if (isOffsetBeyondEof(rawTokenTypeStart(rawLookAheadSteps))) return null

        return super.rawLookup(rawLookAheadSteps)
    }

    private fun rawLookAhead(steps: Int): Int {
        // This code reproduces the behavior of SyntaxTreeBuilder.lookAhead(), but returns a number of raw steps instead of a token type
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
        return myEOFPosition in 0..offsetFromCurrent
    }
}

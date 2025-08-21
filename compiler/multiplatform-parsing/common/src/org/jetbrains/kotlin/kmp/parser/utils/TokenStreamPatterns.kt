/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.parser.utils

import com.intellij.platform.syntax.SyntaxElementType

internal abstract class AbstractTokenStreamPredicate {
    abstract fun matching(topLevel: Boolean): Boolean
}

internal abstract class TokenStreamPattern {
    protected var lastOccurrence: Int = -1

    /**
     * Called on each token
     *
     * @param offset
     * @param topLevel see [.isTopLevel]
     * @return `true` to stop
     */
    abstract fun processToken(offset: Int, topLevel: Boolean): Boolean

    protected fun fail() {
        lastOccurrence = -1
    }

    /**
     * @return the position where the predicate has matched, -1 if no match was found
     */
    fun result(): Int {
        return lastOccurrence
    }

    /**
     * Decides if the combination of open bracket counts makes a "top level position"
     * Straightforward meaning would be: if all counts are zero, then it's a top level
     */
    fun isTopLevel(openAngleBrackets: Int, openBrackets: Int, openBraces: Int, openParentheses: Int): Boolean {
        return openBraces == 0 && openBrackets == 0 && openParentheses == 0 && openAngleBrackets == 0
    }

    /**
     * Called on right parentheses, brackets, braces and angles (>)
     * @param token the closing bracket
     * @return true to stop matching, false to proceed
     */
    fun handleUnmatchedClosing(token: SyntaxElementType?): Boolean {
        return false
    }

    open fun reset() {
        lastOccurrence = -1
    }
}

internal class FirstBefore(private val lookFor: AbstractTokenStreamPredicate, private val stopAt: AbstractTokenStreamPredicate) : TokenStreamPattern() {
    override fun processToken(offset: Int, topLevel: Boolean): Boolean {
        if (lookFor.matching(topLevel)) {
            lastOccurrence = offset
            return true
        }
        if (stopAt.matching(topLevel)) {
            return true
        }
        return false
    }
}

internal class LastBefore private constructor(
    private val lookFor: AbstractTokenStreamPredicate,
    private val stopAt: AbstractTokenStreamPredicate,
    private val dontStopRightAfterOccurrence: Boolean
) : TokenStreamPattern() {
    private var previousLookForResult = false

    constructor(lookFor: AbstractTokenStreamPredicate, stopAt: AbstractTokenStreamPredicate) : this(lookFor, stopAt, false)

    override fun processToken(offset: Int, topLevel: Boolean): Boolean {
        val lookForResult = lookFor.matching(topLevel)
        if (lookForResult) {
            lastOccurrence = offset
        }
        if (stopAt.matching(topLevel)) {
            if (topLevel && (!dontStopRightAfterOccurrence || !previousLookForResult)) return true
        }
        previousLookForResult = lookForResult
        return false
    }

    override fun reset() {
        super.reset()
        previousLookForResult = false
    }
}




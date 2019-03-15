/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.psi

import org.jetbrains.kotlin.idea.debugger.sequence.psi.sequence.SequenceCallCheckerWithNameHeuristics
import org.jetbrains.kotlin.psi.KtCallExpression

abstract class CallCheckerWithNameHeuristics(private val nestedChecker: StreamCallChecker) : StreamCallChecker {
    override fun isIntermediateCall(expression: KtCallExpression): Boolean = nestedChecker.isIntermediateCall(expression)

    override fun isTerminationCall(expression: KtCallExpression): Boolean {
        val name = expression.calleeExpression?.text
        if (name != null) {
            return isTerminalCallName(name) && nestedChecker.isTerminationCall(expression)
        }

        return nestedChecker.isTerminationCall(expression)
    }

    abstract fun isTerminalCallName(callName: String): Boolean
}
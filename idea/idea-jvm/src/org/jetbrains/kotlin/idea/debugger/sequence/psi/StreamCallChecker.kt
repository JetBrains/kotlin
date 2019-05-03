/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.sequence.psi

import org.jetbrains.kotlin.psi.KtCallExpression

interface StreamCallChecker {
    fun isIntermediateCall(expression: KtCallExpression): Boolean
    fun isTerminationCall(expression: KtCallExpression): Boolean

    fun isStreamCall(expression: KtCallExpression): Boolean = isIntermediateCall(expression) || isTerminationCall(expression)
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.utils.addToStdlib.popLast

data class StackVisitorData<T>(
    val treeToStack: MutableList<Pair<T?, String?>> = mutableListOf(),
    val evaluationStack: MutableList<FirStatement> = mutableListOf(),
    var stringLiteralConcatenationAnalysis: Boolean = false,
) {
    fun validateStackResult(): FirStatement {
        assert(treeToStack.isEmpty())
        assert(evaluationStack.count() == 1)
        stringLiteralConcatenationAnalysis = false
        return evaluationStack.popLast()
    }
}

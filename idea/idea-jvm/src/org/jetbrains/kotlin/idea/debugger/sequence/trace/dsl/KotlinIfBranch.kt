// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.StatementFactory
import com.intellij.debugger.streams.trace.dsl.impl.common.IfBranchBase

class KotlinIfBranch(condition: Expression, thenBlock: CodeBlock, statementFactory: StatementFactory) :
    IfBranchBase(condition, thenBlock, statementFactory) {
    override fun toCode(indent: Int): String {
        val elseBlockVar = elseBlock
        val ifThen = "if (${condition.toCode(0)}) {\n".withIndent(indent) +
                thenBlock.toCode(indent + 1) +
                "}".withIndent(indent)
        if (elseBlockVar != null) {
            return ifThen + " else { \n" +
                    elseBlockVar.toCode(indent + 1) +
                    "}".withIndent(indent)
        }

        return ifThen
    }
}
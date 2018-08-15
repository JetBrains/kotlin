// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Lambda
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression

class KotlinLambda(override val variableName: String, override val body: CodeBlock) : Lambda {
    override fun call(callName: String, vararg args: Expression): Expression = TextExpression("(${toCode()})").call(callName, *args)

    override fun toCode(indent: Int): String =
        "{ $variableName ->\n".withIndent(indent) +
                body.toCode(indent + 1) +
                "}"
}
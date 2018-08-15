// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Convertable
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.ForLoopBody
import com.intellij.debugger.streams.trace.dsl.Variable

class KotlinForEachLoop(
    private val iterateVariable: Variable,
    private val collection: Expression,
    private val loopBody: ForLoopBody
) : Convertable {
    override fun toCode(indent: Int): String =
        "for (${iterateVariable.name} in ${collection.toCode()}) {\n".withIndent(indent) +
                loopBody.toCode(indent + 1) +
                "}".withIndent(indent)
}
// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Variable
import com.intellij.debugger.streams.trace.dsl.impl.AssignmentStatement

class KotlinAssignmentStatement(override val variable: Variable, override val expression: Expression) : AssignmentStatement {
    override fun toCode(indent: Int): String = "${variable.toCode()} = ${expression.toCode()}".withIndent(indent)
}
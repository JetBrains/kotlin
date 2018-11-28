// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.ArrayVariable
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.dsl.impl.VariableImpl
import com.intellij.debugger.streams.trace.impl.handler.type.ArrayType

class KotlinArrayVariable(override val type: ArrayType, override val name: String) : VariableImpl(type, name), ArrayVariable {
    override fun get(index: Expression): Expression = TextExpression("$name[${index.toCode()}]!!")

    override fun set(index: Expression, value: Expression): Expression = TextExpression("$name[${index.toCode()}] = ${value.toCode()}")

    override fun defaultDeclaration(size: Expression): VariableDeclaration =
        KotlinVariableDeclaration(this, false, type.sizedDeclaration(size.toCode()))
}
// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Lambda
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.dsl.impl.common.MapVariableBase
import com.intellij.debugger.streams.trace.impl.handler.type.MapType

class KotlinMapVariable(type: MapType, name: String) : MapVariableBase(type, name) {
    override fun get(key: Expression): Expression = this.call("getValue", key)

    override fun set(key: Expression, newValue: Expression): Expression =
        TextExpression("${toCode()}[${key.toCode()}] = ${newValue.toCode()}")

    override fun contains(key: Expression): Expression = TextExpression("(${key.toCode()} in ${toCode()})")

    override fun size(): Expression = TextExpression("${toCode()}.size")

    override fun keys(): Expression = TextExpression("${toCode()}.keys")

    override fun computeIfAbsent(key: Expression, supplier: Lambda): Expression =
        TextExpression("${toCode()}.computeIfAbsent(${key.toCode()}, ${supplier.toCode()})")

    override fun defaultDeclaration(isMutable: Boolean): VariableDeclaration =
        KotlinVariableDeclaration(this, false, type.defaultValue)
}
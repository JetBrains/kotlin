/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Lambda
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression
import com.intellij.debugger.streams.trace.dsl.impl.common.MapVariableBase
import com.intellij.debugger.streams.trace.impl.handler.type.MapType

/**
 * @author Vitaliy.Bibaev
 */
class KotlinMapVariable(type: MapType, name: String) : MapVariableBase(type, name) {
  override fun get(key: Expression): Expression = TextExpression("${toCode()}[${key.toCode()}]")

  override fun set(key: Expression, newValue: Expression): Expression =
    TextExpression("${toCode()}[${key.toCode()}] = ${newValue.toCode()}")

  override fun contains(key: Expression): Expression = TextExpression("${key.toCode()} in ${toCode()}")

  override fun size(): Expression = TextExpression("${toCode()}.size")

  override fun keys(): Expression = TextExpression("${toCode()}.keys")

  override fun computeIfAbsent(key: Expression, supplier: Lambda): Expression =
    TextExpression("${toCode()}.getOrPut(${key.toCode()}, ${supplier.toCode()}")

  override fun defaultDeclaration(isMutable: Boolean): VariableDeclaration =
    KotlinVariableDeclaration(this, false, type.defaultValue)
}
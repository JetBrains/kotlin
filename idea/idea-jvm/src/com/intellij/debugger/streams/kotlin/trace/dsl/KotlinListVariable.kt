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
import com.intellij.debugger.streams.trace.dsl.ListVariable
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration
import com.intellij.debugger.streams.trace.dsl.impl.VariableImpl
import com.intellij.debugger.streams.trace.impl.handler.type.ListType

/**
 * @author Vitaliy.Bibaev
 */
class KotlinListVariable(override val type: ListType, name: String)
  : VariableImpl(type, name), ListVariable {
  override fun get(index: Expression): Expression = call("get", index)
  override fun set(index: Expression, newValue: Expression): Expression = call("set", index, newValue)
  override fun add(element: Expression): Expression = call("add", element)

  override fun contains(element: Expression): Expression = call("contains", element)

  override fun size(): Expression = property("size")

  override fun defaultDeclaration(): VariableDeclaration =
    KotlinVariableDeclaration(this, false, type.defaultValue)
}
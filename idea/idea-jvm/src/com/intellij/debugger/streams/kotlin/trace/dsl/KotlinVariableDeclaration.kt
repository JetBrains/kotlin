// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Variable
import com.intellij.debugger.streams.trace.dsl.VariableDeclaration

/**
 * @author Vitaliy.Bibaev
 */
class KotlinVariableDeclaration(override val variable: Variable,
                                override val isMutable: Boolean,
                                private val init: String = "") : VariableDeclaration {
  override fun toCode(indent: Int): String {
    val prefix = if (isMutable) "var" else "val"
    val suffix = if (init.trim().isEmpty()) "" else " = $init"
    return "$prefix ${variable.name}: ${variable.type.variableTypeName}$suffix".withIndent(indent)
  }
}
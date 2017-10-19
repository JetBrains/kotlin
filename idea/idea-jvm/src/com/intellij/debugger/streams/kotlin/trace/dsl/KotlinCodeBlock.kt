// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.StatementFactory
import com.intellij.debugger.streams.trace.dsl.impl.LineSeparatedCodeBlock

/**
 * @author Vitaliy.Bibaev
 */
open class KotlinCodeBlock(statementFactory: StatementFactory) : LineSeparatedCodeBlock(statementFactory) {
  override fun doReturn(expression: Expression) = addStatement(expression)
}
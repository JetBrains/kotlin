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

import com.intellij.debugger.streams.trace.dsl.CodeBlock
import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.Lambda
import com.intellij.debugger.streams.trace.dsl.impl.TextExpression

/**
 * @author Vitaliy.Bibaev
 */
class KotlinLambda(override val variableName: String, override val body: CodeBlock) : Lambda {
  override fun call(callName: String, vararg args: Expression): Expression = TextExpression("(${toCode()})").call(callName, *args)

  override fun toCode(indent: Int): String =
    "{ $variableName -> \n".withIndent(indent) +
    body.toCode(indent + 1) +
    "}"
}
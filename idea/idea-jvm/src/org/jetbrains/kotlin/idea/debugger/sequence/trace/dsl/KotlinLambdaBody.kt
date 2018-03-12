// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.trace.dsl

import com.intellij.debugger.streams.trace.dsl.Expression
import com.intellij.debugger.streams.trace.dsl.LambdaBody
import com.intellij.debugger.streams.trace.dsl.StatementFactory

/**
 * @author Vitaliy.Bibaev
 */
class KotlinLambdaBody(override val lambdaArg: Expression, statementFactory: StatementFactory)
  : KotlinCodeBlock(statementFactory), LambdaBody

// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.debugger.streams.kotlin.psi

import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
interface StreamCallChecker {
  fun isIntermediateCall(expression: KtCallExpression): Boolean
  fun isTerminationCall(expression: KtCallExpression): Boolean

  fun isStreamCall(expression: KtCallExpression): Boolean = isIntermediateCall(expression) || isTerminationCall(expression)
}

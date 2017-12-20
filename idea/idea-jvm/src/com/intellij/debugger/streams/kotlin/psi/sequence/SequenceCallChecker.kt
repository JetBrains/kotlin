// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.sequence

import com.intellij.debugger.streams.kotlin.psi.StreamCallChecker
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
class SequenceCallChecker : StreamCallChecker {
  override fun isIntermediateCall(expression: KtCallExpression): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun isTerminationCall(expression: KtCallExpression): Boolean {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }
}
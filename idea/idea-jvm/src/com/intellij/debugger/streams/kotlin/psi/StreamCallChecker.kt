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

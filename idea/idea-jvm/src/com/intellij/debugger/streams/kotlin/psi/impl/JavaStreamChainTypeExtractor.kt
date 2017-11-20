package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor
import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor.IntermediateCallTypes
import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor.TerminatorCallTypes
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
class JavaStreamChainTypeExtractor : CallTypeExtractor {
  override fun extractIntermediateCallTypes(call: KtCallExpression): IntermediateCallTypes =
      IntermediateCallTypes(KotlinTypes.NULLABLE_ANY, KotlinTypes.NULLABLE_ANY)

  override fun extractTerminalCallTypes(call: KtCallExpression): TerminatorCallTypes =
      TerminatorCallTypes(KotlinTypes.NULLABLE_ANY, KotlinTypes.NULLABLE_ANY)
}
package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
class KotlinCollectionsTypeExtractor : CallTypeExtractor {
  override fun extractIntermediateCallTypes(call: KtCallExpression): CallTypeExtractor.IntermediateCallTypes =
      CallTypeExtractor.IntermediateCallTypes(KotlinTypes.NULLABLE_ANY, KotlinTypes.NULLABLE_ANY)

  override fun extractTerminalCallTypes(call: KtCallExpression): CallTypeExtractor.TerminatorCallTypes =
      CallTypeExtractor.TerminatorCallTypes(KotlinTypes.NULLABLE_ANY, KotlinTypes.NULLABLE_ANY)
}
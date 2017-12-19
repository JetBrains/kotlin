// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi

import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * @author Vitaliy.Bibaev
 */
interface CallTypeExtractor {
  fun extractIntermediateCallTypes(call: KtCallExpression): IntermediateCallTypes
  fun extractTerminalCallTypes(call: KtCallExpression): TerminatorCallTypes

  data class IntermediateCallTypes(val typeBefore: GenericType, val typeAfter: GenericType)
  data class TerminatorCallTypes(val typeBefore: GenericType, val resultType: GenericType)
}
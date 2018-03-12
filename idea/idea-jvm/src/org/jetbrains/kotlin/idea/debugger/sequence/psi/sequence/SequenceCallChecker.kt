// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.sequence

import com.intellij.debugger.streams.kotlin.psi.KotlinPsiUtil
import com.intellij.debugger.streams.kotlin.psi.StreamCallChecker
import com.intellij.debugger.streams.kotlin.psi.receiverType
import com.intellij.debugger.streams.kotlin.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.supertypes

/**
 * @author Vitaliy.Bibaev
 */
class SequenceCallChecker : StreamCallChecker {
  override fun isIntermediateCall(expression: KtCallExpression): Boolean {
    val receiverType = expression.receiverType() ?: return false
    return isSequenceInheritor(receiverType) && isSequenceInheritor(expression.resolveType())
  }

  override fun isTerminationCall(expression: KtCallExpression): Boolean {
    val receiverType = expression.receiverType() ?: return false
    return isSequenceInheritor(receiverType) && !isSequenceInheritor(expression.resolveType())
  }

  private fun isSequenceInheritor(type: KotlinType): Boolean =
      isSequenceType(type) || type.supertypes().any(this::isSequenceType)

  private fun isSequenceType(type: KotlinType): Boolean =
      "kotlin.sequences.Sequence" == KotlinPsiUtil.getTypeWithoutTypeParameters(type)
}
// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor
import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor.IntermediateCallTypes
import com.intellij.debugger.streams.kotlin.psi.CallTypeExtractor.TerminatorCallTypes
import com.intellij.debugger.streams.kotlin.psi.KotlinPsiUtil
import com.intellij.debugger.streams.kotlin.psi.receiverType
import com.intellij.debugger.streams.kotlin.psi.resolveType
import com.intellij.debugger.streams.kotlin.trace.dsl.KotlinTypes
import com.intellij.debugger.streams.trace.impl.handler.type.ClassTypeImpl
import com.intellij.debugger.streams.trace.impl.handler.type.GenericType
import com.intellij.psi.CommonClassNames
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.getImmediateSuperclassNotAny

/**
 * @author Vitaliy.Bibaev
 */
class JavaStreamChainTypeExtractor : CallTypeExtractor {
  override fun extractIntermediateCallTypes(call: KtCallExpression): IntermediateCallTypes {
    val resultType = call.resolveType()
    val receiverType = call.receiverType()
    return IntermediateCallTypes(extractItemsType(receiverType), extractItemsType(resultType))
  }

  override fun extractTerminalCallTypes(call: KtCallExpression): TerminatorCallTypes =
      TerminatorCallTypes(extractItemsType(call.receiverType()), getResultType(call.resolveType()))

  private fun extractItemsType(type: KotlinType?): GenericType {
    if (type == null) {
      return KotlinTypes.NULLABLE_ANY
    }

    return when (KotlinPsiUtil.getTypeWithoutTypeParameters(type)) {
      CommonClassNames.JAVA_UTIL_STREAM_INT_STREAM -> KotlinTypes.INT
      CommonClassNames.JAVA_UTIL_STREAM_DOUBLE_STREAM -> KotlinTypes.DOUBLE
      CommonClassNames.JAVA_UTIL_STREAM_LONG_STREAM -> KotlinTypes.LONG
      CommonClassNames.JAVA_UTIL_STREAM_BASE_STREAM -> KotlinTypes.NULLABLE_ANY
      else -> extractItemsType(type.getImmediateSuperclassNotAny())
    }
  }

  private fun getResultType(type: KotlinType): GenericType = ClassTypeImpl(KotlinPsiUtil.getTypeName(type))
}
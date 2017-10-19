// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.debugger.streams.kotlin.psi.impl

import com.intellij.debugger.streams.kotlin.psi.StreamCallChecker
import com.intellij.debugger.streams.kotlin.psi.receiverType
import com.intellij.debugger.streams.kotlin.psi.resolveType
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author Vitaliy.Bibaev
 */
class PackageBasedCallChecker(private val supportedPackage: String) : StreamCallChecker {
  override fun isIntermediateCall(expression: KtCallExpression): Boolean {
    return checkReceiverSupported(expression) && checkResultSupported(expression, true)
  }

  override fun isTerminationCall(expression: KtCallExpression): Boolean {
    return checkReceiverSupported(expression) && checkResultSupported(expression, false)
  }

  private fun checkResultSupported(expression: KtCallExpression,
                                   shouldSupportResult: Boolean): Boolean {
    val resultType = expression.resolveType()
    return shouldSupportResult == isSupportedType(resultType)
  }

  private fun checkReceiverSupported(expression: KtCallExpression): Boolean {
    val receiverType = expression.receiverType()
    return receiverType != null && isSupportedType(receiverType)
  }

  private fun isSupportedType(type: KotlinType): Boolean {
    val typeName = type.getJetTypeFqName(false)
    return StringUtil.getPackageName(typeName).startsWith(supportedPackage)
  }
}
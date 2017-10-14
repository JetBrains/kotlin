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
    return checkCallSupported(expression, true, true)
  }

  override fun isTerminationCall(expression: KtCallExpression): Boolean {
    return checkCallSupported(expression, true, false)
  }

  private fun checkCallSupported(expression: KtCallExpression,
                                 shouldSupportReceiver: Boolean,
                                 shouldSupportResult: Boolean): Boolean {
    val receiverType = expression.receiverType()
    val resultType = expression.resolveType()

    return (receiverType == null || // there is no producer or producer is a static method
        shouldSupportReceiver == isSupportedType(receiverType)) && shouldSupportResult == isSupportedType(resultType)
  }

  private fun isSupportedType(type: KotlinType?): Boolean {
    if (type == null) {
      return false
    }

    val typeName = type.getJetTypeFqName(false)
    return StringUtil.getPackageName(typeName).startsWith(supportedPackage)
  }
}
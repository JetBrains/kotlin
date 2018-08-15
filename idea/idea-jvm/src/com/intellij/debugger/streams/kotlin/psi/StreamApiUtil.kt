package com.intellij.debugger.streams.kotlin.psi

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType

/**
 * @author Vitaliy.Bibaev
 */
object StreamApiUtil {
  fun isStreamCall(expression: KtCallExpression): Boolean {
    return isIntermediateStreamCall(expression) || isProducerStreamCall(expression) || isTerminationStreamCall(expression)
  }

  fun isProducerStreamCall(expression: KtCallExpression): Boolean {
    return checkCallSupported(expression, false, true)
  }

  private fun isIntermediateStreamCall(expression: KtCallExpression): Boolean {
    return checkCallSupported(expression, true, true)
  }

  fun isTerminationStreamCall(expression: KtCallExpression): Boolean {
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
    return StringUtil.getPackageName(typeName).startsWith("java.util.stream")
  }
}
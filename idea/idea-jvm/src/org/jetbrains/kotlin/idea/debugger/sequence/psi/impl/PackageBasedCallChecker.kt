// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence.psi.impl

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.idea.debugger.sequence.psi.KotlinPsiUtil
import org.jetbrains.kotlin.idea.debugger.sequence.psi.StreamCallChecker
import org.jetbrains.kotlin.idea.debugger.sequence.psi.receiverType
import org.jetbrains.kotlin.idea.debugger.sequence.psi.resolveType
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.types.KotlinType

class PackageBasedCallChecker(private val supportedPackage: String) : StreamCallChecker {
    override fun isIntermediateCall(expression: KtCallExpression): Boolean {
        return checkReceiverSupported(expression) && checkResultSupported(expression, true)
    }

    override fun isTerminationCall(expression: KtCallExpression): Boolean {
        return checkReceiverSupported(expression) && checkResultSupported(expression, false)
    }

    private fun checkResultSupported(
        expression: KtCallExpression,
        shouldSupportResult: Boolean
    ): Boolean {
        val resultType = expression.resolveType()
        return shouldSupportResult == isSupportedType(resultType)
    }

    private fun checkReceiverSupported(expression: KtCallExpression): Boolean {
        val receiverType = expression.receiverType()
        return receiverType != null && isSupportedType(receiverType)
    }

    private fun isSupportedType(type: KotlinType): Boolean {
        val typeName = KotlinPsiUtil.getTypeWithoutTypeParameters(type)
        return StringUtil.getPackageName(typeName).startsWith(supportedPackage)
    }
}
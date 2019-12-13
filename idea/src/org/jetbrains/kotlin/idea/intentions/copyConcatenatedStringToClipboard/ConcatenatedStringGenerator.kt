/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.copyConcatenatedStringToClipboard

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getTopmostParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class ConcatenatedStringGenerator {
    fun create(element: KtBinaryExpression): String {
        val binaryExpression = element.getTopmostParentOfType<KtBinaryExpression>() ?: element
        val stringBuilder = StringBuilder()
        binaryExpression.appendTo(stringBuilder)
        return stringBuilder.toString()
    }

    private fun KtBinaryExpression.appendTo(sb: StringBuilder) {
        left?.appendTo(sb)
        right?.appendTo(sb)
    }

    private fun KtExpression.appendTo(sb: StringBuilder) {
        when (this) {
            is KtBinaryExpression -> this.appendTo(sb)
            is KtConstantExpression -> sb.append(text)
            is KtStringTemplateExpression -> this.appendTo(sb)
            else -> sb.append(convertToValueIfCompileTimeConstant() ?: "?")
        }
    }

    private fun KtStringTemplateExpression.appendTo(sb: StringBuilder) {
        collectDescendantsOfType<KtStringTemplateEntry>().forEach { stringTemplate ->
            when (stringTemplate) {
                is KtLiteralStringTemplateEntry -> sb.append(stringTemplate.text)
                is KtEscapeStringTemplateEntry -> sb.append(stringTemplate.unescapedValue)
                else -> sb.append(stringTemplate.expression?.convertToValueIfCompileTimeConstant() ?: "?")
            }
        }
    }

    private fun KtExpression.convertToValueIfCompileTimeConstant(): String? {
        val resolvedCall = resolveToCall(BodyResolveMode.FULL) ?: return null
        val propertyDescriptor = resolvedCall.resultingDescriptor as? PropertyDescriptor ?: return null
        return propertyDescriptor.compileTimeInitializer?.value?.toString()
    }

}
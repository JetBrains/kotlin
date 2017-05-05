/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.inspections.IntentionBasedInspection
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.isFlexible

class RemoveRedundantCallsOfConversionMethodsInspection : IntentionBasedInspection<KtQualifiedExpression>(
        RemoveRedundantCallsOfConversionMethodsIntention::class
) {
    override fun problemHighlightType(element: KtQualifiedExpression) = ProblemHighlightType.LIKE_UNUSED_SYMBOL
}

class RemoveRedundantCallsOfConversionMethodsIntention : SelfTargetingRangeIntention<KtQualifiedExpression>(KtQualifiedExpression::class.java, "Remove redundant calls of the conversion method") {

    private val targetClassMap = mapOf("toString()" to String::class.qualifiedName,
                                       "toDouble()" to Double::class.qualifiedName,
                                       "toFloat()" to Float::class.qualifiedName,
                                       "toLong()" to Long::class.qualifiedName,
                                       "toInt()" to Int::class.qualifiedName,
                                       "toChar()" to Char::class.qualifiedName,
                                       "toShort()" to Short::class.qualifiedName,
                                       "toByte()" to Byte::class.qualifiedName)


    override fun applyTo(element: KtQualifiedExpression, editor: Editor?) {
        element.replaced(element.receiverExpression)
    }

    override fun applicabilityRange(element: KtQualifiedExpression): TextRange? {
        val selectorExpression = element.selectorExpression ?: return null
        val selectorExpressionText = selectorExpression.text
        val qualifiedName = targetClassMap[selectorExpressionText] ?: return null
        if(element.receiverExpression.isApplicableReceiverExpression(qualifiedName)) {
            return selectorExpression.textRange
        } else {
            return null
        }
    }

    private fun KtExpression.isApplicableReceiverExpression(qualifiedName: String): Boolean {
        return when (this) {
            is KtStringTemplateExpression -> String::class.qualifiedName
            is KtConstantExpression -> getType(analyze())?.getJetTypeFqName(false)
            else -> {
                getResolvedCall(analyze())?.candidateDescriptor?.returnType?.let {
                    if (it.isFlexible()) null
                    else if (it.isMarkedNullable && parent !is KtSafeQualifiedExpression) null
                    else it.getJetTypeFqName(false)
                }
            }
        } == qualifiedName
    }
}

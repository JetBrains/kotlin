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

package org.jetbrains.kotlin.idea.intentions.copyConcatenatedStringToClipboard

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

class ConcatenatedStringGenerator {
    fun create(element: KtBinaryExpression): String {
        val binaryExpression = KtPsiUtil.getTopmostParentOfTypes(element, KtBinaryExpression::class.java) as? KtBinaryExpression ?: element
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
            else -> sb.append("?")
        }
    }

    private fun KtStringTemplateExpression.appendTo(sb: StringBuilder) {
        collectDescendantsOfType<KtStringTemplateEntry>().forEach {
            stringTemplate ->
            when (stringTemplate) {
                is KtLiteralStringTemplateEntry -> sb.append(stringTemplate.text)
                is KtEscapeStringTemplateEntry -> sb.append(stringTemplate.unescapedValue)
                else -> sb.append("?")
            }
        }
    }
}
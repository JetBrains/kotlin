/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.injection

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Trinity
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.intelliLang.inject.InjectedLanguage
import org.intellij.plugins.intelliLang.inject.InjectorUtils
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import java.util.*

data class InjectionSplitResult(val isUnparsable: Boolean, val ranges: List<Trinity<PsiLanguageInjectionHost, InjectedLanguage, TextRange>>)

fun splitLiteralToInjectionParts(injection: BaseInjection, literal: KtStringTemplateExpression): InjectionSplitResult? {
    InjectorUtils.getLanguage(injection) ?: return null

    val children = literal.children.toList()
    val len = children.size

    if (children.isEmpty()) return null

    val result = ArrayList<Trinity<PsiLanguageInjectionHost, InjectedLanguage, TextRange>>()

    fun addInjectionRange(range: TextRange, prefix: String, suffix: String) {
        TextRange.assertProperRange(range, injection)
        val injectedLanguage = InjectedLanguage.create(injection.injectedLanguageId, prefix, suffix, true)!!
        result.add(Trinity.create(literal, injectedLanguage, range))
    }

    fun suffix(i: Int) = if (i == len - 1) injection.suffix else ""

    var unparsable = false

    var prefix = injection.prefix

    var i = 0
    while (i < len) {
        val child = children[i]
        val partOffsetInParent = child.startOffsetInParent

        if (child is KtSimpleNameStringTemplateEntry || child is KtBlockStringTemplateEntry) {
            if (!prefix.isEmpty() || i == 0) {
                // Store part with prefix before replacing it
                addInjectionRange(TextRange.from(partOffsetInParent, 0), prefix, suffix(i))
            }

            prefix = when (child) {
                is KtSimpleNameStringTemplateEntry -> {
                    tryEvaluateConstant(child.expression) ?: run {
                        unparsable = true
                        child.expression?.text ?: NO_VALUE_NAME
                    }
                }
                is KtBlockStringTemplateEntry -> {
                    tryEvaluateConstant(child.expression) ?: run {
                        unparsable = true
                        NO_VALUE_NAME
                    }
                }
                else -> {
                    error("Child type should be KtSimpleNameStringTemplateEntry or KtBlockStringTemplateEntry")
                }
            }

            if (i == len - 1 && !prefix.isEmpty()) {
                // There won't be more elements, so create part with prefix right away
                addInjectionRange(TextRange.from(partOffsetInParent + child.textLength, 0), prefix, suffix(i))
            }
        } else {
            if (child is KtLiteralStringTemplateEntry || child is KtEscapeStringTemplateEntry) {
                // Merge all consecutive string literals into one part
                i += children.countFromWhile(i + 1) { it is KtLiteralStringTemplateEntry || it is KtEscapeStringTemplateEntry }
            } else {
                unparsable = true
            }

            val lastInPart = children[i]
            addInjectionRange(
                TextRange.create(partOffsetInParent, lastInPart.startOffsetInParent + lastInPart.textLength),
                prefix,
                suffix(i)
            )

            prefix = ""
        }

        i++
    }

    return InjectionSplitResult(unparsable, result)
}

private fun tryEvaluateConstant(ktExpression: KtExpression?) =
    ktExpression?.let { expression ->
        ConstantExpressionEvaluator.getConstant(expression, expression.analyze())
            ?.takeUnless { it.isError }
            ?.getValue(TypeUtils.NO_EXPECTED_TYPE)
            ?.safeAs<String>()
    }

private const val NO_VALUE_NAME = "missingValue"

private inline fun <T : Any> List<T>.countFromWhile(from: Int, predicate: (T) -> Boolean): Int {
    var i = from
    while (i < size) {
        if (!predicate(get(i))) {
            break
        }
        i++
    }

    return i - from
}

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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import org.intellij.plugins.intelliLang.inject.InjectedLanguage
import org.intellij.plugins.intelliLang.inject.InjectorUtils
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.jetbrains.kotlin.psi.*
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

    var unparsable = false

    var prefix = injection.prefix
    val lastChild = children.lastOrNull()

    var i = 0
    while (i < len) {
        val child = children[i]
        val partOffsetInParent = child.startOffsetInParent

        val part = when (child) {
            is KtLiteralStringTemplateEntry, is KtEscapeStringTemplateEntry -> {
                val partSize = children.subList(i, len).asSequence()
                        .takeWhile { it is KtLiteralStringTemplateEntry || it is KtEscapeStringTemplateEntry }
                        .count()
                i += partSize - 1
                children[i]
            }
            is KtSimpleNameStringTemplateEntry -> {
                unparsable = true
                child.expression?.text ?: NO_VALUE_NAME
            }
            is KtBlockStringTemplateEntry -> {
                unparsable = true
                NO_VALUE_NAME
            }
            else -> {
                unparsable = true
                child
            }
        }

        val suffix = if (child == lastChild) injection.suffix else ""

        if (part is PsiElement) {
            addInjectionRange(TextRange.create(partOffsetInParent, part.startOffsetInParent + part.textLength), prefix, suffix)
        }
        else if (!prefix.isEmpty() || i == 0) {
            addInjectionRange(TextRange.from(partOffsetInParent, 0), prefix, suffix)
        }

        prefix = part as? String ?: ""
        i++
    }

    if (lastChild != null && !prefix.isEmpty()) {
        // Last element was interpolated part, need to add a range after it
        addInjectionRange(TextRange.from(lastChild.startOffsetInParent + lastChild.textLength, 0), prefix, injection.suffix)
    }

    return InjectionSplitResult(unparsable, result)
}

private val NO_VALUE_NAME = "missingValue"

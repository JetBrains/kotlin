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

package org.jetbrains.kotlin.idea.refactoring.introduce

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.nextSiblingOfSameType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils

class ExtractableSubstringInfo(
        val startEntry: KtStringTemplateEntry,
        val endEntry: KtStringTemplateEntry,
        val prefix: String,
        val suffix: String,
        type: KotlinType? = null
) {
    private fun guessLiteralType(literal: String): KotlinType {
        val facade = template.getResolutionFacade()
        val module = facade.moduleDescriptor
        val stringType = module.builtIns.stringType

        if (startEntry != endEntry || startEntry !is KtLiteralStringTemplateEntry) return stringType

        val expr = KtPsiFactory(startEntry).createExpressionIfPossible(literal) ?: return stringType

        val context = facade.analyze(template, BodyResolveMode.PARTIAL)
        val scope = template.getResolutionScope(context, facade)

        val tempContext = expr.analyzeInContext(scope, template)
        val trace = DelegatingBindingTrace(tempContext, "Evaluate '$literal'")
        val languageVersionSettings = facade.getFrontendService(LanguageVersionSettings::class.java)
        val value = ConstantExpressionEvaluator(module, languageVersionSettings).evaluateExpression(expr, trace)
        if (value == null || value.isError) return stringType

        return value.toConstantValue(TypeUtils.NO_EXPECTED_TYPE).getType(module)
    }

    val template: KtStringTemplateExpression = startEntry.parent as KtStringTemplateExpression

    val content = with(entries.map { it.text }.joinToString(separator = "")) { substring(prefix.length, length - suffix.length) }

    val type = type ?: guessLiteralType(content)

    val contentRange: TextRange
        get() = TextRange(startEntry.startOffset + prefix.length, endEntry.endOffset - suffix.length)

    val relativeContentRange: TextRange
        get() = contentRange.shiftRight(-template.startOffset)

    val entries: Sequence<KtStringTemplateEntry>
        get() = generateSequence(startEntry) { if (it != endEntry) it.nextSiblingOfSameType() else null }

    fun createExpression(): KtExpression {
        val quote = template.firstChild.text
        val literalValue = if (KotlinBuiltIns.isString(type)) "$quote$content$quote" else content
        return KtPsiFactory(startEntry).createExpression(literalValue).apply { extractableSubstringInfo = this@ExtractableSubstringInfo }
    }

    fun copy(newTemplate: KtStringTemplateExpression): ExtractableSubstringInfo {
        val oldEntries = template.entries
        val newEntries = newTemplate.entries
        val startIndex = oldEntries.indexOf(startEntry)
        val endIndex = oldEntries.indexOf(endEntry)
        if (startIndex < 0 || startIndex >= newEntries.size || endIndex < 0 || endIndex >= newEntries.size) {
            throw AssertionError("Old template($startIndex..$endIndex): ${template.text}, new template: ${newTemplate.text}")
        }
        return ExtractableSubstringInfo(newEntries[startIndex], newEntries[endIndex], prefix, suffix, type)
    }
}

var KtExpression.extractableSubstringInfo: ExtractableSubstringInfo? by UserDataProperty(Key.create("EXTRACTED_SUBSTRING_INFO"))

val KtExpression.substringContextOrThis: KtExpression
    get() = extractableSubstringInfo?.template ?: this

val PsiElement.substringContextOrThis: PsiElement
    get() = (this as? KtExpression)?.extractableSubstringInfo?.template ?: this
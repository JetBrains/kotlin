/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.Expression
import com.intellij.codeInsight.template.ExpressionContext
import com.intellij.codeInsight.template.TextResult
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil

//TODO: move it somewhere else and reuse
abstract class ChooseValueExpression<T : Any>(
        lookupItems: Collection<T>,
        protected val defaultItem: T,
        private val advertisementText: String? = null
) : Expression() {

    protected abstract fun getLookupString(element: T): String
    protected abstract fun getResult(element: T): String

    protected val lookupItems: Array<LookupElement> = lookupItems.map { suggestion ->
        LookupElementBuilder.create(suggestion, getLookupString(suggestion)).withInsertHandler { context, item ->
            val topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(context.editor)
            val templateState = TemplateManagerImpl.getTemplateState(topLevelEditor)
            if (templateState != null) {
                val range = templateState.currentVariableRange
                if (range != null) {
                    @Suppress("UNCHECKED_CAST")
                    topLevelEditor.document.replaceString(range.startOffset, range.endOffset, getResult(item.`object` as T))
                }
            }
        }
    }.toTypedArray()

    override fun calculateLookupItems(context: ExpressionContext) = if (lookupItems.size > 1) lookupItems else null

    override fun calculateQuickResult(context: ExpressionContext) = calculateResult(context)

    override fun calculateResult(context: ExpressionContext) = TextResult(getLookupString(defaultItem))

    override fun getAdvertisingText() = advertisementText
}

class ChooseStringExpression(
        suggestions: Collection<String>,
        default: String = suggestions.first(),
        advertisementText: String? = null
) : ChooseValueExpression<String>(suggestions, default, advertisementText) {
    override fun getLookupString(element: String) = element
    override fun getResult(element: String) = element
}

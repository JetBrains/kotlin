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

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions.COMPARE_TO
import org.jetbrains.kotlin.util.OperatorNameConventions.CONTAINS
import org.jetbrains.kotlin.util.OperatorNameConventions.EQUALS
import org.jetbrains.kotlin.util.OperatorNameConventions.GET
import org.jetbrains.kotlin.util.OperatorNameConventions.INVOKE
import org.jetbrains.kotlin.util.OperatorNameConventions.SET

object OperatorNameCompletion {

    private val additionalOperatorPresentation = mapOf(
            SET to "[...] = ...",
            GET to "[...]",
            CONTAINS to "in !in",
            COMPARE_TO to "< > <= >=",
            EQUALS to "== !=",
            INVOKE to "(...)"
    )

    private fun buildLookupElement(opName: Name): LookupElement {
        val element = LookupElementBuilder.create(opName)

        val symbol =
                (OperatorConventions.getOperationSymbolForName(opName) as? KtSingleValueToken)?.value ?:
                additionalOperatorPresentation[opName]

        if (symbol != null) return element.withTypeText(symbol)
        return element
    }

    fun doComplete(collector: LookupElementsCollector, descriptorNameFilter: (String) -> Boolean) {
        collector.addElements(OperatorConventions.CONVENTION_NAMES
                                      .filter { descriptorNameFilter(it.asString()) }
                                      .map(this::buildLookupElement))
    }
}
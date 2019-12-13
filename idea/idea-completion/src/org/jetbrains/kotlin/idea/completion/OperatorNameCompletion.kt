/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
            (OperatorConventions.getOperationSymbolForName(opName) as? KtSingleValueToken)?.value ?: additionalOperatorPresentation[opName]

        if (symbol != null) return element.withTypeText(symbol)
        return element
    }

    fun doComplete(collector: LookupElementsCollector, descriptorNameFilter: (String) -> Boolean) {
        collector.addElements(OperatorConventions.CONVENTION_NAMES.filter { descriptorNameFilter(it.asString()) }
                                  .map(this::buildLookupElement))
    }
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtDenotableType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.render

internal class HighLevelApiLookupElementFactory {
    private val classLookupElementFactory = ClassLookupElementFactory()
    private val variableLookupElementFactory = VariableLookupElementFactory()
    private val functionLookupElementFactory = FunctionLookupElementFactory()

    fun createLookupElement(symbol: KtNamedSymbol): LookupElement {
        val elementBuilder = when (symbol) {
            is KtFunctionSymbol -> functionLookupElementFactory.createLookup(symbol)
            is KtVariableLikeSymbol -> variableLookupElementFactory.createLookup(symbol)
            is KtClassLikeSymbol -> classLookupElementFactory.createLookup(symbol)
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }

        return elementBuilder
            .withPsiElement(symbol.psi) // TODO check if it is a heavy operation and should be postponed
            .withIcon(KotlinSymbolIconProvider.getIconFor(symbol))
    }
}

/**
 * This is a temporary hack to prevent clash of the lookup elements with same names.
 */
private class UniqueLookupObject

private class ClassLookupElementFactory {
    fun createLookup(symbol: KtClassLikeSymbol): LookupElementBuilder {
        return LookupElementBuilder.create(UniqueLookupObject(), symbol.name.asString())
    }
}

private class VariableLookupElementFactory {
    fun createLookup(symbol: KtVariableLikeSymbol): LookupElementBuilder {
        return LookupElementBuilder.create(UniqueLookupObject(), symbol.name.asString())
            .withTypeText(ShortNamesRenderer.renderType(symbol.type))
            .withInsertHandler(createInsertHandler(symbol))
    }

    private fun createInsertHandler(symbol: KtVariableLikeSymbol): InsertHandler<LookupElement> {
        return QuotedNamesAwareInsertionHandler(symbol.name)
    }
}

private class FunctionLookupElementFactory {
    fun createLookup(symbol: KtFunctionSymbol): LookupElementBuilder {
        return LookupElementBuilder.create(UniqueLookupObject(), symbol.name.asString())
            .appendTailText(ShortNamesRenderer.renderFunctionParameters(symbol), true)
            .withTypeText(ShortNamesRenderer.renderType(symbol.type))
            .withInsertHandler(createInsertHandler(symbol))
    }

    private fun createInsertHandler(symbol: KtFunctionSymbol): InsertHandler<LookupElement> {
        return QuotedNamesAwareInsertionHandler(symbol.name)
    }
}

private open class QuotedNamesAwareInsertionHandler(private val name: Name) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val startOffset = context.startOffset
        if (startOffset > 0 && context.document.isTextAt(startOffset - 1, "`")) {
            context.document.deleteString(startOffset - 1, startOffset)
        }
        context.document.replaceString(context.startOffset, context.tailOffset, name.render())

        context.commitDocument()
    }
}


private object ShortNamesRenderer {
    fun renderFunctionParameters(function: KtFunctionSymbol): String =
        function.valueParameters.joinToString(", ", "(", ")") { renderFunctionParameter(it) }

    fun renderType(ktType: KtType): String = (ktType as? KtDenotableType)?.asString() ?: ""

    private fun renderFunctionParameter(param: KtFunctionParameterSymbol): String =
        "${if (param.isVararg) "vararg " else ""}${param.name.asString()}: ${renderType(param.type)}"
}

private fun Document.isTextAt(offset: Int, text: String) =
    offset + text.length <= textLength && getText(TextRange(offset, offset + text.length)) == text

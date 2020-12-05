/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.frontend.api.types.render
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.render

internal class KotlinFirLookupElementFactory {
    private val classLookupElementFactory = ClassLookupElementFactory()
    private val variableLookupElementFactory = VariableLookupElementFactory()
    private val functionLookupElementFactory = FunctionLookupElementFactory()
    private val typeParameterLookupElementFactory = TypeParameterLookupElementFactory()

    fun KtAnalysisSession.createLookupElement(symbol: KtNamedSymbol): LookupElement? {
        val elementBuilder = when (symbol) {
            is KtFunctionSymbol -> with(functionLookupElementFactory) { createLookup(symbol) }
            is KtVariableLikeSymbol -> variableLookupElementFactory.createLookup(symbol)
            is KtClassLikeSymbol -> classLookupElementFactory.createLookup(symbol)
            is KtTypeParameterSymbol -> typeParameterLookupElementFactory.createLookup(symbol)
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        } ?: return null

        return elementBuilder
            .withPsiElement(symbol.psi) // TODO check if it is a heavy operation and should be postponed
            .withIcon(KotlinFirIconProvider.getIconFor(symbol))
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

private class TypeParameterLookupElementFactory {
    fun createLookup(symbol: KtTypeParameterSymbol): LookupElementBuilder {
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
    fun KtAnalysisSession.createLookup(symbol: KtFunctionSymbol): LookupElementBuilder? {
        return try {
            LookupElementBuilder.create(UniqueLookupObject(), symbol.name.asString())
                .withTailText(getTailText(symbol), true)
                .withTypeText(ShortNamesRenderer.renderType(symbol.type))
                .withInsertHandler(createInsertHandler(symbol))
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            LOG.error(e)
            null
        }
    }

    private fun KtAnalysisSession.getTailText(symbol: KtFunctionSymbol): String {
        return if (insertLambdaBraces(symbol)) " {...}" else ShortNamesRenderer.renderFunctionParameters(symbol)
    }

    private fun KtAnalysisSession.insertLambdaBraces(symbol: KtFunctionSymbol): Boolean {
        val singleParam = symbol.valueParameters.singleOrNull()
        return singleParam != null && !singleParam.hasDefaultValue && singleParam.type.isBuiltInFunctionalType()
    }

    private fun KtAnalysisSession.createInsertHandler(symbol: KtFunctionSymbol): InsertHandler<LookupElement> {
        return FunctionInsertionHandler(
            symbol.name,
            inputValueArguments = symbol.valueParameters.isNotEmpty(),
            insertEmptyLambda = insertLambdaBraces(symbol)
        )
    }

    companion object {
        private val LOG = logger<FunctionLookupElementFactory>()
    }
}

/**
 * A partial copy from `KotlinFunctionInsertHandler.Normal`.
 */
private class FunctionInsertionHandler(
    name: Name,
    private val inputValueArguments: Boolean,
    private val insertEmptyLambda: Boolean
) : QuotedNamesAwareInsertionHandler(name) {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val startOffset = context.startOffset
        val element = context.file.findElementAt(startOffset) ?: return

        addArguments(context, element)
    }

    private fun addArguments(context: InsertionContext, offsetElement: PsiElement) {
        val completionChar = context.completionChar
        if (completionChar == '(') { //TODO: more correct behavior related to braces type
            context.setAddCompletionChar(false)
        }

        var offset = context.tailOffset
        val document = context.document
        val editor = context.editor
        val project = context.project
        val chars = document.charsSequence

        val isSmartEnterCompletion = completionChar == Lookup.COMPLETE_STATEMENT_SELECT_CHAR
        val isReplaceCompletion = completionChar == Lookup.REPLACE_SELECT_CHAR

        val (openingBracket, closingBracket) = if (insertEmptyLambda) '{' to '}' else '(' to ')'

        if (isReplaceCompletion) {
            val offset1 = chars.skipSpaces(offset)
            if (offset1 < chars.length) {
                if (chars[offset1] == '<') {
                    val token = context.file.findElementAt(offset1)!!
                    if (token.node.elementType == KtTokens.LT) {
                        val parent = token.parent
                        /* if type argument list is on multiple lines this is more likely wrong parsing*/
                        if (parent is KtTypeArgumentList && parent.getText().indexOf('\n') < 0) {
                            offset = parent.endOffset
                        }
                    }
                }
            }
        }

        var openingBracketOffset = chars.indexOfSkippingSpace(openingBracket, offset)
        var closeBracketOffset = openingBracketOffset?.let { chars.indexOfSkippingSpace(closingBracket, it + 1) }

        if (openingBracketOffset == null) {
            if (insertEmptyLambda) {
                if (completionChar == ' ' || completionChar == '{') {
                    context.setAddCompletionChar(false)
                }

                document.insertString(offset, " {}")
            } else {
                if (isSmartEnterCompletion) {
                    document.insertString(offset, "(")
                } else {
                    document.insertString(offset, "()")
                }
            }
            context.commitDocument()

            openingBracketOffset = document.charsSequence.indexOfSkippingSpace(openingBracket, offset)!!
            closeBracketOffset = document.charsSequence.indexOfSkippingSpace(closingBracket, openingBracketOffset + 1)
        }

        if (shouldPlaceCaretInBrackets(completionChar) || closeBracketOffset == null) {
            editor.caretModel.moveToOffset(openingBracketOffset + 1)
            AutoPopupController.getInstance(project)?.autoPopupParameterInfo(editor, offsetElement)
        } else {
            editor.caretModel.moveToOffset(closeBracketOffset + 1)
        }
    }

    private fun shouldPlaceCaretInBrackets(completionChar: Char): Boolean {
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return inputValueArguments || insertEmptyLambda
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

    fun renderType(ktType: KtType): String = ktType.render()

    private fun renderFunctionParameter(param: KtFunctionParameterSymbol): String =
        "${if (param.isVararg) "vararg " else ""}${param.name.asString()}: ${renderType(param.type)}"
}

private fun Document.isTextAt(offset: Int, text: String) =
    offset + text.length <= textLength && getText(TextRange(offset, offset + text.length)) == text

private fun CharSequence.skipSpaces(index: Int): Int =
    (index until length).firstOrNull { val c = this[it]; c != ' ' && c != '\t' } ?: this.length

private fun CharSequence.indexOfSkippingSpace(c: Char, startIndex: Int): Int? {
    for (i in startIndex until this.length) {
        val currentChar = this[i]
        if (c == currentChar) return i
        if (currentChar != ' ' && currentChar != '\t') return null
    }
    return null
}

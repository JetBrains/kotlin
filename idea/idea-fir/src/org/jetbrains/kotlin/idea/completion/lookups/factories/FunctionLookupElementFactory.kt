/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.lookups.factories

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.lookups.*
import org.jetbrains.kotlin.idea.completion.lookups.ImportStrategy
import org.jetbrains.kotlin.idea.completion.lookups.CallableInsertionStrategy
import org.jetbrains.kotlin.idea.completion.lookups.CompletionShortNamesRenderer
import org.jetbrains.kotlin.idea.completion.lookups.QuotedNamesAwareInsertionHandler
import org.jetbrains.kotlin.idea.completion.lookups.addCallableImportIfRequired
import org.jetbrains.kotlin.idea.completion.lookups.shortenReferencesForFirCompletion
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.idea.completion.lookups.TailTextProvider.getTailText
import org.jetbrains.kotlin.idea.completion.lookups.TailTextProvider.insertLambdaBraces
import org.jetbrains.kotlin.idea.completion.lookups.CompletionShortNamesRenderer.renderFunctionParameters
import org.jetbrains.kotlin.idea.completion.lookups.CompletionShortNamesRenderer.TYPE_RENDERING_OPTIONS
import org.jetbrains.kotlin.idea.core.withRootPrefixIfNeeded
import org.jetbrains.kotlin.idea.frontend.api.components.KtDeclarationRendererOptions

internal class FunctionLookupElementFactory {
    fun KtAnalysisSession.createLookup(
        symbol: KtFunctionSymbol,
        options: CallableInsertionOptions,
    ): LookupElementBuilder {
        val lookupObject = FunctionCallLookupObject(
            symbol.name,
            options,
            renderFunctionParameters(symbol),
            inputValueArguments = symbol.valueParameters.isNotEmpty(),
            insertEmptyLambda = insertLambdaBraces(symbol),
        )

        val insertionHandler = when (val insertionStrategy = options.insertionStrategy) {
            CallableInsertionStrategy.AsCall -> FunctionInsertionHandler
            CallableInsertionStrategy.AsIdentifier -> QuotedNamesAwareInsertionHandler()
            is CallableInsertionStrategy.AsIdentifierCustom -> object : QuotedNamesAwareInsertionHandler() {
                override fun handleInsert(context: InsertionContext, item: LookupElement) {
                    super.handleInsert(context, item)
                    insertionStrategy.insertionHandlerAction(context)
                }
            }
        }

        return LookupElementBuilder.create(lookupObject, symbol.name.asString())
            .withTailText(getTailText(symbol), true)
            .withTypeText(symbol.annotatedType.type.render(TYPE_RENDERING_OPTIONS))
            .withInsertHandler(insertionHandler)
            .let { withSymbolInfo(symbol, it) }
    }
}


internal data class FunctionCallLookupObject(
    override val shortName: Name,
    override val options: CallableInsertionOptions,
    override val renderedDeclaration: String,
    val inputValueArguments: Boolean,
    val insertEmptyLambda: Boolean,
) : KotlinCallableLookupObject()


internal object FunctionInsertionHandler : QuotedNamesAwareInsertionHandler() {
    private fun addArguments(context: InsertionContext, offsetElement: PsiElement, lookupObject: FunctionCallLookupObject) {
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

        val (openingBracket, closingBracket) = if (lookupObject.insertEmptyLambda) '{' to '}' else '(' to ')'

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
        var inBracketsShift = 0

        if (openingBracketOffset == null) {
            if (lookupObject.insertEmptyLambda) {
                if (completionChar == ' ' || completionChar == '{') {
                    context.setAddCompletionChar(false)
                }

                if (isInsertSpacesInOneLineFunctionEnabled(context.file)) {
                    document.insertString(offset, " {  }")
                    inBracketsShift = 1
                } else {
                    document.insertString(offset, " {}")
                }
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

        if (shouldPlaceCaretInBrackets(completionChar, lookupObject) || closeBracketOffset == null) {
            editor.caretModel.moveToOffset(openingBracketOffset + 1 + inBracketsShift)
            AutoPopupController.getInstance(project)?.autoPopupParameterInfo(editor, offsetElement)
        } else {
            editor.caretModel.moveToOffset(closeBracketOffset + 1)
        }
    }

    private fun shouldPlaceCaretInBrackets(completionChar: Char, lookupObject: FunctionCallLookupObject): Boolean {
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return lookupObject.inputValueArguments || lookupObject.insertEmptyLambda
    }

    // FIXME Should be fetched from language settings (INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD), but we do not have them right now
    private fun isInsertSpacesInOneLineFunctionEnabled(file: PsiElement) = true

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        val lookupObject = item.`object` as FunctionCallLookupObject

        super.handleInsert(context, item)

        val startOffset = context.startOffset
        val element = context.file.findElementAt(startOffset) ?: return

        val importStrategy = lookupObject.options.importingStrategy
        if (importStrategy is ImportStrategy.InsertFqNameAndShorten) {
            context.document.replaceString(
                context.startOffset,
                context.tailOffset,
                importStrategy.fqName.withRootPrefixIfNeeded().render()
            )
            context.commitDocument()

            addArguments(context, element, lookupObject)
            context.commitDocument()

            shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
        } else {
            addArguments(context, element, lookupObject)
            context.commitDocument()

            if (importStrategy is ImportStrategy.AddImport) {
                addCallableImportIfRequired(targetFile, importStrategy.nameToImport)
            }
        }
    }
}

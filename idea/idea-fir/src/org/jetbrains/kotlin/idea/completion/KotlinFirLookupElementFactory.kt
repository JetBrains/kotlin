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
import org.jetbrains.kotlin.idea.core.withRootPrefixIfNeeded
import org.jetbrains.kotlin.idea.frontend.api.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyze
import org.jetbrains.kotlin.idea.frontend.api.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtFunctionalType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
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
            is KtVariableLikeSymbol -> with(variableLookupElementFactory) { createLookup(symbol) }
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

private data class ClassifierLookupObject(val shortName: Name, val classId: ClassId?)

/**
 * Simplest lookup object so two lookup elements for the same function will clash.
 */
private data class FunctionLookupObject(val name: Name, val callableIdIfNonLocal: FqName?, val renderedFunctionParameters: String)

/**
 * Simplest lookup object so two lookup elements for the same property will clash.
 */
private data class VariableLookupObject(val name: Name, val callableIdIfNonLocal: FqName?)

private class ClassLookupElementFactory {
    fun createLookup(symbol: KtClassLikeSymbol): LookupElementBuilder {
        return LookupElementBuilder.create(ClassifierLookupObject(symbol.name, symbol.classIdIfNonLocal), symbol.name.asString())
            .withInsertHandler(createInsertHandler(symbol))
    }

    private fun createInsertHandler(symbol: KtClassLikeSymbol): InsertHandler<LookupElement> {
        val classFqName = symbol.classIdIfNonLocal?.asSingleFqName()
            ?: return QuotedNamesAwareInsertionHandler(symbol.name)

        return ClassifierInsertionHandler(classFqName)
    }
}

private class TypeParameterLookupElementFactory {
    fun createLookup(symbol: KtTypeParameterSymbol): LookupElementBuilder {
        return LookupElementBuilder.create(UniqueLookupObject(), symbol.name.asString())
    }
}

private class VariableLookupElementFactory {
    fun KtAnalysisSession.createLookup(symbol: KtVariableLikeSymbol): LookupElementBuilder {
        val lookupObject = VariableLookupObject(symbol.name, symbol.callableIdIfExists)

        return LookupElementBuilder.create(lookupObject, symbol.name.asString())
            .withTypeText(symbol.annotatedType.type.render())
            .markIfSyntheticJavaProperty(symbol)
            .withInsertHandler(createInsertHandler(symbol))
    }

    private fun LookupElementBuilder.markIfSyntheticJavaProperty(symbol: KtVariableLikeSymbol): LookupElementBuilder = when (symbol) {
        is KtSyntheticJavaPropertySymbol -> {
            val getterName = symbol.javaGetterName.asString()
            val setterName = symbol.javaSetterName?.asString()
            this.withTailText((" (from ${buildSyntheticPropertyTailText(getterName, setterName)})"))
                .withLookupStrings(listOfNotNull(getterName, setterName))
        }
        else -> this
    }

    private fun buildSyntheticPropertyTailText(getterName: String, setterName: String?): String =
        if (setterName != null) "$getterName()/$setterName()" else "$getterName()"

    private fun createInsertHandler(symbol: KtVariableLikeSymbol): InsertHandler<LookupElement> {
        val callableId = symbol.callableIdIfExists ?: return QuotedNamesAwareInsertionHandler(symbol.name)

        return ShorteningVariableInsertionHandler(callableId)
    }

    private val KtVariableLikeSymbol.callableIdIfExists: FqName?
        get() = when (this) {
            is KtJavaFieldSymbol -> callableIdIfNonLocal
            is KtKotlinPropertySymbol -> callableIdIfNonLocal
            is KtSyntheticJavaPropertySymbol -> callableIdIfNonLocal

            // Compiler will complain if there would be a new type in the hierarchy
            is KtEnumEntrySymbol,
            is KtLocalVariableSymbol,
            is KtFunctionParameterSymbol,
            is KtConstructorParameterSymbol,
            is KtSetterParameterSymbol -> null
        }
}

private class FunctionLookupElementFactory {
    fun KtAnalysisSession.createLookup(symbol: KtFunctionSymbol): LookupElementBuilder? {
        val lookupObject = FunctionLookupObject(
            symbol.name,
            symbol.callableIdIfNonLocal,
            with(ShortNamesRenderer) { renderFunctionParameters(symbol) }
        )

        return try {
            LookupElementBuilder.create(lookupObject, symbol.name.asString())
                .withTailText(getTailText(symbol), true)
                .withTypeText(symbol.annotatedType.type.render())
                .withInsertHandler(createInsertHandler(symbol))
        } catch (e: Throwable) {
            if (e is ControlFlowException) throw e
            LOG.error(e)
            null
        }
    }

    private fun KtAnalysisSession.getTailText(symbol: KtFunctionSymbol): String {
        return if (insertLambdaBraces(symbol)) " {...}" else with(ShortNamesRenderer) { renderFunctionParameters(symbol) }
    }

    private fun KtAnalysisSession.insertLambdaBraces(symbol: KtFunctionSymbol): Boolean {
        val singleParam = symbol.valueParameters.singleOrNull()
        return singleParam != null && !singleParam.hasDefaultValue && singleParam.annotatedType.type is KtFunctionalType
    }

    private fun KtAnalysisSession.createInsertHandler(symbol: KtFunctionSymbol): InsertHandler<LookupElement> {
        val functionFqName = symbol.callableIdIfNonLocal

        return if (functionFqName != null && canBeCalledByFqName(symbol)) {
            ShorteningFunctionInsertionHandler(
                functionFqName,
                inputValueArguments = symbol.valueParameters.isNotEmpty(),
                insertEmptyLambda = insertLambdaBraces(symbol)
            )
        } else {
            SimpleFunctionInsertionHandler(
                symbol.name,
                inputValueArguments = symbol.valueParameters.isNotEmpty(),
                insertEmptyLambda = insertLambdaBraces(symbol)
            )
        }
    }

    private fun canBeCalledByFqName(symbol: KtFunctionSymbol): Boolean {
        return !symbol.isExtension && symbol.dispatchType == null
    }

    companion object {
        private val LOG = logger<FunctionLookupElementFactory>()
    }
}

/**
 * The simplest implementation of the insertion handler for a classifiers.
 */
private class ClassifierInsertionHandler(private val fqName: FqName) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return

        context.document.replaceString(context.startOffset, context.tailOffset, fqName.render())
        context.commitDocument()

        shortenReferences(targetFile, TextRange(context.startOffset, context.tailOffset))
    }
}

private abstract class AbstractFunctionInsertionHandler(
    name: Name,
    private val inputValueArguments: Boolean,
    private val insertEmptyLambda: Boolean
) : QuotedNamesAwareInsertionHandler(name) {
    protected fun addArguments(context: InsertionContext, offsetElement: PsiElement) {
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

private class SimpleFunctionInsertionHandler(
    name: Name,
    inputValueArguments: Boolean,
    insertEmptyLambda: Boolean
) : AbstractFunctionInsertionHandler(name, inputValueArguments, insertEmptyLambda) {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        super.handleInsert(context, item)

        val startOffset = context.startOffset
        val element = context.file.findElementAt(startOffset) ?: return

        addArguments(context, element)
    }
}

private class ShorteningFunctionInsertionHandler(
    private val name: FqName,
    inputValueArguments: Boolean,
    insertEmptyLambda: Boolean,
) : AbstractFunctionInsertionHandler(name.shortName(), inputValueArguments, insertEmptyLambda) {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        super.handleInsert(context, item)

        val startOffset = context.startOffset
        val element = context.file.findElementAt(startOffset) ?: return

        context.document.replaceString(
            context.startOffset,
            context.tailOffset,
            name.withRootPrefixIfNeeded().render()
        )
        context.commitDocument()

        addArguments(context, element)
        context.commitDocument()

        shortenReferences(targetFile, TextRange(context.startOffset, context.tailOffset))
    }
}

private class ShorteningVariableInsertionHandler(private val name: FqName) : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return

        context.document.replaceString(
            context.startOffset,
            context.tailOffset,
            name.withRootPrefixIfNeeded().render()
        )
        context.commitDocument()

        shortenReferences(targetFile, TextRange(context.startOffset, context.tailOffset))
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
    fun KtAnalysisSession.renderFunctionParameters(function: KtFunctionSymbol): String =
        function.valueParameters.joinToString(", ", "(", ")") { renderFunctionParameter(it) }

    private fun KtAnalysisSession.renderFunctionParameter(param: KtFunctionParameterSymbol): String =
        "${if (param.isVararg) "vararg " else ""}${param.name.asString()}: ${param.annotatedType.type.render()}"
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

private fun shortenReferences(targetFile: KtFile, textRange: TextRange) {
    val shortenings = withAllowedResolve {
        analyze(targetFile) {
            collectPossibleReferenceShortenings(targetFile, textRange)
        }
    }

    shortenings.invokeShortening()
}

// FIXME: This is a hack, we should think how we can get rid of it
private inline fun <T> withAllowedResolve(action: () -> T): T {
    @OptIn(HackToForceAllowRunningAnalyzeOnEDT::class)
    return hackyAllowRunningOnEdt(action)
}
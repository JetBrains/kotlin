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
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.contributors.helpers.addDotAndInvokeCompletion
import org.jetbrains.kotlin.idea.completion.handlers.isTextAt
import org.jetbrains.kotlin.idea.core.asFqNameWithRootPrefixIfNeeded
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.analyse
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.idea.frontend.api.fir.utils.addImportToFile
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.tokens.HackToForceAllowRunningAnalyzeOnEDT
import org.jetbrains.kotlin.idea.frontend.api.tokens.hackyAllowRunningOnEdt
import org.jetbrains.kotlin.idea.frontend.api.types.KtFunctionalType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.miniStdLib.letIf
import org.jetbrains.kotlin.name.*
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
        return when (symbol) {
            is KtCallableSymbol -> createCallableLookupElement(symbol, importingStrategy = detectImportStrategy(symbol), CallableInsertionStrategy.AS_CALL)
            is KtClassLikeSymbol -> with(classLookupElementFactory) { createLookup(symbol) }
            is KtTypeParameterSymbol -> with(typeParameterLookupElementFactory) { createLookup(symbol) }
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }
    }

    fun KtAnalysisSession.createCallableLookupElement(
        symbol: KtCallableSymbol,
        importingStrategy: CallableImportStrategy,
        insertionStrategy: CallableInsertionStrategy,
    ): LookupElementBuilder? {
        return when (symbol) {
            is KtFunctionSymbol -> with(functionLookupElementFactory) { createLookup(symbol, importingStrategy, insertionStrategy) }
            is KtVariableLikeSymbol -> with(variableLookupElementFactory) { createLookup(symbol, importingStrategy) }
            else -> throw IllegalArgumentException("Cannot create a lookup element for $symbol")
        }
    }

    fun createPackagePartLookupElement(packagePartFqName: FqName): LookupElement {
        val shortName = packagePartFqName.shortName()
        return LookupElementBuilder.create(PackagePartLookupObject(shortName), "${shortName.render()}.")
            .withInsertHandler(PackagePartInsertionHandler)
            .withIcon(AllIcons.Nodes.Package)
            .letIf(!packagePartFqName.parent().isRoot) {
                it.appendTailText("(${packagePartFqName.asString()})", true)
            }
    }

    fun KtAnalysisSession.createLookupElementForClassLikeSymbol(symbol: KtClassLikeSymbol, insertFqName: Boolean = true): LookupElement? {
        if (symbol !is KtNamedSymbol) return null
        return with(classLookupElementFactory) { createLookup(symbol, insertFqName) }
    }
}

private fun KtAnalysisSession.withSymbolInfo(
    symbol: KtSymbol,
    elementBuilder: LookupElementBuilder
): LookupElementBuilder = elementBuilder
    .withPsiElement(symbol.psi) // TODO check if it is a heavy operation and should be postponed
    .withIcon(KotlinFirIconProvider.getIconFor(symbol))

internal sealed class CallableImportStrategy {
    object DoNothing : CallableImportStrategy()
    data class AddImport(val nameToImport: CallableId) : CallableImportStrategy()
    data class InsertFqNameAndShorten(val callableId: CallableId) : CallableImportStrategy()
}

/**
 * This is a temporary hack to prevent clash of the lookup elements with same names.
 */
private class UniqueLookupObject

private interface KotlinLookupObject {
    val shortName: Name
}

private data class PackagePartLookupObject(
    override val shortName: Name,
) : KotlinLookupObject

private data class ClassifierLookupObject(override val shortName: Name, val classId: ClassId?, val insertFqName: Boolean) :
    KotlinLookupObject

/**
 * Simplest lookup object so two lookup elements for the same function will clash.
 */
private data class FunctionLookupObject(
    override val shortName: Name,
    val importStrategy: CallableImportStrategy,
    val inputValueArguments: Boolean,
    val insertEmptyLambda: Boolean,
    // for distinction between different overloads
    private val renderedFunctionParameters: String
) : KotlinLookupObject

/**
 * Simplest lookup object so two lookup elements for the same property will clash.
 */
private data class VariableLookupObject(
    override val shortName: Name,
    val importStrategy: CallableImportStrategy
) : KotlinLookupObject

class ClassLookupElementFactory {
    fun KtAnalysisSession.createLookup(symbol: KtClassLikeSymbol, insertFqName: Boolean = true): LookupElementBuilder {
        val name = symbol.nameOrAnonymous
        return LookupElementBuilder.create(ClassifierLookupObject(name, symbol.classIdIfNonLocal, insertFqName), name.asString())
            .withInsertHandler(ClassifierInsertionHandler)
            .let { withSymbolInfo(symbol, it) }
    }
}

private class TypeParameterLookupElementFactory {
    fun KtAnalysisSession.createLookup(symbol: KtTypeParameterSymbol): LookupElementBuilder {
        return LookupElementBuilder
            .create(UniqueLookupObject(), symbol.name.asString())
            .let { withSymbolInfo(symbol, it) }
    }
}

private class VariableLookupElementFactory {
    fun KtAnalysisSession.createLookup(
        symbol: KtVariableLikeSymbol,
        importStrategy: CallableImportStrategy = detectImportStrategy(symbol)
    ): LookupElementBuilder {
        val lookupObject = VariableLookupObject(
            symbol.name,
            importStrategy = importStrategy
        )

        return LookupElementBuilder.create(lookupObject, symbol.name.asString())
            .withTypeText(symbol.annotatedType.type.render(WITH_TYPE_RENDERING_OPTIONS))
            .markIfSyntheticJavaProperty(symbol)
            .withInsertHandler(VariableInsertionHandler)
            .let { withSymbolInfo(symbol, it) }
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
}

private fun detectImportStrategy(symbol: KtCallableSymbol): CallableImportStrategy {
    if (symbol !is KtKotlinPropertySymbol || symbol.dispatchType != null) return CallableImportStrategy.DoNothing

    val propertyId = symbol.callableIdIfNonLocal ?: return CallableImportStrategy.DoNothing

    return if (symbol.isExtension) {
        CallableImportStrategy.AddImport(propertyId)
    } else {
        CallableImportStrategy.InsertFqNameAndShorten(propertyId)
    }
}

internal enum class CallableInsertionStrategy {
    AS_CALL,
    AS_IDENTIFIER
}

private class FunctionLookupElementFactory {
    fun KtAnalysisSession.createLookup(
        symbol: KtFunctionSymbol,
        importStrategy: CallableImportStrategy,
        insertionStrategy: CallableInsertionStrategy
    ): LookupElementBuilder? {
        val lookupObject = FunctionLookupObject(
            symbol.name,
            importStrategy = importStrategy,
            inputValueArguments = symbol.valueParameters.isNotEmpty(),
            insertEmptyLambda = insertLambdaBraces(symbol),
            renderedFunctionParameters = with(ShortNamesRenderer) { renderFunctionParameters(symbol) }
        )

        val insertionHandler = when (insertionStrategy) {
            CallableInsertionStrategy.AS_CALL -> FunctionInsertionHandler
            CallableInsertionStrategy.AS_IDENTIFIER -> QuotedNamesAwareInsertionHandler()
        }

        return try {
            LookupElementBuilder.create(lookupObject, symbol.name.asString())
                .withTailText(getTailText(symbol), true)
                .withTypeText(symbol.annotatedType.type.render(WITH_TYPE_RENDERING_OPTIONS))
                .withInsertHandler(insertionHandler)
                .let { withSymbolInfo(symbol, it) }
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

    companion object {
        private val LOG = logger<FunctionLookupElementFactory>()
    }
}

private val WITH_TYPE_RENDERING_OPTIONS = KtTypeRendererOptions.SHORT_NAMES

/**
 * The simplest implementation of the insertion handler for a classifiers.
 */
private object ClassifierInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        val lookupObject = item.`object` as ClassifierLookupObject

        if (lookupObject.classId != null && lookupObject.insertFqName) {
            val fqName = lookupObject.classId.asSingleFqName()

            context.document.replaceString(context.startOffset, context.tailOffset, fqName.render())
            context.commitDocument()

            shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
        }
    }
}

private object FunctionInsertionHandler : QuotedNamesAwareInsertionHandler() {
    private fun addArguments(context: InsertionContext, offsetElement: PsiElement, lookupObject: FunctionLookupObject) {
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

    private fun shouldPlaceCaretInBrackets(completionChar: Char, lookupObject: FunctionLookupObject): Boolean {
        if (completionChar == ',' || completionChar == '.' || completionChar == '=') return false
        if (completionChar == '(') return true
        return lookupObject.inputValueArguments || lookupObject.insertEmptyLambda
    }

    // FIXME Should be fetched from language settings (INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD), but we do not have them right now
    private fun isInsertSpacesInOneLineFunctionEnabled(file: PsiElement) = true

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        val lookupObject = item.`object` as FunctionLookupObject

        super.handleInsert(context, item)

        val startOffset = context.startOffset
        val element = context.file.findElementAt(startOffset) ?: return

        val importStrategy = lookupObject.importStrategy
        if (importStrategy is CallableImportStrategy.InsertFqNameAndShorten) {
            context.document.replaceString(
                context.startOffset,
                context.tailOffset,
                importStrategy.callableId.asFqNameWithRootPrefixIfNeeded().render()
            )
            context.commitDocument()

            addArguments(context, element, lookupObject)
            context.commitDocument()

            shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
        } else {
            addArguments(context, element, lookupObject)
            context.commitDocument()

            if (importStrategy is CallableImportStrategy.AddImport) {
                addCallableImportIfRequired(targetFile, importStrategy.nameToImport)
            }
        }
    }
}

private object VariableInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val targetFile = context.file as? KtFile ?: return
        val lookupObject = item.`object` as VariableLookupObject

        when (val importStrategy = lookupObject.importStrategy) {
            is CallableImportStrategy.AddImport -> {
                addCallableImportIfRequired(targetFile, importStrategy.nameToImport)
            }

            is CallableImportStrategy.InsertFqNameAndShorten -> {
                context.document.replaceString(
                    context.startOffset,
                    context.tailOffset,
                    importStrategy.callableId.asFqNameWithRootPrefixIfNeeded().render()
                )

                context.commitDocument()
                shortenReferencesForFirCompletion(targetFile, TextRange(context.startOffset, context.tailOffset))
            }

            is CallableImportStrategy.DoNothing -> {
            }
        }
    }
}

private object PackagePartInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val lookupElement = item.`object` as PackagePartLookupObject
        val name = lookupElement.shortName.render()
        context.document.replaceString(context.startOffset, context.tailOffset, name)
        context.commitDocument()
        context.addDotAndInvokeCompletion()
    }
}

private open class QuotedNamesAwareInsertionHandler : InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val lookupElement = item.`object` as KotlinLookupObject

        val startOffset = context.startOffset
        if (startOffset > 0 && context.document.isTextAt(startOffset - 1, "`")) {
            context.document.deleteString(startOffset - 1, startOffset)
        }
        context.document.replaceString(context.startOffset, context.tailOffset, lookupElement.shortName.render())

        context.commitDocument()
    }
}

private fun addCallableImportIfRequired(targetFile: KtFile, nameToImport: CallableId) {
    if (!alreadyHasImport(targetFile, nameToImport)) {
        addImportToFile(targetFile.project, targetFile, nameToImport)
    }
}

private fun alreadyHasImport(file: KtFile, nameToImport: CallableId): Boolean {
    if (file.importDirectives.any { it.importPath?.fqName == nameToImport.asSingleFqName() }) return true

    withAllowedResolve {
        analyse(file) {
            val scopes = file.getScopeContextForFile().scopes
            if (!scopes.mayContainName(nameToImport.callableName)) return false

            return scopes
                .getCallableSymbols { it == nameToImport.callableName }
                .any {
                    it is KtKotlinPropertySymbol && it.callableIdIfNonLocal == nameToImport ||
                            it is KtFunctionSymbol && it.callableIdIfNonLocal == nameToImport
                }
        }
    }
}

private object ShortNamesRenderer {
    fun KtAnalysisSession.renderFunctionParameters(function: KtFunctionSymbol): String =
        function.valueParameters.joinToString(", ", "(", ")") { renderFunctionParameter(it) }

    private fun KtAnalysisSession.renderFunctionParameter(param: KtValueParameterSymbol): String =
        "${if (param.isVararg) "vararg " else ""}${param.name.asString()}: ${param.annotatedType.type.render(WITH_TYPE_RENDERING_OPTIONS)}"
}


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

internal fun shortenReferencesForFirCompletion(targetFile: KtFile, textRange: TextRange) {
    val shortenings = withAllowedResolve {
        analyse(targetFile) {
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
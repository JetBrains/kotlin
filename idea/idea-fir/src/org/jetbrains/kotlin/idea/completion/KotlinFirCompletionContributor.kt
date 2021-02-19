/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.idea.frontend.api.InvalidWayOfUsingAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.getAnalysisSessionFor
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtCompositeScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.isExtension
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression

class KotlinFirCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), KotlinFirCompletionProvider)
    }
}

private object KotlinFirCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        if (shouldSuppressCompletion(parameters, result.prefixMatcher)) return

        val resultSet = createResultSet(parameters, result)
        val indexHelper = IndexHelper(
            parameters.position.project,
            parameters.position.getModuleInfo().contentScope()
        )

        KotlinCommonCompletionProvider(resultSet.prefixMatcher, indexHelper).addCompletions(parameters, resultSet)
    }

    private fun createResultSet(parameters: CompletionParameters, result: CompletionResultSet): CompletionResultSet =
        result.withRelevanceSorter(createSorter(parameters, result))

    private fun createSorter(parameters: CompletionParameters, result: CompletionResultSet): CompletionSorter =
        CompletionSorter.defaultSorter(parameters, result.prefixMatcher)
            .let(Weighers::addWeighersToCompletionSorter)

    private val AFTER_NUMBER_LITERAL = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText(""),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.FLOAT_LITERAL, KtTokens.INTEGER_LITERAL))
    )
    private val AFTER_INTEGER_LITERAL_AND_DOT = PsiJavaPatterns.psiElement().afterLeafSkipping(
        PsiJavaPatterns.psiElement().withText("."),
        PsiJavaPatterns.psiElement().withElementType(PsiJavaPatterns.elementType().oneOf(KtTokens.INTEGER_LITERAL))
    )

    private fun shouldSuppressCompletion(parameters: CompletionParameters, prefixMatcher: PrefixMatcher): Boolean {
        val position = parameters.position
        val invocationCount = parameters.invocationCount

        // no completion inside number literals
        if (AFTER_NUMBER_LITERAL.accepts(position)) return true

        // no completion auto-popup after integer and dot
        if (invocationCount == 0 && prefixMatcher.prefix.isEmpty() && AFTER_INTEGER_LITERAL_AND_DOT.accepts(position)) return true

        return false
    }
}

/**
 * Currently, this class is responsible for collecting all possible completion variants.
 *
 * TODO refactor it, try to split into several classes, or decompose it into several classes.
 */
private class KotlinCommonCompletionProvider(
    private val prefixMatcher: PrefixMatcher,
    private val indexHelper: IndexHelper
) {
    private val lookupElementFactory = KotlinFirLookupElementFactory()

    private val scopeNameFilter: KtScopeNameFilter =
        { name -> !name.isSpecial && prefixMatcher.prefixMatches(name.identifier) }

    private val shouldCompleteTopLevelCallablesFromIndex: Boolean
        get() = prefixMatcher.prefix.isNotEmpty()

    private fun KtAnalysisSession.addSymbolToCompletion(completionResultSet: CompletionResultSet, expectedType: KtType?, symbol: KtSymbol) {
        if (symbol !is KtNamedSymbol) return
        with(lookupElementFactory) {
            createLookupElement(symbol)
                ?.let { applyWeighers(it, symbol, expectedType) }
                ?.let(completionResultSet::addElement)
        }
    }

    private fun KtAnalysisSession.applyWeighers(
        lookupElement: LookupElement,
        symbol: KtSymbol,
        expectedType: KtType?
    ): LookupElement = lookupElement.apply {
        with(Weighers) { applyWeighsToLookupElement(lookupElement, symbol, expectedType) }
    }

    private fun recordOriginalFile(completionParameters: CompletionParameters) {
        val originalFile = completionParameters.originalFile as? KtFile ?: return
        val fakeFile = completionParameters.position.containingFile as? KtFile ?: return
        fakeFile.originalKtFile = originalFile
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val originalFile = parameters.originalFile as? KtFile ?: return
        recordOriginalFile(parameters)

        val reference = (parameters.position.parent as? KtSimpleNameExpression)?.mainReference ?: return
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression } ?: return

        val explicitReceiver = nameExpression.getReceiverExpression()

        with(getAnalysisSessionFor(originalFile).createContextDependentCopy(originalFile, nameExpression)) {
            val expectedType = nameExpression.getExpectedType()

            val (implicitScopes, _) = originalFile.getScopeContextForPosition(nameExpression)

            fun KtCallableSymbol.hasSuitableExtensionReceiver(): Boolean =
                checkExtensionIsSuitable(originalFile, nameExpression, explicitReceiver)

            when {
                nameExpression.parent is KtUserType -> collectTypesCompletion(result, implicitScopes, expectedType)
                explicitReceiver != null -> collectDotCompletion(
                    result,
                    implicitScopes,
                    explicitReceiver,
                    expectedType,
                    KtCallableSymbol::hasSuitableExtensionReceiver
                )
                else -> collectDefaultCompletion(result, implicitScopes, expectedType, KtCallableSymbol::hasSuitableExtensionReceiver)
            }
        }
    }

    private fun KtAnalysisSession.collectTypesCompletion(
        result: CompletionResultSet,
        implicitScopes: KtScope,
        expectedType: KtType?,
    ) {
        val classesFromScopes = implicitScopes.getClassifierSymbols(scopeNameFilter)
        classesFromScopes.forEach { addSymbolToCompletion(result, expectedType, it) }

        val kotlinClassesFromIndices = indexHelper.getKotlinClasses(scopeNameFilter, psiFilter = { it !is KtEnumEntry })
        kotlinClassesFromIndices.forEach { addSymbolToCompletion(result, expectedType, it.getSymbol()) }
    }

    private fun KtAnalysisSession.collectDotCompletion(
        result: CompletionResultSet,
        implicitScopes: KtCompositeScope,
        explicitReceiver: KtExpression,
        expectedType: KtType?,
        hasSuitableExtensionReceiver: KtCallableSymbol.() -> Boolean
    ) {
        val typeOfPossibleReceiver = explicitReceiver.getKtType()
        val possibleReceiverScope = typeOfPossibleReceiver.getTypeScope() ?: return

        val nonExtensionMembers = possibleReceiverScope
            .getCallableSymbols(scopeNameFilter)
            .filterNot { it.isExtension }

        val extensionNonMembers = implicitScopes
            .getCallableSymbols(scopeNameFilter)
            .filter { it.isExtension && it.hasSuitableExtensionReceiver() }

        nonExtensionMembers.forEach { addSymbolToCompletion(result, expectedType, it) }
        extensionNonMembers.forEach { addSymbolToCompletion(result, expectedType, it) }
    }

    private fun KtAnalysisSession.collectDefaultCompletion(
        result: CompletionResultSet,
        implicitScopes: KtCompositeScope,
        expectedType: KtType?,
        hasSuitableExtensionReceiver: KtCallableSymbol.() -> Boolean,
    ) {
        val availableNonExtensions = implicitScopes
            .getCallableSymbols(scopeNameFilter)
            .filterNot { it.isExtension }

        val extensionsWhichCanBeCalled = implicitScopes
            .getCallableSymbols(scopeNameFilter)
            .filter { it.isExtension && it.hasSuitableExtensionReceiver() }

        availableNonExtensions.forEach { addSymbolToCompletion(result, expectedType, it) }
        extensionsWhichCanBeCalled.forEach { addSymbolToCompletion(result, expectedType, it) }

        if (shouldCompleteTopLevelCallablesFromIndex) {
            val topLevelCallables = indexHelper.getTopLevelCallables(scopeNameFilter)
            topLevelCallables.asSequence()
                .map { it.getSymbol() }
                .forEach { addSymbolToCompletion(result, expectedType, it) }
        }


        collectTypesCompletion(result, implicitScopes, expectedType)
    }
}
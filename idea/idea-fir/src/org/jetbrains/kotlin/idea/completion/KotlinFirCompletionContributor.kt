/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirPositionCompletionContextDetector
import org.jetbrains.kotlin.idea.completion.context.FirUnknownPositionContext
import org.jetbrains.kotlin.idea.completion.contributors.FirClassifierCompletionContributor
import org.jetbrains.kotlin.idea.completion.contributors.FirCompletionContributorBase
import org.jetbrains.kotlin.idea.completion.contributors.FirContextCompletionContributorBase
import org.jetbrains.kotlin.idea.completion.contributors.FirKeywordCompletionContributor
import org.jetbrains.kotlin.idea.completion.weighers.Weighers
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalKtFile
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtScopeContext
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtCompositeScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

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

        val basicContext = FirBasicCompletionContext.createFromParameters(parameters, resultSet) ?: return
        recordOriginalFile(basicContext)
        val positionContext = FirPositionCompletionContextDetector.detect(basicContext)

        val keywordContributor = FirKeywordCompletionContributor(basicContext)

        FirPositionCompletionContextDetector.analyseInContext(basicContext, positionContext) {
            with(keywordContributor) { completeKeywords(positionContext) }
            when (positionContext) {
                is FirNameReferencePositionContext -> with(
                    KotlinWithNameReferenceCompletionProvider(basicContext, indexHelper)
                ) {
                    addCompletions(positionContext)
                }
                is FirUnknownPositionContext -> {
                }
            }
        }
    }


    private fun recordOriginalFile(basicCompletionContext: FirBasicCompletionContext) {
        val originalFile = basicCompletionContext.originalKtFile
        val fakeFile = basicCompletionContext.fakeKtFile
        fakeFile.originalKtFile = originalFile
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

internal fun interface ExtensionApplicabilityChecker {
    fun isApplicable(symbol: KtCallableSymbol): Boolean
}

internal fun interface CompletionVisibilityChecker {
    fun isVisible(symbol: KtSymbolWithVisibility): Boolean

    fun isVisible(symbol: KtCallableSymbol): Boolean {
        return symbol !is KtSymbolWithVisibility || isVisible(symbol as KtSymbolWithVisibility)
    }

    fun isVisible(symbol: KtClassifierSymbol): Boolean {
        return symbol !is KtSymbolWithVisibility || isVisible(symbol as KtSymbolWithVisibility)
    }
}

/**
 * Currently, this class is responsible for collecting all possible completion variants.
 *
 * TODO refactor it, try to split into several classes, or decompose it into several classes.
 */
private class KotlinWithNameReferenceCompletionProvider(
    basicContext: FirBasicCompletionContext,
    indexHelper: IndexHelper
) : FirContextCompletionContributorBase(basicContext, indexHelper) {
    private val typeNamesProvider = TypeNamesProvider(indexHelper)

    private val shouldCompleteTopLevelCallablesFromIndex: Boolean
        get() = prefixMatcher.prefix.isNotEmpty()


    fun KtAnalysisSession.addCompletions(
        positionContext: FirNameReferencePositionContext
    ) = with(positionContext) {
        val fileSymbol = originalKtFile.getFileSymbol()
        val expectedType = nameExpression.getExpectedType()

        val scopesContext = originalKtFile.getScopeContextForPosition(nameExpression)

        val extensionChecker = ExtensionApplicabilityChecker {
            it.checkExtensionIsSuitable(originalKtFile, nameExpression, explicitReceiver)
        }

        val visibilityChecker = CompletionVisibilityChecker {
            parameters.invocationCount > 1 || isVisible(
                it,
                fileSymbol,
                positionContext.explicitReceiver,
                positionContext.position
            )
        }

        when {
            nameExpression.parent is KtUserType -> with(FirClassifierCompletionContributor(basicContext, indexHelper)) {
                collectTypesCompletion(
                    scopesContext.scopes,
                    expectedType,
                    visibilityChecker
                )
            }
            explicitReceiver != null -> {
                collectDotCompletion(
                    scopesContext.scopes,
                    explicitReceiver,
                    expectedType,
                    extensionChecker,
                    visibilityChecker,
                )
            }

            else -> collectDefaultCompletion(scopesContext, expectedType, extensionChecker, visibilityChecker)
        }
    }


    private fun KtAnalysisSession.collectDotCompletion(
        implicitScopes: KtCompositeScope,
        explicitReceiver: KtExpression,
        expectedType: KtType?,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ) {
        val typeOfPossibleReceiver = explicitReceiver.getKtType()
        val possibleReceiverScope = typeOfPossibleReceiver.getTypeScope() ?: return

        val nonExtensionMembers = possibleReceiverScope.collectNonExtensions(visibilityChecker)
        val extensionNonMembers = implicitScopes.collectSuitableExtensions(extensionChecker, visibilityChecker)

        nonExtensionMembers.forEach { addSymbolToCompletion(expectedType, it) }
        extensionNonMembers.forEach { addSymbolToCompletion(expectedType, it) }

        collectTopLevelExtensionsFromIndices(listOf(typeOfPossibleReceiver), extensionChecker, visibilityChecker)
            .forEach { addSymbolToCompletion(expectedType, it) }
    }

    private fun KtAnalysisSession.collectDefaultCompletion(
        implicitScopesContext: KtScopeContext,
        expectedType: KtType?,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ) {
        val (implicitScopes, implicitReceivers) = implicitScopesContext
        val implicitReceiversTypes = implicitReceivers.map { it.type }

        val availableNonExtensions = implicitScopes.collectNonExtensions(visibilityChecker)
        val extensionsWhichCanBeCalled = implicitScopes.collectSuitableExtensions(extensionChecker, visibilityChecker)

        availableNonExtensions.forEach { addSymbolToCompletion(expectedType, it) }
        extensionsWhichCanBeCalled.forEach { addSymbolToCompletion(expectedType, it) }

        if (shouldCompleteTopLevelCallablesFromIndex) {
            val topLevelCallables = indexHelper.getTopLevelCallables(scopeNameFilter)
            topLevelCallables.asSequence()
                .map { it.getSymbol() as KtCallableSymbol }
                .filter { visibilityChecker.isVisible(it) }
                .forEach { addSymbolToCompletion(expectedType, it) }
        }

        collectTopLevelExtensionsFromIndices(implicitReceiversTypes, extensionChecker, visibilityChecker)
            .forEach { addSymbolToCompletion(expectedType, it) }

        with(FirClassifierCompletionContributor(basicContext, indexHelper)) {
            collectTypesCompletion(implicitScopes, expectedType, visibilityChecker)
        }
    }

    private fun KtAnalysisSession.collectTopLevelExtensionsFromIndices(
        receiverTypes: List<KtType>,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ): Sequence<KtCallableSymbol> {
        val implicitReceiverNames = findAllNamesOfTypes(receiverTypes)
        val topLevelExtensions = indexHelper.getTopLevelExtensions(scopeNameFilter, implicitReceiverNames)

        return topLevelExtensions.asSequence()
            .map { it.getSymbol() as KtCallableSymbol }
            .filter { visibilityChecker.isVisible(it) }
            .filter { extensionChecker.isApplicable(it) }
    }

    private fun KtScope.collectNonExtensions(visibilityChecker: CompletionVisibilityChecker) =
        getCallableSymbols(scopeNameFilter)
            .filterNot { it.isExtension }
            .filter { visibilityChecker.isVisible(it) }

    private fun KtCompositeScope.collectSuitableExtensions(
        hasSuitableExtensionReceiver: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ): Sequence<KtCallableSymbol> =
        getCallableSymbols(scopeNameFilter)
            .filter { it.isExtension }
            .filter { visibilityChecker.isVisible(it) }
            .filter { hasSuitableExtensionReceiver.isApplicable(it) }

    private fun KtAnalysisSession.findAllNamesOfTypes(implicitReceiversTypes: List<KtType>) =
        implicitReceiversTypes.flatMapTo(hashSetOf()) { with(typeNamesProvider) { findAllNames(it) } }
}

private class TypeNamesProvider(private val indexHelper: IndexHelper) {
    fun KtAnalysisSession.findAllNames(type: KtType): Set<String> {
        if (type !is KtClassType) return emptySet()

        val typeName = type.classId.shortClassName.let {
            if (it.isSpecial) return emptySet()
            it.identifier
        }

        val result = hashSetOf<String>()
        result += typeName
        result += indexHelper.getPossibleTypeAliasExpansionNames(typeName)

        val superTypes = (type.classSymbol as? KtClassOrObjectSymbol)?.superTypes
        superTypes?.forEach { superType ->
            result += findAllNames(superType.type)
        }

        return result
    }
}
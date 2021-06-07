/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.completion.checkers.ExtensionApplicabilityChecker
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.contributors.helpers.insertSymbolAndInvokeCompletion
import org.jetbrains.kotlin.idea.completion.lookups.CallableInsertionStrategy
import org.jetbrains.kotlin.idea.fir.HLIndexHelper
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.components.KtScopeContext
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtCompositeScope
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtExpression

internal open class FirCallableCompletionContributor(
    basicContext: FirBasicCompletionContext,
) : FirContextCompletionContributorBase<FirNameReferencePositionContext>(basicContext) {
    private val typeNamesProvider = TypeNamesProvider(indexHelper)

    protected open val insertionStrategy: CallableInsertionStrategy = CallableInsertionStrategy.AsCall

    protected open fun KtAnalysisSession.filter(symbol: KtCallableSymbol): Boolean = true

    private val shouldCompleteTopLevelCallablesFromIndex: Boolean
        get() = prefixMatcher.prefix.isNotEmpty()

    override fun KtAnalysisSession.complete(positionContext: FirNameReferencePositionContext): Unit = with(positionContext) {
        val visibilityChecker = CompletionVisibilityChecker.create(basicContext, positionContext)
        val expectedType = nameExpression.getExpectedType()
        val scopesContext = originalKtFile.getScopeContextForPosition(nameExpression)

        val extensionChecker = ExtensionApplicabilityChecker {
            it.checkExtensionIsSuitable(originalKtFile, nameExpression, explicitReceiver)
        }

        val receiver = explicitReceiver

        when {
            receiver != null -> {
                collectDotCompletion(
                    scopesContext.scopes,
                    receiver,
                    expectedType,
                    extensionChecker,
                    visibilityChecker,
                )
            }

            else -> completeWithoutReceiver(scopesContext, expectedType, extensionChecker, visibilityChecker)
        }
    }


    private fun KtAnalysisSession.completeWithoutReceiver(
        implicitScopesContext: KtScopeContext,
        expectedType: KtType?,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ) {
        val (implicitScopes, implicitReceivers) = implicitScopesContext
        val implicitReceiversTypes = implicitReceivers.map { it.type }

        val availableNonExtensions = collectNonExtensions(implicitScopes, visibilityChecker)
        val extensionsWhichCanBeCalled = collectSuitableExtensions(implicitScopes, extensionChecker, visibilityChecker)

        availableNonExtensions.forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }
        extensionsWhichCanBeCalled.forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }

        if (shouldCompleteTopLevelCallablesFromIndex) {
            val topLevelCallables = indexHelper.getTopLevelCallables(scopeNameFilter)
            topLevelCallables.asSequence()
                .map { it.getSymbol() as KtCallableSymbol }
                .filter { with(visibilityChecker) { isVisible(it) } }
                .forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }
        }

        collectTopLevelExtensionsFromIndices(implicitReceiversTypes, extensionChecker, visibilityChecker)
            .forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }
    }

    protected open fun KtAnalysisSession.collectDotCompletion(
        implicitScopes: KtCompositeScope,
        explicitReceiver: KtExpression,
        expectedType: KtType?,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ) {
        val symbol = explicitReceiver.reference()?.resolveToSymbol()
        when {
            symbol is KtPackageSymbol -> collectDotCompletionForPackageReceiver(symbol, expectedType, visibilityChecker)
            symbol is KtNamedClassOrObjectSymbol && !symbol.classKind.isObject && symbol.companionObject == null -> {
                // symbol cannot be used as callable receiver
            }
            else -> {
                collectDotCompletionForCallableReceiver(implicitScopes, explicitReceiver, expectedType, extensionChecker, visibilityChecker)
            }
        }
    }

    private fun KtAnalysisSession.collectDotCompletionForPackageReceiver(
        packageSymbol: KtPackageSymbol,
        expectedType: KtType?,
        visibilityChecker: CompletionVisibilityChecker
    ) {
        packageSymbol.getPackageScope()
            .getCallableSymbols(scopeNameFilter)
            .filterNot { it.isExtension }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .filter { filter(it) }
            .forEach { callable ->
                addCallableSymbolToCompletion(expectedType, callable, insertionStrategy = insertionStrategy)
            }
    }

    protected fun KtAnalysisSession.collectDotCompletionForCallableReceiver(
        implicitScopes: KtCompositeScope,
        explicitReceiver: KtExpression,
        expectedType: KtType?,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ) {
        val typeOfPossibleReceiver = explicitReceiver.getKtType()
        val possibleReceiverScope = typeOfPossibleReceiver.getTypeScope() ?: return

        val nonExtensionMembers = collectNonExtensions(possibleReceiverScope, visibilityChecker)
        val extensionNonMembers = collectSuitableExtensions(implicitScopes, extensionChecker, visibilityChecker)

        nonExtensionMembers
            .filter { filter(it) }
            .forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }

        extensionNonMembers
            .filter { filter(it) }
            .forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }

        collectTopLevelExtensionsFromIndices(listOf(typeOfPossibleReceiver), extensionChecker, visibilityChecker)
            .filter { filter(it) }
            .forEach { addCallableSymbolToCompletion(expectedType, it, insertionStrategy = insertionStrategy) }
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
            .filter { filter(it) }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .filter { with(extensionChecker) { isApplicable(it) } }
    }

    private fun KtAnalysisSession.collectNonExtensions(scope: KtScope, visibilityChecker: CompletionVisibilityChecker) =
        scope.getCallableSymbols(scopeNameFilter)
            .filterNot { it.isExtension }
            .filter { filter(it) }
            .filter { with(visibilityChecker) { isVisible(it) } }

    private fun KtAnalysisSession.collectSuitableExtensions(
        scope: KtCompositeScope,
        hasSuitableExtensionReceiver: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker,
    ): Sequence<KtCallableSymbol> =
        scope.getCallableSymbols(scopeNameFilter)
            .filter { it.isExtension }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .filter { filter(it) }
            .filter { with(hasSuitableExtensionReceiver) { isApplicable(it) } }

    private fun KtAnalysisSession.findAllNamesOfTypes(implicitReceiversTypes: List<KtType>) =
        implicitReceiversTypes.flatMapTo(hashSetOf()) { with(typeNamesProvider) { findAllNames(it) } }
}

internal class FirCallableReferenceCompletionContributor(
    basicContext: FirBasicCompletionContext
) : FirCallableCompletionContributor(basicContext) {
    override val insertionStrategy: CallableInsertionStrategy = CallableInsertionStrategy.AsIdentifier

    override fun KtAnalysisSession.collectDotCompletion(
        implicitScopes: KtCompositeScope,
        explicitReceiver: KtExpression,
        expectedType: KtType?,
        extensionChecker: ExtensionApplicabilityChecker,
        visibilityChecker: CompletionVisibilityChecker
    ) {
        when (val resolved = explicitReceiver.reference()?.resolveToSymbol()) {
            is KtPackageSymbol -> return
            is KtNamedClassOrObjectSymbol -> {
                resolved.getMemberScope()
                    .getCallableSymbols(scopeNameFilter)
                    .filter { with(visibilityChecker) { isVisible(it) } }
                    .forEach { symbol ->
                        addCallableSymbolToCompletion(expectedType = null, symbol, insertionStrategy = insertionStrategy)
                    }

            }
            else -> {
                collectDotCompletionForCallableReceiver(implicitScopes, explicitReceiver, expectedType, extensionChecker, visibilityChecker)
            }
        }
    }
}

internal class FirInfixCallableCompletionContributor(
    basicContext: FirBasicCompletionContext
) : FirCallableCompletionContributor(basicContext) {
    override val insertionStrategy: CallableInsertionStrategy = CallableInsertionStrategy.AsIdentifierCustom {
        insertSymbolAndInvokeCompletion(" ")
    }

    override fun KtAnalysisSession.filter(symbol: KtCallableSymbol): Boolean {
        return symbol is KtFunctionSymbol && symbol.isInfix
    }
}

private class TypeNamesProvider(private val indexHelper: HLIndexHelper) {
    fun KtAnalysisSession.findAllNames(type: KtType): Set<String> {
        if (type !is KtNonErrorClassType) return emptySet()

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
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.*
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.util.ProcessingContext
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

        KotlinAvailableScopesCompletionProvider(result.prefixMatcher).addCompletions(parameters, result)
    }

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

private class KotlinAvailableScopesCompletionProvider(prefixMatcher: PrefixMatcher) {
    private val lookupElementFactory = KotlinFirLookupElementFactory()

    private val scopeNameFilter: KtScopeNameFilter =
        { name -> !name.isSpecial && prefixMatcher.prefixMatches(name.identifier) }

    private fun CompletionResultSet.addSymbolToCompletion(symbol: KtSymbol) {
        if (symbol !is KtNamedSymbol) return
        addElement(lookupElementFactory.createLookupElement(symbol))
    }

    @OptIn(InvalidWayOfUsingAnalysisSession::class)
    fun addCompletions(parameters: CompletionParameters, result: CompletionResultSet) {
        val originalFile = parameters.originalFile as? KtFile ?: return

        val reference = (parameters.position.parent as? KtSimpleNameExpression)?.mainReference ?: return
        val nameExpression = reference.expression.takeIf { it !is KtLabelReferenceExpression } ?: return

        val explicitReceiver = nameExpression.getReceiverExpression()

        with(getAnalysisSessionFor(originalFile).createContextDependentCopy()) {
            val (implicitScopes, _) = originalFile.getScopeContextForPosition(nameExpression)

            fun KtCallableSymbol.hasSuitableExtensionReceiver(): Boolean =
                checkExtensionIsSuitable(originalFile, nameExpression, explicitReceiver)

            when {
                nameExpression.parent is KtUserType -> collectTypesCompletion(result, implicitScopes)
                explicitReceiver != null ->
                    collectDotCompletion(result, implicitScopes, explicitReceiver, KtCallableSymbol::hasSuitableExtensionReceiver)
                else -> collectDefaultCompletion(result, implicitScopes, KtCallableSymbol::hasSuitableExtensionReceiver)
            }
        }
    }

    private fun collectTypesCompletion(result: CompletionResultSet, implicitScopes: KtScope) {
        val availableClasses = implicitScopes.getClassifierSymbols(scopeNameFilter)
        availableClasses.forEach { result.addSymbolToCompletion(it) }
    }

    private fun KtAnalysisSession.collectDotCompletion(
        result: CompletionResultSet,
        implicitScopes: KtCompositeScope,
        explicitReceiver: KtExpression,
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

        nonExtensionMembers.forEach { result.addSymbolToCompletion(it) }
        extensionNonMembers.forEach { result.addSymbolToCompletion(it) }
    }

    private fun collectDefaultCompletion(
        result: CompletionResultSet,
        implicitScopes: KtCompositeScope,
        hasSuitableExtensionReceiver: KtCallableSymbol.() -> Boolean,
    ) {
        val availableNonExtensions = implicitScopes
            .getCallableSymbols(scopeNameFilter)
            .filterNot { it.isExtension }

        val extensionsWhichCanBeCalled = implicitScopes
            .getCallableSymbols(scopeNameFilter)
            .filter { it.isExtension && it.hasSuitableExtensionReceiver() }

        availableNonExtensions.forEach { result.addSymbolToCompletion(it) }
        extensionsWhichCanBeCalled.forEach { result.addSymbolToCompletion(it) }

        collectTypesCompletion(result, implicitScopes)
    }
}

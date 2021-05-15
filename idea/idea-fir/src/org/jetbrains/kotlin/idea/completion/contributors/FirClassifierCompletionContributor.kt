/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferenceRawPositionContext
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassLikeSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeParameterSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression

internal class FirClassifierCompletionContributor(
    basicContext: FirBasicCompletionContext,
) : FirContextCompletionContributorBase<FirNameReferenceRawPositionContext>(basicContext) {

    override fun KtAnalysisSession.complete(positionContext: FirNameReferenceRawPositionContext) {
        val visibilityChecker = CompletionVisibilityChecker.create(basicContext, positionContext)

        when (val receiver = positionContext.explicitReceiver) {
            null -> {
                completeWithoutReceiver(positionContext, visibilityChecker)
            }
            else -> {
                completeWithReceiver(receiver, visibilityChecker)
            }
        }
    }

    private fun KtAnalysisSession.completeWithReceiver(
        receiver: KtExpression,
        visibilityChecker: CompletionVisibilityChecker
    ) {
        val reference = receiver.mainReference ?: return
        val symbol = reference.resolveToSymbol() as? KtSymbolWithMembers ?: return
        symbol.getMemberScope()
            .getClassifierSymbols(scopeNameFilter)
            .filter { with(visibilityChecker) { isVisible(it) } }
            .forEach { addClassifierSymbolToCompletion(it, insertFqName = false) }
    }

    private fun KtAnalysisSession.completeWithoutReceiver(
        positionContext: FirNameReferenceRawPositionContext,
        visibilityChecker: CompletionVisibilityChecker
    ) {
        val implicitScopes = originalKtFile.getScopeContextForPosition(positionContext.nameExpression).scopes
        val classesFromScopes = implicitScopes
            .getClassifierSymbols(scopeNameFilter)
            .filter { with(visibilityChecker) { isVisible(it) } }

        classesFromScopes.forEach { addClassifierSymbolToCompletion(it, insertFqName = true) }

        val kotlinClassesFromIndices = indexHelper.getKotlinClasses(scopeNameFilter, psiFilter = { it !is KtEnumEntry })
        kotlinClassesFromIndices.asSequence()
            .map { it.getSymbol() as KtClassifierSymbol }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .forEach { addClassifierSymbolToCompletion(it, insertFqName = true) }
    }
}
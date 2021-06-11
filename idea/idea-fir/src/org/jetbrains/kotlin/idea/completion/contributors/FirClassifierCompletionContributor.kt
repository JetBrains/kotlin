/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.contributors.helpers.getStaticScope
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.idea.completion.contributors.helpers.FirClassifierProvider.getAvailableClassifiersCurrentScope
import org.jetbrains.kotlin.idea.frontend.api.types.KtNonErrorClassType

internal open class FirClassifierCompletionContributor(
    basicContext: FirBasicCompletionContext,
) : FirContextCompletionContributorBase<FirNameReferencePositionContext>(basicContext) {

    protected open fun KtAnalysisSession.filterClassifiers(classifierSymbol: KtClassifierSymbol): Boolean = true

    override fun KtAnalysisSession.complete(positionContext: FirNameReferencePositionContext) {
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
        val reference = receiver.reference() ?: return
        val scope = getStaticScope(reference) ?: return
        scope
            .getClassifierSymbols(scopeNameFilter)
            .filter { filterClassifiers(it) }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .forEach { addClassifierSymbolToCompletion(it, insertFqName = false) }
    }

    private fun KtAnalysisSession.completeWithoutReceiver(
        positionContext: FirNameReferencePositionContext,
        visibilityChecker: CompletionVisibilityChecker
    ) {
        getAvailableClassifiersCurrentScope(
            originalKtFile,
            positionContext.nameExpression,
            scopeNameFilter,
            indexHelper,
            visibilityChecker
        )
            .filter { filterClassifiers(it) }
            .forEach { addClassifierSymbolToCompletion(it, insertFqName = true) }
    }
}

internal class FirAnnotationCompletionContributor(
    basicContext: FirBasicCompletionContext
) : FirClassifierCompletionContributor(basicContext) {

    override fun KtAnalysisSession.filterClassifiers(classifierSymbol: KtClassifierSymbol): Boolean = when (classifierSymbol) {
        is KtAnonymousObjectSymbol -> false
        is KtTypeParameterSymbol -> false
        is KtNamedClassOrObjectSymbol -> when (classifierSymbol.classKind) {
            KtClassKind.ANNOTATION_CLASS -> true
            KtClassKind.ENUM_CLASS -> false
            KtClassKind.ENUM_ENTRY -> false
            KtClassKind.ANONYMOUS_OBJECT -> false
            KtClassKind.CLASS, KtClassKind.OBJECT, KtClassKind.COMPANION_OBJECT, KtClassKind.INTERFACE -> {
                // TODO show class if nested classifier is annotation class
                // classifierSymbol.getDeclaredMemberScope().getClassifierSymbols().any { filterClassifiers(it) }
                false
            }
        }
        is KtTypeAliasSymbol -> {
            val expendedClass = (classifierSymbol.expandedType as? KtNonErrorClassType)?.classSymbol
            expendedClass?.let { filterClassifiers(it) } == true
        }
    }
}
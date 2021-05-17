/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferenceRawPositionContext
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtNamedSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtSymbolWithMembers
import org.jetbrains.kotlin.idea.frontend.api.types.KtClassType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression

internal open class FirClassifierCompletionContributor(
    basicContext: FirBasicCompletionContext,
) : FirContextCompletionContributorBase<FirNameReferenceRawPositionContext>(basicContext) {

    protected open fun KtAnalysisSession.filterClassifiers(classifierSymbol: KtClassifierSymbol): Boolean = true

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
        val reference = receiver.reference() ?: return
        val scope = when (val symbol = reference.resolveToSymbol()) {
            is KtSymbolWithMembers -> symbol.getMemberScope()
            is KtPackageSymbol -> symbol.getPackageScope()
            else -> return
        }
        scope
            .getClassifierSymbols(scopeNameFilter)
            .filter { filterClassifiers(it) }
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
            .filter { filterClassifiers(it) }
            .filter { with(visibilityChecker) { isVisible(it) } }

        classesFromScopes.forEach { addClassifierSymbolToCompletion(it, insertFqName = true) }

        val kotlinClassesFromIndices = indexHelper.getKotlinClasses(scopeNameFilter, psiFilter = { it !is KtEnumEntry })
        kotlinClassesFromIndices.asSequence()
            .map { it.getSymbol() as KtClassifierSymbol }
            .filter { filterClassifiers(it) }
            .filter { with(visibilityChecker) { isVisible(it) } }
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
            val expendedClass = (classifierSymbol.expandedType as? KtClassType)?.classSymbol
            expendedClass?.let { filterClassifiers(it) } == true
        }
    }
}
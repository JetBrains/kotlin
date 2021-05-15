/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferenceRawPositionContext
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScope
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.idea.frontend.api.types.KtType
import org.jetbrains.kotlin.psi.KtEnumEntry

internal class FirClassifierCompletionContributor(
    basicContext: FirBasicCompletionContext,
    indexHelper: IndexHelper
) : FirContextCompletionContributorBase<FirNameReferenceRawPositionContext>(basicContext, indexHelper) {

    override fun KtAnalysisSession.complete(positionContext: FirNameReferenceRawPositionContext) {
        val implicitScopes = originalKtFile.getScopeContextForPosition(positionContext.nameExpression).scopes
        val visibilityChecker = CompletionVisibilityChecker.create(basicContext, positionContext)
        val classesFromScopes = implicitScopes
            .getClassifierSymbols(scopeNameFilter)
            .filter { with(visibilityChecker) { isVisible(it) } }

        classesFromScopes.forEach { addSymbolToCompletion(expectedType = null, it) }

        val kotlinClassesFromIndices = indexHelper.getKotlinClasses(scopeNameFilter, psiFilter = { it !is KtEnumEntry })
        kotlinClassesFromIndices.asSequence()
            .map { it.getSymbol() as KtClassifierSymbol }
            .filter { with(visibilityChecker) { isVisible(it) } }
            .forEach { addSymbolToCompletion(expectedType = null, it) }
    }
}
/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors.helpers

import org.jetbrains.kotlin.idea.completion.checkers.CompletionVisibilityChecker
import org.jetbrains.kotlin.idea.fir.low.level.api.IndexHelper
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile

internal object FirClassifierProvider {
    fun KtAnalysisSession.getAvailableClassifiersCurrentScope(
        originalKtFile: KtFile,
        position: KtElement,
        scopeNameFilter: KtScopeNameFilter,
        indexHelper: IndexHelper,
        visibilityChecker: CompletionVisibilityChecker
    ): Sequence<KtClassifierSymbol> = sequence {
        yieldAll(
            originalKtFile.getScopeContextForPosition(position).scopes
                .getClassifierSymbols(scopeNameFilter)
                .filter { with(visibilityChecker) { isVisible(it) } }
        )

        yieldAll(
            indexHelper.getKotlinClasses(scopeNameFilter, psiFilter = { it !is KtEnumEntry }).asSequence()
                .map { it.getSymbol() as KtClassifierSymbol }
                .filter { with(visibilityChecker) { isVisible(it) } }
        )
    }
}
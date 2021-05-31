/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferencePositionContext
import org.jetbrains.kotlin.idea.completion.context.FirRawPositionCompletionContext
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.isExcludedFromAutoImport
import org.jetbrains.kotlin.idea.project.languageVersionSettings

internal class FirPackageCompletionContributor(
    basicContext: FirBasicCompletionContext,
) : FirCompletionContributorBase<FirRawPositionCompletionContext>(basicContext) {

    override fun KtAnalysisSession.complete(positionContext: FirRawPositionCompletionContext) {
        val rootSymbol = if (positionContext !is FirNameReferencePositionContext || positionContext.explicitReceiver == null) {
            ROOT_PACKAGE_SYMBOL
        } else {
            positionContext.explicitReceiver?.reference()?.resolveToSymbol() as? KtPackageSymbol
        } ?: return
        rootSymbol.getPackageScope()
            .getPackageSymbols(scopeNameFilter)
            .filterNot { packageName ->
                packageName.fqName.isExcludedFromAutoImport(project, originalKtFile, originalKtFile.languageVersionSettings)
            }
            .forEach { packageSymbol ->
                sink.addElement(lookupElementFactory.createPackagePartLookupElement(packageSymbol.fqName))
            }
    }
}
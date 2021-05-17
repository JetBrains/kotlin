/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.contributors

import com.intellij.psi.JavaPsiFacade
import org.jetbrains.kotlin.idea.completion.context.FirBasicCompletionContext
import org.jetbrains.kotlin.idea.completion.context.FirNameReferenceRawPositionContext
import org.jetbrains.kotlin.idea.completion.context.FirRawPositionCompletionContext
import org.jetbrains.kotlin.idea.frontend.api.KtAnalysisSession
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.stubindex.PackageIndexUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

internal class FirPackageCompletionContributor(
    basicContext: FirBasicCompletionContext,
) : FirCompletionContributorBase<FirRawPositionCompletionContext>(basicContext) {

    override fun KtAnalysisSession.complete(positionContext: FirRawPositionCompletionContext) {
        val rootSymbol = if (positionContext !is FirNameReferenceRawPositionContext || positionContext.explicitReceiver == null) {
            ROOT_PACKAGE_SYMBOL
        } else {
            positionContext.explicitReceiver?.reference()?.resolveToSymbol() as? KtPackageSymbol
        } ?: return
        rootSymbol.getPackageScope()
            .getPackageSymbols(scopeNameFilter)
            .forEach { packageSymbol ->
                result.addElement(lookupElementFactory.createPackageLookupElement(packageSymbol.fqName))
            }
    }

    private fun KtExpression.reference() = when (this) {
        is KtDotQualifiedExpression -> selectorExpression?.mainReference
        else -> mainReference
    }
}
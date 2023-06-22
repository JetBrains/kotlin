/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components

import org.jetbrains.kotlin.analysis.api.KtSymbolBasedReference
import org.jetbrains.kotlin.analysis.api.components.KtReferenceResolveProvider
import org.jetbrains.kotlin.analysis.api.fir.KtFirAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirSafe
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference

internal class KtFirReferenceResolveProvider(
    override val analysisSession: KtFirAnalysisSession
) : KtReferenceResolveProvider(), KtFirAnalysisSessionComponent {
    override fun resolveToSymbols(reference: KtReference): Collection<KtSymbol> {
        check(reference is KtSymbolBasedReference) { "To get reference symbol the one should be KtSymbolBasedReference" }
        with(reference) {
            return analysisSession.resolveToSymbols()
        }
    }

    override fun isImplicitReferenceToCompanion(reference: KtReference): Boolean {
        if (reference !is KtSimpleNameReference) {
            return false
        }
        val referenceElement = reference.element
        val qualifier = referenceElement.getOrBuildFirSafe<FirResolvedQualifier>(analysisSession.firResolveSession) ?: return false
        return qualifier.resolvedToCompanionObject
    }
}
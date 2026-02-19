/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.references

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaAdditionalKDocResolutionProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement

/**
 * Adapter class to convert legacy [AdditionalKDocResolutionProvider][org.jetbrains.kotlin.analysis.api.symbols.AdditionalKDocResolutionProvider]
 * to [KaAdditionalKDocResolutionProvider].
 */
internal class KaAdditionalKDocResolutionProviderAdapter : KaAdditionalKDocResolutionProvider {
    override fun resolveKdocFqName(
        analysisSession: KaSession,
        fqName: FqName,
        contextElement: KtElement,
    ): Collection<KaSymbol> {
        @Suppress("DEPRECATION")
        return org.jetbrains.kotlin.analysis.api.symbols.AdditionalKDocResolutionProvider.resolveKdocFqName(
            analysisSession = analysisSession,
            fqName = fqName,
            contextElement = contextElement,
        )
    }
}

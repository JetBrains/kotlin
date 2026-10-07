/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.components.bridges

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.components.KaReferenceShortener
import org.jetbrains.kotlin.analysis.api.components.ShortenCommand
import org.jetbrains.kotlin.analysis.api.components.KaShortenOptions
import org.jetbrains.kotlin.analysis.api.components.KaShortenStrategy
import org.jetbrains.kotlin.analysis.api.impl.base.components.KaBaseSessionComponent
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.analysis.api.components.collectPossibleReferenceShortenings as collectPossibleReferenceShorteningsEndpoint
import org.jetbrains.kotlin.analysis.api.components.collectPossibleReferenceShorteningsInElement as collectPossibleReferenceShorteningsInElementEndpoint

internal class KaReferenceShortenerBridge(
    override val analysisSessionProvider: () -> KaSession,
) : KaBaseSessionComponent<KaSession>(), KaReferenceShortener {
    override fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange,
        shortenOptions: KaShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> KaShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> KaShortenStrategy,
    ): ShortenCommand =
        context(analysisSession) {
            collectPossibleReferenceShorteningsEndpoint(file, selection, shortenOptions, classShortenStrategy, callableShortenStrategy)
        }

    override fun collectPossibleReferenceShorteningsInElement(
        element: KtElement,
        shortenOptions: KaShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> KaShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> KaShortenStrategy,
    ): ShortenCommand =
        context(analysisSession) {
            collectPossibleReferenceShorteningsInElementEndpoint(element, shortenOptions, classShortenStrategy, callableShortenStrategy)
        }
}

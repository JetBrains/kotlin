/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.analysis.api.KaIdeApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.components.ShortenCommand
import org.jetbrains.kotlin.analysis.api.components.ShortenOptions
import org.jetbrains.kotlin.analysis.api.components.ShortenStrategy
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
@OptIn(KaIdeApi::class)
public interface KaInternalsReferenceShortener {
    public fun collectPossibleReferenceShortenings(
        file: KtFile,
        selection: TextRange,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy,
    ): ShortenCommand

    public fun collectPossibleReferenceShorteningsInElement(
        element: KtElement,
        shortenOptions: ShortenOptions,
        classShortenStrategy: (KaClassLikeSymbol) -> ShortenStrategy,
        callableShortenStrategy: (KaCallableSymbol) -> ShortenStrategy,
    ): ShortenCommand
}

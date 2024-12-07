/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement

/**
 * An extension point to provide additional symbols for a KDoc reference. KDoc link resolution will use symbols returned by this extension
 * point only if the real resolution was unsuccessful.
 *
 * #### Example
 *
 * Let's assume that you want to return symbol `fun foo() = 3` for the following KDoc resolution:
 *
 * ```
 * package com.example
 *
 * fun foo() = 3
 *
 * /**
 *  * [this.is.not.com.example.fo<caret>o] is not the above `com.example.foo`, but you want to resolve it to the above `com.example.foo`.
 *  */
 * fun bar() = 7
 * ```
 *
 * You can create the following provider:
 *
 * ```
 * class AdditionalKDocResolutionProviderBasedOnNameMatch : AdditionalKDocResolutionProvider {
 *   override fun resolveKdocFqName(analysisSession: KaSession, fqName: FqName, contextElement: KtElement): Collection<KaSymbol> =
 *     contextElement.containingKtFile.declarations.filter { it.name == fqName.shortName().asString() }.map { it.getSymbol() }
 * }
 * ```
 */
public interface AdditionalKDocResolutionProvider {
    /**
     * Returns additional symbols for the given [contextElement] in KDoc.
     */
    public fun resolveKdocFqName(analysisSession: KaSession, fqName: FqName, contextElement: KtElement): Collection<KaSymbol>

    public companion object {
        public val EP_NAME: ExtensionPointName<AdditionalKDocResolutionProvider> =
            ExtensionPointName<AdditionalKDocResolutionProvider>("org.jetbrains.kotlin.analysis.additionalKDocResolutionProvider")

        public fun resolveKdocFqName(analysisSession: KaSession, fqName: FqName, contextElement: KtElement): Collection<KaSymbol> =
            EP_NAME.extensions.flatMap { it.resolveKdocFqName(analysisSession, fqName, contextElement) }
    }
}

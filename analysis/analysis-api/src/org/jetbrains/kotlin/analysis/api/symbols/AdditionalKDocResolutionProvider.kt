/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.symbols

import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtElement

/**
 * An extension point to provide additional symbols for a KDoc reference. KDoc link resolution will use symbols returned by this EP
 * only if the real resolution was unsuccessful. You can use this EP by creating a class implementing this interface.
 * For example, let's assume that you want to return symbol `fun foo() = 3` for the following KDoc resolution:
 *
 * ```
 *   package com.example
 *   fun foo() = 3
 *   /**
 *    * [this.is.not.com.example.fo<caret>o] is not the above `com.example.foo`, but you want to resolve it to the above `com.example.foo`.
 *    */
 *   fun bar() = 7
 * ```
 *
 * You can create
 *
 * ```
 *   class AdditionalKDocResolutionProviderBasedOnNameMatch : AdditionalKDocResolutionProvider {
 *     context(KtAnalysisSession)
 *     override fun resolveKdocFqName(fqName: FqName, contextElement: KtElement): Collection<KtSymbol> =
 *       contextElement.containingKtFile.declarations.filter { it.name == fqName.shortName().asString() }.map { it.getSymbol() }
 *   }
 * ```
 */
public interface AdditionalKDocResolutionProvider {
    /**
     * This function must return additional symbols for [contextElement] in KDoc.
     */
    context(KtAnalysisSession)
    public fun resolveKdocFqName(fqName: FqName, contextElement: KtElement): Collection<KtSymbol>

    public companion object {
        public val EP_NAME: ExtensionPointName<AdditionalKDocResolutionProvider> =
            ExtensionPointName<AdditionalKDocResolutionProvider>("org.jetbrains.kotlin.analysis.additionalKDocResolutionProvider")

        context(KtAnalysisSession)
        public fun resolveKdocFqName(fqName: FqName, contextElement: KtElement): Collection<KtSymbol> =
            EP_NAME.extensions.flatMap { it.resolveKdocFqName(fqName, contextElement) }
    }
}
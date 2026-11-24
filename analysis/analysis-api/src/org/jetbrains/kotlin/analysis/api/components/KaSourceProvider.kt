/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.psi.KtDeclaration

@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaNonPublicApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSourceProvider : KaLifetimeOwner {
    /**
     * The source file name for the given [KtDeclaration] located in a Kotlin library (klib), or `null` if the declaration is not located in
     * a klib, or when the source file name is not available.
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.symbols.klibSourceFileName' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.klibSourceFileName",
            imports = [
                "org.jetbrains.kotlin.analysis.api.symbols.klibSourceFileName",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaNonPublicApi
    public val KaDeclarationSymbol.klibSourceFileName: String?
}

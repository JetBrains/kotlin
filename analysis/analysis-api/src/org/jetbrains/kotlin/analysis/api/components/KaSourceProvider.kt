/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.psi.KtDeclaration

@KaNonPublicApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaSourceProvider : KaSessionComponent {
    /**
     * The source file name for the given [KtDeclaration] located in a Kotlin library (klib), or `null` if the declaration is not located in
     * a klib, or when the source file name is not available.
     */
    @KaNonPublicApi
    public val KaDeclarationSymbol.klibSourceFileName: String?
}

/**
 * The source file name for the given [KtDeclaration] located in a Kotlin library (klib), or `null` if the declaration is not located in
 * a klib, or when the source file name is not available.
 */
@KaNonPublicApi
@Deprecated(
    message = "Use the 'org.jetbrains.kotlin.analysis.api.symbols' endpoint instead.",
    replaceWith = ReplaceWith(
        "this.klibSourceFileName",
        "org.jetbrains.kotlin.analysis.api.symbols.klibSourceFileName",
    ),
)
@KaContextParameterApi
context(session: KaSession)
public val KaDeclarationSymbol.klibSourceFileName: String?
    get() = with(session) { klibSourceFileName }

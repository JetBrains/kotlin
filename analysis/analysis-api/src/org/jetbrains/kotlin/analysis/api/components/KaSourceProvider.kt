/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.psi.KtDeclaration

@KaNonPublicApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaSourceProvider : KaSessionComponent {
    /**
     * The source file name for the given [KtDeclaration] located in a Kotlin library (klib), or `null` if the declaration is not located in
     * a klib, or when the source file name is not available.
     */
    @KaNonPublicApi
    public val KaDeclarationSymbol.klibSourceFileName: String?
}

/**
 * @see KaSourceProvider.klibSourceFileName
 */
@KaContextParameterApi
@KaNonPublicApi
context(context: KaSourceProvider)
public val KaDeclarationSymbol.klibSourceFileName: String?
    get() = with(context) { klibSourceFileName }

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.*
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
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
    @KaK1Unsupported
    public val KaDeclarationSymbol.klibSourceFileName: String?

    /**
     * File-level annotations (`@file:SomeAnnotation`) for the given [KaDeclarationSymbol] located in a Kotlin library
     * (klib), or `null` if the declaration is not located in a klib, or when file annotations are not available.
     */
    @KaNonPublicApi
    @KaK1Unsupported
    @KaExperimentalApi
    public val KaDeclarationSymbol.klibFileAnnotations: KaAnnotationList?
}

/**
 * The source file name for the given [KtDeclaration] located in a Kotlin library (klib), or `null` if the declaration is not located in
 * a klib, or when the source file name is not available.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaNonPublicApi
@KaK1Unsupported
@KaContextParameterApi
context(session: KaSession)
public val KaDeclarationSymbol.klibSourceFileName: String?
    get() = with(session) { klibSourceFileName }

/**
 * File-level annotations (`@file:SomeAnnotation`) for the given [KaDeclarationSymbol] located in a Kotlin library
 * (klib), or `null` if the declaration is not located in a klib, or when file annotations are not available.
 */
@KaNonPublicApi
@KaK1Unsupported
@KaContextParameterApi
@KaExperimentalApi
context(session: KaSession)
public val KaDeclarationSymbol.klibFileAnnotations: KaAnnotationList?
    get() = with(session) { klibFileAnnotations }

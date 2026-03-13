/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaK1Unsupported
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.name.ClassId
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
     * The class IDs of file-level annotations (`@file:SomeAnnotation`) for the given [KaDeclarationSymbol] located in a Kotlin library
     * (klib), or `null` if the declaration is not located in a klib, or when file annotations are not available.
     */
    @KaNonPublicApi
    @KaK1Unsupported
    public val KaDeclarationSymbol.klibFileAnnotationClassIds: List<ClassId>?
        get() = null
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
 * The class IDs of file-level annotations (`@file:SomeAnnotation`) for the given [KaDeclarationSymbol] located in a Kotlin library
 * (klib), or `null` if the declaration is not located in a klib, or when file annotations are not available.
 */
@KaNonPublicApi
@KaK1Unsupported
@KaContextParameterApi
context(session: KaSession)
public val KaDeclarationSymbol.klibFileAnnotationClassIds: List<ClassId>?
    get() = with(session) { klibFileAnnotationClassIds }

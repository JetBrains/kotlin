/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaK1Unsupported
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationList
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol

@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaFileAnnotationProvider : KaSessionComponent {

    /**
     * File-level annotations (`@file:SomeAnnotation`) of the source file this top-level declaration was defined in.
     * If the declaration is not top-level, returns an empty list.
     */
    @KaExperimentalApi
    @KaK1Unsupported
    public val KaDeclarationSymbol.fileAnnotations: KaAnnotationList
}

/**
 * File-level annotations (`@file:SomeAnnotation`) of the source file this top-level declaration was defined in.
 * If the declaration is not top-level, returns an empty list.
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaK1Unsupported
@KaContextParameterApi
context(session: KaSession)
public val KaDeclarationSymbol.fileAnnotations: KaAnnotationList
    get() = with(session) { fileAnnotations }

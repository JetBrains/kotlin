/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.psi.KtDeclaration

@KaNonPublicApi
public interface KaSourceProvider {
    /**
     * If [KtDeclaration] is a deserialized klib-based symbol, then information about the original
     * [SourceFile] is returned when available.
     */
    @KaNonPublicApi
    public val KaDeclarationSymbol.klibSourceFileName: String?
}
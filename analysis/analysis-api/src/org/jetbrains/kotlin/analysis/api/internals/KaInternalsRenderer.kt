/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.internals

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance

@KaImplementationDetail
@SubclassOptInRequired(KaImplementationDetail::class)
@OptIn(KaExperimentalApi::class)
public interface KaInternalsRenderer {
    public fun render(symbol: KaDeclarationSymbol, renderer: KaDeclarationRenderer): String

    public fun render(type: KaType, renderer: KaTypeRenderer, position: Variance): String
}

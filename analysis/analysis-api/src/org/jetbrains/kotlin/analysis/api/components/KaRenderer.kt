/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance

/**
 * Provides services for rendering Symbols and Types into the Kotlin strings
 */
@KaExperimentalApi
public interface KaRenderer {
    /**
     * Render symbol into the representable Kotlin string
     */
    @KaExperimentalApi
    public fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES): String

    /**
     * Render kotlin type into the representable Kotlin type string
     */
    @KaExperimentalApi
    public fun KaType.render(renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES, position: Variance): String
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.impl.KaDeclarationRendererForSource
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.impl.KaTypeRendererForSource
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.types.Variance

/**
 * Provides services for rendering [declaration symbols][KaDeclarationSymbol] and [types][KaType] to strings.
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaRenderer : KaSessionComponent {
    /**
     * Renders the given [KaDeclarationSymbol] to a string. The particular rendering strategy is defined by the [renderer].
     */
    @KaExperimentalApi
    public fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES): String

    /**
     * Renders the given [KaType] into a string. The particular rendering strategy is defined by the [renderer].
     *
     * If [position] is not invariant, the rendered type is approximated. Specifically, a denotable subtype is used for
     * [Variance.IN_VARIANCE], and a denotable supertype is used for [Variance.OUT_VARIANCE].
     */
    @KaExperimentalApi
    public fun KaType.render(renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES, position: Variance): String
}

/**
 * @see KaRenderer.render
 */
@KaContextParameterApi
@KaExperimentalApi
context(context: KaRenderer)
public fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES): String {
    return with(context) { render(renderer) }
}

/**
 * @see KaRenderer.render
 */
@KaContextParameterApi
@KaExperimentalApi
context(context: KaRenderer)
public fun KaType.render(renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES, position: Variance): String {
    return with(context) { render(renderer, position) }
}

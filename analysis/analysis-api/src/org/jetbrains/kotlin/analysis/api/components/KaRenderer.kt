/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaContextParameterApi
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaDeclarationRenderer
import org.jetbrains.kotlin.analysis.api.renderer.declarations.KaRendererTypeApproximator
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
     * [position] controls the way the given type is approximated using [KaTypeRenderer.typeApproximator].
     *
     * Specifically, when [KaRendererTypeApproximator.TO_DENOTABLE] is used, no approximation is performed for [Variance.INVARIANT],
     * a denotable subtype is rendered for [Variance.IN_VARIANCE], and a denotable supertype is rendered for [Variance.OUT_VARIANCE].
     */
    @KaExperimentalApi
    public fun KaType.render(renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES, position: Variance): String
}

/**
 * Renders the given [KaDeclarationSymbol] to a string. The particular rendering strategy is defined by the [renderer].
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KaDeclarationSymbol.render(renderer: KaDeclarationRenderer = KaDeclarationRendererForSource.WITH_QUALIFIED_NAMES): String {
    return with(s) {
        render(
            renderer = renderer,
        )
    }
}

/**
 * Renders the given [KaType] into a string. The particular rendering strategy is defined by the [renderer].
 *
 * [position] controls the way the given type is approximated using [KaTypeRenderer.typeApproximator].
 *
 * Specifically, when [KaRendererTypeApproximator.TO_DENOTABLE] is used, no approximation is performed for [Variance.INVARIANT],
 * a denotable subtype is rendered for [Variance.IN_VARIANCE], and a denotable supertype is rendered for [Variance.OUT_VARIANCE].
 */
// Auto-generated bridge. DO NOT EDIT MANUALLY!
@KaExperimentalApi
@KaContextParameterApi
context(s: KaSession)
public fun KaType.render(renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES, position: Variance): String {
    return with(s) {
        render(
            renderer = renderer,
            position = position,
        )
    }
}

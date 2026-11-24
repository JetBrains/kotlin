/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.analysis.api.components

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeOwner
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
@Deprecated(
    message = "Session components are an implementation detail of the legacy 'KaSession' API. Use the context-parameter API instead.",
    level = DeprecationLevel.WARNING,
)
@KaExperimentalApi
@KaSessionComponentImplementationDetail
@SubclassOptInRequired(KaSessionComponentImplementationDetail::class)
public interface KaRenderer : KaLifetimeOwner {
    /**
     * Renders the given [KaDeclarationSymbol] to a string. The particular rendering strategy is defined by the [renderer].
     */
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.renderer.render' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.render(renderer)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.renderer.render",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
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
    @Deprecated(
        message = "Use 'org.jetbrains.kotlin.analysis.api.renderer.render' from the context-parameter API instead.",
        replaceWith = ReplaceWith(
            expression = "this.render(renderer, position)",
            imports = [
                "org.jetbrains.kotlin.analysis.api.renderer.render",
            ]
        ),
        level = DeprecationLevel.WARNING,
    )
    @KaExperimentalApi
    public fun KaType.render(renderer: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES, position: Variance): String
}

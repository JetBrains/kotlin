/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import com.intellij.openapi.util.NlsSafe
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

/**
 * Renders Analysis API entities such as [KaSymbol]s or [KaType]s into a [KaRenderingOutput] as human-readable, Kotlin-like text.
 *
 * A renderer is an immutable bundle of [KaPieceRenderer]s (a stack per [KaPiece]) together with [KaRenderingOption] values.
 * You can obtain the standard renderer via [default] and derive customized variants with [copy].
 */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaRenderer {
    /** Renders the given [piece] [value] into the [output]. */
    context(session: KaSession)
    public fun <T> render(value: T, piece: KaPiece<T>, output: KaRenderingOutput)

    /**
     * Returns a copy of this renderer with the customizations applied by [block] (overriding [KaRenderingOption]s or pushing
     * additional [KaPieceRenderer]s). This renderer is left unchanged.
     *
     * Copying a renderer obtained from [default] does not require an initialized application, so the result can be cached in a class
     * initializer, such as a companion object property.
     */
    public fun copy(block: KaRendererBuilder.() -> Unit): KaRenderer

    @KaExperimentalApi
    public companion object {
        /**
         * The standard [KaRenderer], which renders declarations and types close to their Kotlin source form.
         *
         * Neither accessing this property nor deriving a renderer from it with [copy] requires an initialized application: the
         * engine-provided renderer is resolved on the first rendering call. Both are therefore safe to use from a class initializer, such
         * as when caching a customized renderer in a companion object property.
         */
        public val default: KaRenderer = KaDeferredRenderer(customizations = emptyList())
    }
}

/** Renders the [symbol] into [output]. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.render(symbol: KaSymbol, output: KaRenderingOutput) {
    render(symbol, KaPiece.Symbol, output)
}

/** Renders the [type] into [output]. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.render(type: KaType, output: KaRenderingOutput) {
    render(type, KaPiece.Type, output)
}

/** Renders the [symbol] into a string. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.renderToString(symbol: KaSymbol): @NlsSafe String {
    val output = KaRenderingOutput.plainString()
    render(symbol, output)
    return output.toString()
}

/** Renders the [type] into a string. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.renderToString(type: KaType): @NlsSafe String {
    val output = KaRenderingOutput.plainString()
    render(type, output)
    return output.toString()
}

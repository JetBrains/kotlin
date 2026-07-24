/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import com.intellij.openapi.components.service
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.internals.KaRendererProvider
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection

/**
 * Renders [KaSymbol]s, [KaType]s, [KaTypeProjection]s, and [KaConstantValue]s into a [KaRenderingOutput] as human-readable, Kotlin-like
 * text.
 *
 * A renderer is an immutable bundle of [KaPieceRenderer]s (a stack per [KaPiece]) together with [KaRenderingOption] values. Obtain the
 * standard renderer via [default] and derive customized variants with [copy].
 */
@KaExperimentalApi
public interface KaRenderer {
    /** Renders the declaration of [symbol] — its signature and modifiers, and, for a class, its body — into [output]. */
    context(session: KaSession)
    public fun render(symbol: KaSymbol, output: KaRenderingOutput)

    /** Renders [type] into [output]. */
    context(session: KaSession)
    public fun render(type: KaType, output: KaRenderingOutput)

    /** Renders [constantValue] into [output]. */
    context(session: KaSession)
    public fun render(constantValue: KaConstantValue, output: KaRenderingOutput)

    /**
     * Returns a copy of this renderer with the customizations applied by [block] (overriding [KaRenderingOption]s or pushing
     * additional [KaPieceRenderer]s). This renderer is left unchanged.
     */
    public fun copy(block: KaRendererBuilder.() -> Unit): KaRenderer

    @KaExperimentalApi
    public companion object {
        /** The standard [KaRenderer], which renders declarations and types close to their Kotlin source form. */
        public val default: KaRenderer
            get() {
                @OptIn(KaExperimentalApi::class)
                return service<KaRendererProvider>().defaultRenderer
            }
    }
}

/** Renders the [symbol] into a string. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.renderToString(symbol: KaSymbol): String {
    val output = KaRenderingOutput.plainString()
    render(symbol, output)
    return output.toString()
}

/** Renders the [type] into a string. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.renderToString(type: KaType): String {
    val output = KaRenderingOutput.plainString()
    render(type, output)
    return output.toString()
}

/** Renders the [constantValue] into a string. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.renderToString(constantValue: KaConstantValue): String {
    val output = KaRenderingOutput.plainString()
    render(constantValue, output)
    return output.toString()
}

/** A mutable builder for customizing a [KaRenderer], available within [KaRenderer.copy]. */
@KaExperimentalApi
public interface KaRendererBuilder {
    /** Pushes [renderer] on top of the stack for its [KaPieceRenderer.piece], so it takes precedence over the renderers below it. */
    public fun <T> push(renderer: KaPieceRenderer<T>)

    /** Overrides the value of [option]. */
    public fun <T> set(option: KaRenderingOption<T>, value: T)

    /** Resets [option] back to its [KaRenderingOption.defaultValue]. */
    public fun <T> unset(option: KaRenderingOption<T>)
}

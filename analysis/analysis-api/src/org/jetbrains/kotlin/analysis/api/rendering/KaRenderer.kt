/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import com.intellij.openapi.util.NlsSafe
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
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

/** Renders the [constantValue] into a string. */
@KaExperimentalApi
context(session: KaSession)
public fun KaRenderer.renderToString(constantValue: KaConstantValue): @NlsSafe String {
    val output = KaRenderingOutput.plainString()
    render(constantValue, output)
    return output.toString()
}

/** A mutable builder for customizing a [KaRenderer], available within [KaRenderer.copy]. */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaRendererBuilder {
    /** Pushes [renderer] on top of the stack for its [KaPieceRenderer.piece], so it takes precedence over the renderers below it. */
    public fun <T> push(renderer: KaPieceRenderer<T>)

    /** Returns the current effective value of [option]. */
    public fun <T> get(option: KaRenderingOption<T>): T

    /** Overrides the value of [option]. */
    public fun <T> set(option: KaRenderingOption<T>, value: T)

    /** Resets [option] back to its [KaRenderingOption.defaultValue]. */
    public fun <T> unset(option: KaRenderingOption<T>)
}

/**
 * Pushes a simple, always-consuming [KaPieceRenderer] for the given [piece], which is created using the provided rendering [block].
 */
@KaExperimentalApi
public fun <T> KaRendererBuilder.push(
    piece: KaPiece<T>,
    block: context(KaSession, KaRenderingContext, KaRenderingOutput) (T) -> Unit,
) {
    val renderer = object : KaPieceRenderer<T>(piece) {
        context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
        override fun render(value: T, next: () -> Unit): Boolean {
            block(value)
            return true
        }
    }
    push(renderer)
}

/**
 * Pushes a no-op, always-consuming [KaPieceRenderer] for the given [piece].
 */
@KaExperimentalApi
public fun <T> KaRendererBuilder.pushEmpty(piece: KaPiece<T>) {
    push(KaPieceRenderer.empty(piece))
}

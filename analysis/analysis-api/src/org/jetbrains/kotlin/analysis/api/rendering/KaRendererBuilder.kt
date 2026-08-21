/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession

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
 * Resets the [option] to a value returned from the [provider] using the previous effective value.
 */
@KaExperimentalApi
public inline fun <T> KaRendererBuilder.reset(option: KaRenderingOption<T>, provider: (T) -> T) {
    val oldValue = get(option)
    set(option, provider(oldValue))
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

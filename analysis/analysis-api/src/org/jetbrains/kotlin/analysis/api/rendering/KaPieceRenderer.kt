/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.KaSpi

/**
 * Renders a single [KaPiece] into the contextual [KaRenderingOutput].
 *
 * Renderers for a given [piece] form a stack within a [KaRenderer]; [KaRendererBuilder.push] adds one on top. A renderer may
 * emit nested pieces via the top-level [render] function and fall back to the renderer beneath it by invoking its `next` callback.
 *
 * @param piece the piece this renderer is responsible for.
 */
@KaSpi
@KaExperimentalApi
public abstract class KaPieceRenderer<T>(public val piece: KaPiece<T>) {
    /**
     * Renders [value] into the contextual [KaRenderingOutput]. Returns `true` if the value was handled, or `false` to fall back to
     * the renderer beneath this one.
     *
     * Invoke [next] to render [value] with the renderer immediately beneath this one. After calling [next], you are supposed to return
     * `true`; otherwise, the `next` will render the `value` twice.
     */
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    public abstract fun render(value: T, next: () -> Unit): Boolean

    @KaExperimentalApi
    public companion object {
        /**
         * The no-op renderer for the given [piece].
         */
        public fun <T> empty(piece: KaPiece<T>): KaPieceRenderer<T> {
            return object : KaPieceRenderer<T>(piece) {
                context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
                override fun render(value: T, next: () -> Unit): Boolean = true
            }
        }
    }
}

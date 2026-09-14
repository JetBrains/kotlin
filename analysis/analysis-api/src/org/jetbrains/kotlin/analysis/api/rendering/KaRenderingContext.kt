/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession

/** The rendering state passed to every [KaPieceRenderer], used to render nested pieces and to read [KaRenderingOption] values. */
@KaExperimentalApi
@SubclassOptInRequired(KaImplementationDetail::class)
public interface KaRenderingContext {
    /** Renders [value] as the given [piece], dispatching to the corresponding renderer stack. */
    context(session: KaSession)
    public fun <T> render(value: T, piece: KaPiece<T>)

    /**
     * Renders [value] as the given [piece], dispatching to the corresponding renderer stack, but writes the result into [output]
     * instead of the output of the current rendering. Nested pieces are written into [output] as well.
     *
     * This allows a renderer to capture the rendered form of a piece, e.g. to post-process it or to render it as a single fragment.
     */
    context(session: KaSession)
    public fun <T> render(value: T, piece: KaPiece<T>, output: KaRenderingOutput)

    /**
     * Whether the current renderer has been requested by [piece] (possibly transitively).
     *
     * The piece which the current renderer renders is not a requester of itself. Passing that piece therefore detects nesting, such as a
     * [KaPiece.Type] which is rendered as a type argument of another [KaPiece.Type].
     */
    public fun <T> isInside(piece: KaPiece<T>): Boolean

    /** Returns the effective value of [option] for this renderer (the overridden value, or its [KaRenderingOption.defaultValue]). */
    public fun <T> valueFor(option: KaRenderingOption<T>): T
}

/** Renders [value] as the given [piece], dispatching to the corresponding renderer stack. */
@KaExperimentalApi
context(session: KaSession, context: KaRenderingContext)
public fun <T> render(value: T, piece: KaPiece<T>) {
    context.render(value, piece)
}

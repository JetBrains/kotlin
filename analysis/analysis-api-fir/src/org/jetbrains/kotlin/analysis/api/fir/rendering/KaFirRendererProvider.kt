/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.rendering.KaParametrizedPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType

@Suppress("UNCHECKED_CAST")
internal class KaRendererImpl(val renderers: KaPieceRendererMap, val options: KaRenderingOptionMap) : KaRenderer {
    context(session: KaSession)
    override fun render(symbol: KaSymbol, output: KaRenderingOutput) {
        render(symbol, data = Unit, KaPiece.Symbol, RenderingContext(output), output)
    }

    context(session: KaSession)
    override fun render(type: KaType, output: KaRenderingOutput) {
        render(type, KaPiece.Type, RenderingContext(output), output)
    }

    context(session: KaSession)
    override fun render(constantValue: KaConstantValue, output: KaRenderingOutput) {
        render(constantValue, KaPiece.ConstantValue, RenderingContext(output), output)
    }

    /** The state of a single top-level rendering call. A nested piece is rendered with the same context. */
    private inner class RenderingContext(private val output: KaRenderingOutput) : KaRenderingContext {
        /**
         * The chain of pieces which are currently being rendered, outermost first.
         *
         * The last element is the piece which is being rendered right now, so the elements before it are exactly the pieces which requested
         * it, and the list is empty while no piece is being rendered.
         */
        val pieces: MutableList<KaPiece<*>> = ArrayList()

        context(session: KaSession)
        override fun <T> render(value: T, piece: KaPiece<T>) {
            render(value, piece, this, output)
        }

        context(session: KaSession)
        override fun <T, D> render(value: T, data: D, piece: KaPiece.Parametrized<T, D>) {
            render(value, data, piece, this, output)
        }

        override fun <T> isInside(piece: KaPiece<T>): Boolean {
            return piece in pieces
        }

        override fun <T> valueFor(option: KaRenderingOption<T>): T {
            return (options[option] as T?) ?: option.defaultValue
        }
    }

    context(session: KaSession)
    private fun <T> render(value: T, piece: KaPiece<T>, context: RenderingContext, output: KaRenderingOutput) {
        val rendererTower = renderers.getValue(piece) as List<KaPieceRenderer<T>>

        try {
            context.pieces.add(piece)
            context(output, context) {
                render(value, isParametrized = false, Unit, rendererTower, rendererTower.lastIndex)
            }
        } finally {
            context.pieces.removeAt(context.pieces.lastIndex)
        }
    }

    context(session: KaSession)
    private fun <T, D> render(value: T, data: D, piece: KaPiece<T>, context: RenderingContext, output: KaRenderingOutput) {
        val rendererTower = renderers.getValue(piece) as List<KaPieceRenderer<T>>

        try {
            context.pieces.add(piece)
            context(output, context) {
                render(value, isParametrized = true, data, rendererTower, rendererTower.lastIndex)
            }
        } finally {
            context.pieces.removeAt(context.pieces.lastIndex)
        }
    }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun <T, D> render(value: T, isParametrized: Boolean, data: D, rendererTower: List<KaPieceRenderer<T>>, topIndex: Int) {
        var index = topIndex

        val next = object : () -> Unit {
            var nextIndex = 0

            override fun invoke() {
                if (nextIndex < 0) {
                    error("The built-in renderer must not delegate")
                }
                render(value, isParametrized, data, rendererTower, nextIndex)
            }
        }

        while (index >= 0) {
            next.nextIndex = index - 1
            val renderer = rendererTower[index]

            val result = if (isParametrized && renderer is KaParametrizedPieceRenderer<T, *>) {
                (renderer as KaParametrizedPieceRenderer<T, D>).render(value, data, next)
            } else {
                renderer.render(value, next)
            }

            if (result) {
                return // Value is handled
            }
            index -= 1
        }
    }

    override fun copy(block: KaRendererBuilder.() -> Unit) = buildRenderer(this, block)
}

internal typealias KaPieceRendererMap = Map<KaPiece<*>, List<KaPieceRenderer<*>>>
internal typealias KaRenderingOptionMap = Map<KaRenderingOption<*>, *>

internal fun buildRenderer(origin: KaRendererImpl?, block: KaRendererBuilder.() -> Unit): KaRendererImpl {
    val renderers: MutableMap<KaPiece<*>, List<KaPieceRenderer<*>>> = origin?.renderers?.toMutableMap() ?: HashMap()
    val options: MutableMap<KaRenderingOption<*>, Any?> = origin?.options?.toMutableMap() ?: HashMap()

    val builder = object : KaRendererBuilder {
        override fun <T> push(renderer: KaPieceRenderer<T>) {
            renderers.compute(renderer.piece) { _, oldValue ->
                if (oldValue == null) {
                    listOf(renderer)
                } else {
                    oldValue + renderer
                }
            }
        }

        override fun <T> get(option: KaRenderingOption<T>): T {
            @Suppress("UNCHECKED_CAST")
            return (options[option] as T?) ?: option.defaultValue
        }

        override fun <T> set(option: KaRenderingOption<T>, value: T) {
            options[option] = value
        }

        override fun <T> unset(option: KaRenderingOption<T>) {
            options.remove(option)
        }
    }

    builder.block()

    return KaRendererImpl(renderers, options)
}

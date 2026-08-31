/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import com.intellij.openapi.components.service
import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.internals.KaRendererProvider

/**
 * A [KaRenderer] which resolves the engine-provided default renderer on the first rendering call.
 *
 * The indirection keeps [KaRenderer.default] and [copy] free of any service lookup, so a customized renderer can be built and cached in a
 * class initializer, such as a companion object property.
 *
 * [customizations] are the [copy] blocks recorded so far. They are replayed, in order, against the engine-provided renderer.
 */
@KaExperimentalApi
@OptIn(KaImplementationDetail::class)
internal class KaDeferredRenderer(private val customizations: List<KaRendererBuilder.() -> Unit>) : KaRenderer {
    @OptIn(KaImplementationDetail::class)
    private val delegate: KaRenderer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        /**
         * Although it's a bad practice, deferred renderer initializers may contain side effects.
         * The [LazyThreadSafetyMode.SYNCHRONIZED] lazy value makes [customizations] run once.
         */
        customizations.fold(service<KaRendererProvider>().defaultRenderer) { renderer, customization ->
            renderer.copy(customization)
        }
    }

    context(session: KaSession)
    override fun <T> render(value: T, piece: KaPiece<T>, output: KaRenderingOutput) {
        delegate.render(value, piece, output)
    }

    override fun copy(block: KaRendererBuilder.() -> Unit): KaRenderer {
        return KaDeferredRenderer(customizations + block)
    }
}

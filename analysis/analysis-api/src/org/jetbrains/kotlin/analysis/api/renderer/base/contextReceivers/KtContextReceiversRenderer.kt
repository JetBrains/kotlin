/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KaContextReceiverLabelRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KaContextReceiverListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KaContextReceiversRenderer(
    public val contextReceiverListRenderer: KaContextReceiverListRenderer,
    public val contextReceiverLabelRenderer: KaContextReceiverLabelRenderer,
) {
    public fun renderContextReceivers(
        analysisSession: KaSession,
        owner: KaContextReceiversOwner,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    ) {
        contextReceiverListRenderer.renderContextReceivers(analysisSession, owner, this, typeRenderer, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KaContextReceiversRenderer {
        val renderer = this
        return KaContextReceiversRenderer {
            this.contextReceiverListRenderer = renderer.contextReceiverListRenderer
            this.contextReceiverLabelRenderer = renderer.contextReceiverLabelRenderer

            action()
        }
    }

    public class Builder {
        public lateinit var contextReceiverListRenderer: KaContextReceiverListRenderer
        public lateinit var contextReceiverLabelRenderer: KaContextReceiverLabelRenderer

        public fun build(): KaContextReceiversRenderer = KaContextReceiversRenderer(
            contextReceiverListRenderer,
            contextReceiverLabelRenderer,
        )
    }

    public companion object {
        public inline operator fun invoke(action: Builder.() -> Unit): KaContextReceiversRenderer =
            Builder().apply(action).build()
    }
}

public typealias KtContextReceiversRenderer = KaContextReceiversRenderer
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KtContextReceiverLabelRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KtContextReceiverListRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public class KtContextReceiversRenderer(
    public val contextReceiverListRenderer: KtContextReceiverListRenderer,
    public val contextReceiverLabelRenderer: KtContextReceiverLabelRenderer,
) {
    public fun renderContextReceivers(
        analysisSession: KtAnalysisSession,
        owner: KtContextReceiversOwner,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    ) {
        contextReceiverListRenderer.renderContextReceivers(analysisSession, owner, this, typeRenderer, printer)
    }

    public inline fun with(action: Builder.() -> Unit): KtContextReceiversRenderer {
        val renderer = this
        return KtContextReceiversRenderer {
            this.contextReceiverListRenderer = renderer.contextReceiverListRenderer
            this.contextReceiverLabelRenderer = renderer.contextReceiverLabelRenderer

            action()
        }
    }

    public class Builder {
        public lateinit var contextReceiverListRenderer: KtContextReceiverListRenderer
        public lateinit var contextReceiverLabelRenderer: KtContextReceiverLabelRenderer

        public fun build(): KtContextReceiversRenderer = KtContextReceiversRenderer(
            contextReceiverListRenderer,
            contextReceiverLabelRenderer,
        )
    }

    public companion object {
        public inline operator fun invoke(action: Builder.() -> Unit): KtContextReceiversRenderer =
            Builder().apply(action).build()
    }
}
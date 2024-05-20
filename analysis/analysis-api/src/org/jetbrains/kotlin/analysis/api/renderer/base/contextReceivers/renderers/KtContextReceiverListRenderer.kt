/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KaContextReceiversRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KaContextReceiverListRenderer {
    public fun renderContextReceivers(
        analysisSession: KaSession,
        owner: KaContextReceiversOwner,
        contextReceiversRenderer: KaContextReceiversRenderer,
        typeRenderer: KaTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KaContextReceiverListRenderer {
        override fun renderContextReceivers(
            analysisSession: KaSession,
            owner: KaContextReceiversOwner,
            contextReceiversRenderer: KaContextReceiversRenderer,
            typeRenderer: KaTypeRenderer,
            printer: PrettyPrinter,
        ) {
            val contextReceivers = owner.contextReceivers
            if (contextReceivers.isEmpty()) return

            printer {
                append("context(")
                printCollection(contextReceivers) { contextReceiver ->
                    contextReceiversRenderer.contextReceiverLabelRenderer
                        .renderLabel(analysisSession, contextReceiver, contextReceiversRenderer, printer)

                    typeRenderer.renderType(analysisSession, contextReceiver.type, printer)
                }
                append(")")
            }
        }
    }
}

public typealias KtContextReceiverListRenderer = KaContextReceiverListRenderer
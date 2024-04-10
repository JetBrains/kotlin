/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiversOwner
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KtContextReceiversRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter

public interface KtContextReceiverListRenderer {
    public fun renderContextReceivers(
        analysisSession: KtAnalysisSession,
        owner: KtContextReceiversOwner,
        contextReceiversRenderer: KtContextReceiversRenderer,
        typeRenderer: KtTypeRenderer,
        printer: PrettyPrinter,
    )

    public object AS_SOURCE : KtContextReceiverListRenderer {
        override fun renderContextReceivers(
            analysisSession: KtAnalysisSession,
            owner: KtContextReceiversOwner,
            contextReceiversRenderer: KtContextReceiversRenderer,
            typeRenderer: KtTypeRenderer,
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
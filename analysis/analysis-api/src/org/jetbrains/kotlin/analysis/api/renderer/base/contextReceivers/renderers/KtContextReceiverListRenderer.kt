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
    context(KtAnalysisSession, KtContextReceiversRenderer, KtTypeRenderer)
    public fun renderContextReceivers(owner: KtContextReceiversOwner, printer: PrettyPrinter)

    public object AS_SOURCE : KtContextReceiverListRenderer {
        context(KtAnalysisSession, KtContextReceiversRenderer, KtTypeRenderer)
        override fun renderContextReceivers(owner: KtContextReceiversOwner, printer: PrettyPrinter) {
            val contextReceivers = owner.contextReceivers
            if (contextReceivers.isEmpty()) return

            printer {
                append("context(")
                printCollection(contextReceivers) { contextReceiver ->
                    contextReceiverLabelRenderer.renderLabel(contextReceiver, printer)
                    renderType(contextReceiver.type, printer)
                }
                append(")")
            }
        }
    }
}
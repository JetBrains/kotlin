/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.base.KtContextReceiver
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KtContextReceiversRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

public interface KtContextReceiverLabelRenderer {
    context(KtAnalysisSession, KtContextReceiversRenderer)
    public fun renderLabel(contextReceiver: KtContextReceiver, printer: PrettyPrinter)

    public object WITH_LABEL : KtContextReceiverLabelRenderer {
        context(KtAnalysisSession, KtContextReceiversRenderer)
        override fun renderLabel(contextReceiver: KtContextReceiver, printer: PrettyPrinter): Unit = printer {
            val label = contextReceiver.label ?: return@printer
            append(label.render())
            append('@')
        }
    }
}

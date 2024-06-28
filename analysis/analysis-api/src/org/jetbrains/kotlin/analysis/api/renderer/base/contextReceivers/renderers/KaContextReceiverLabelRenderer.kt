/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.KaContextReceiversRenderer
import org.jetbrains.kotlin.analysis.utils.printer.PrettyPrinter
import org.jetbrains.kotlin.renderer.render

@KaExperimentalApi
public interface KaContextReceiverLabelRenderer {
    public fun renderLabel(
        analysisSession: KaSession,
        contextReceiver: KaContextReceiver,
        contextReceiversRenderer: KaContextReceiversRenderer,
        printer: PrettyPrinter,
    )

    public object WITH_LABEL : KaContextReceiverLabelRenderer {
        override fun renderLabel(
            analysisSession: KaSession,
            contextReceiver: KaContextReceiver,
            contextReceiversRenderer: KaContextReceiversRenderer,
            printer: PrettyPrinter,
        ) {
            printer {
                val label = contextReceiver.label ?: return@printer
                append(label.render())
                append('@')
            }
        }
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaContextReceiverLabelRenderer' instead", ReplaceWith("KaContextReceiverLabelRenderer"))
public typealias KtContextReceiverLabelRenderer = KaContextReceiverLabelRenderer
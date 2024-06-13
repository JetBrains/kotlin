/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers

import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KaContextReceiverLabelRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KaContextReceiverListRenderer

public object KaContextReceiversRendererForSource {
    public val WITH_LABELS: KaContextReceiversRenderer = KaContextReceiversRenderer {
        contextReceiverListRenderer = KaContextReceiverListRenderer.AS_SOURCE
        contextReceiverLabelRenderer = KaContextReceiverLabelRenderer.WITH_LABEL
    }
}

public typealias KtContextReceiversRendererForSource = KaContextReceiversRendererForSource
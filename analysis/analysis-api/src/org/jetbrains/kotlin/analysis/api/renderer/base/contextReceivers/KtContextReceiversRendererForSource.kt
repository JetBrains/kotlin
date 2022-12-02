/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers

import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KtContextReceiverLabelRenderer
import org.jetbrains.kotlin.analysis.api.renderer.base.contextReceivers.renderers.KtContextReceiverListRenderer

public object KtContextReceiversRendererForSource {
    public val WITH_LABELS: KtContextReceiversRenderer = KtContextReceiversRenderer {
        contextReceiverListRenderer = KtContextReceiverListRenderer.AS_SOURCE
        contextReceiverLabelRenderer = KtContextReceiverLabelRenderer.WITH_LABEL
    }
}

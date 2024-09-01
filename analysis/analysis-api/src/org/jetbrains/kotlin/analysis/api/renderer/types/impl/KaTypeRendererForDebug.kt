/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.impl

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.renderer.types.KaExpandedTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.renderer.types.KaTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaCapturedTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaFlexibleTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaErrorTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KaUnresolvedClassErrorTypeRenderer

@KaExperimentalApi
public object KaTypeRendererForDebug {
    public val WITH_QUALIFIED_NAMES: KaTypeRenderer = KaTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        expandedTypeRenderingMode = KaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT
        capturedTypeRenderer = KaCapturedTypeRenderer.AS_CAPTURED_TYPE_WITH_PROJECTION
        flexibleTypeRenderer = KaFlexibleTypeRenderer.AS_SHORT
        errorTypeRenderer = KaErrorTypeRenderer.WITH_ERROR_MESSAGE
        unresolvedClassErrorTypeRenderer = KaUnresolvedClassErrorTypeRenderer.WITH_ERROR_MESSAGE
    }

    public val WITH_SHORT_NAMES: KaTypeRenderer = KaTypeRendererForSource.WITH_SHORT_NAMES.with {
        expandedTypeRenderingMode = KaExpandedTypeRenderingMode.RENDER_ABBREVIATED_TYPE_WITH_EXPANDED_TYPE_COMMENT
        capturedTypeRenderer = KaCapturedTypeRenderer.AS_CAPTURED_TYPE_WITH_PROJECTION
        flexibleTypeRenderer = KaFlexibleTypeRenderer.AS_SHORT
        errorTypeRenderer = KaErrorTypeRenderer.WITH_ERROR_MESSAGE
        unresolvedClassErrorTypeRenderer = KaUnresolvedClassErrorTypeRenderer.WITH_ERROR_MESSAGE
    }
}

@KaExperimentalApi
@Deprecated("Use 'KaTypeRendererForDebug' instead", ReplaceWith("KaTypeRendererForDebug"))
public typealias KtTypeRendererForDebug = KaTypeRendererForDebug

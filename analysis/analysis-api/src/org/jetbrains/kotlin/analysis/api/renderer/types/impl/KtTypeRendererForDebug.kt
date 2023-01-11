/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.renderer.types.impl

import org.jetbrains.kotlin.analysis.api.renderer.types.KtTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtCapturedTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtFlexibleTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtTypeErrorTypeRenderer
import org.jetbrains.kotlin.analysis.api.renderer.types.renderers.KtUnresolvedClassErrorTypeRenderer

public object KtTypeRendererForDebug {
    public val WITH_QUALIFIED_NAMES: KtTypeRenderer = KtTypeRendererForSource.WITH_QUALIFIED_NAMES.with {
        capturedTypeRenderer = KtCapturedTypeRenderer.AS_CAPTURED_TYPE_WITH_PROJECTION
        flexibleTypeRenderer = KtFlexibleTypeRenderer.AS_SHORT
        typeErrorTypeRenderer = KtTypeErrorTypeRenderer.WITH_ERROR_MESSAGE
        unresolvedClassErrorTypeRenderer = KtUnresolvedClassErrorTypeRenderer.WITH_ERROR_MESSAGE
    }
}
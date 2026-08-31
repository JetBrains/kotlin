/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer

/** The default renderer, with the built-in piece renderer stacks assembled from the neighboring group files. */
internal val DEFAULT_RENDERER: KaRenderer = buildRenderer(null) {
    pushSymbolRenderers()
    pushAnnotationRenderers()
    pushNameRenderers()
    pushCallableRenderers()
    pushParameterRenderers()
    pushClassifierRenderers()
    pushTypeRenderers()
}

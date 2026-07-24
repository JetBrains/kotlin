/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.internals.KaRendererProvider
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer

internal class KaRendererProviderImpl : KaRendererProvider {
    override val defaultRenderer: KaRenderer
        get() = DEFAULT_RENDERER
}

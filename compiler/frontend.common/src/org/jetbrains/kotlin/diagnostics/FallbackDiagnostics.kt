/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory

object FallbackDiagnostics : KtDiagnosticsContainer() {
    val FALLBACK_ERROR: KtSourcelessDiagnosticFactory = KtSourcelessDiagnosticFactory(
        "FALLBACK_ERROR",
        ERROR,
        getRendererFactory(),
    )
    val FALLBACK_WARNING: KtSourcelessDiagnosticFactory = KtSourcelessDiagnosticFactory(
        "FALLBACK_WARNING",
        WARNING,
        getRendererFactory(),
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FallbackDiagnostics") { map ->
            map.put(
                FALLBACK_ERROR,
                "{0}\n\nThis error was reported on an element without source. This is a bug, please report an issue: https://kotl.in/issue"
            )
            map.put(
                FALLBACK_WARNING,
                "{0}\n\nThis warning was reported on an element without source. This is a bug, please report an issue: https://kotl.in/issue"
            )
        }
    }
}

internal fun AbstractKtDiagnosticFactory.createFallbackDiagnostic(original: KtDiagnostic): KtDiagnosticWithoutSource? {
    val factory = if (severity.isError) FallbackDiagnostics.FALLBACK_ERROR else FallbackDiagnostics.FALLBACK_WARNING
    return factory.create(original.renderMessage(), null, original.context)
}

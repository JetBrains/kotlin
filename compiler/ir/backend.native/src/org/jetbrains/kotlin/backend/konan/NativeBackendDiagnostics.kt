/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory

object NativeBackendDiagnostics : KtDiagnosticsContainer() {
    override fun getRendererFactory(): BaseDiagnosticRendererFactory = Messages

    object Messages : BaseSourcelessDiagnosticRendererFactory() {
        override val MAP by KtDiagnosticFactoryToRendererMap("NativeBackendDiagnostics") {
        }
    }
}

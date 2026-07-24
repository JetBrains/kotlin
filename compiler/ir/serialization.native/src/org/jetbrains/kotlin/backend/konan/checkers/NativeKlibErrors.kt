/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.Renderer
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.render

object NativeKlibErrors : KtDiagnosticsContainer() {
    val LEAKED_VOLATILE_FIELD by error1<PsiElement, IrCall>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = KtDefaultNativeKlibErrorMessages
}

private object KtDefaultNativeKlibErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("KT") { map ->
        map.put(
            NativeKlibErrors.LEAKED_VOLATILE_FIELD,
            "Given call leaks private volatile field into scope where it is not accessible {0}",
            Renderer { it.render() }
        )
    }
}

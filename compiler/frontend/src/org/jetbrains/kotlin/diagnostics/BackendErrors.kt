/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory

object BackendErrors : KtDiagnosticsContainer() {

    val NON_LOCAL_RETURN_IN_DISABLED_INLINE by error0<PsiElement>(SourceElementPositioningStrategies.DEFAULT) // need to reference SourceElementPositioningStrategies at least once to initialize properly


    override fun getRendererFactory(): BaseDiagnosticRendererFactory {
        return BackendErrorMessages
    }
}

object BackendErrorMessages : BaseDiagnosticRendererFactory() {
    override val MAP by KtDiagnosticFactoryToRendererMap("BackendErrors") { map ->
        map.put(BackendErrors.NON_LOCAL_RETURN_IN_DISABLED_INLINE, "Non-local returns are not allowed with inlining disabled")
    }
}

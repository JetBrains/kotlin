/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.rendering.KtDefaultErrorMessages
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object BackendErrors {

    val NON_LOCAL_RETURN_IN_DISABLED_INLINE by error0<PsiElement>(SourceElementPositioningStrategies.DEFAULT) // need to reference SourceElementPositioningStrategies at least once to initialize properly

    init {
        RootDiagnosticRendererFactory.registerFactory(KtDefaultErrorMessages)
    }
}
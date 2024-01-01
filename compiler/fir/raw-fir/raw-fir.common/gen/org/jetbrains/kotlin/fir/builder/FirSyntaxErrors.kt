/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

/**
 * Generated from: [org.jetbrains.kotlin.fir.builder.SYNTAX_DIAGNOSTIC_LIST]
 */
object FirSyntaxErrors {
    // Syntax
    val SYNTAX: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirSyntaxErrorsDefaultMessages)
    }
}

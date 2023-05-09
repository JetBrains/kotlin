/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirSyntaxErrors {
    // Syntax
    val SYNTAX by error1<PsiElement, String>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirSyntaxErrorsDefaultMessages)
    }
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object FirSyntaxErrors {

    val SYNTAX by error0<PsiElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirSyntaxErrorsDefaultMessages)
    }
}

@Suppress("unused")
object FirSyntaxErrorsDefaultMessages : BaseDiagnosticRendererFactory() {

    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(FirSyntaxErrors.SYNTAX, "Syntax error")
    }
}

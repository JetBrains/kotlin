/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.psi.KtExpression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

object FirJsErrors {
    // Annotations
    val WRONG_JS_QUALIFIER by error0<KtExpression>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirJsErrorsDefaultMessages)
    }
}

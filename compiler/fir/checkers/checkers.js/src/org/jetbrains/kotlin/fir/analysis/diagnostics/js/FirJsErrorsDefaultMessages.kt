/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.checkMissingMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_JS_MODULE_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_JS_QUALIFIER
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.WRONG_MULTIPLE_INHERITANCE

@Suppress("unused")
object FirJsErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP = KtDiagnosticFactoryToRendererMap("FIR").also { map ->
        map.put(WRONG_JS_QUALIFIER, "Qualifier contains illegal characters")
        map.put(JS_MODULE_PROHIBITED_ON_VAR, "@JsModule and @JsNonModule annotations prohibited for 'var' declarations. Use 'val' instead.")
        map.put(JS_MODULE_PROHIBITED_ON_NON_NATIVE, "@JsModule and @JsNonModule annotations prohibited for non-external declarations.")
        map.put(
            NESTED_JS_MODULE_PROHIBITED,
            "@JsModule and @JsNonModule can't appear on here since the file is already marked by either @JsModule or @JsNonModule"
        )
        map.put(
            WRONG_MULTIPLE_INHERITANCE,
            "Can''t apply multiple inheritance here, since it''s impossible to generate bridge for system function {0}",
            FirDiagnosticRenderers.SYMBOL
        )

        map.checkMissingMessages(FirJsErrors)
    }
}
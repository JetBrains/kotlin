/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.js

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.checkMissingMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.DELEGATION_BY_DYNAMIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.DYNAMIC
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.IMPLEMENTING_FUNCTION_INTERFACE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_NON_NATIVE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.JS_MODULE_PROHIBITED_ON_VAR
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.NESTED_JS_MODULE_PROHIBITED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_NOT_SUPPORTED
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors.RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION
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
        map.put(DYNAMIC, "A dynamic type appears here")
        map.put(DELEGATION_BY_DYNAMIC, "Can't delegate to dynamic value")
        map.put(IMPLEMENTING_FUNCTION_INTERFACE, "Implementing function interface is prohibited in JavaScript")
        map.put(OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS, "Overriding `external` function with optional parameters")
        map.put(
            OVERRIDING_EXTERNAL_FUN_WITH_OPTIONAL_PARAMS_WITH_FAKE,
            "Overriding `external` function with optional parameters by declaration from superclass: {0}",
            FirDiagnosticRenderers.SYMBOL
        )
        map.put(CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION, "This property can only be used from external declarations")
        map.put(RUNTIME_ANNOTATION_ON_EXTERNAL_DECLARATION, "Runtime annotation can't be put on external declaration")
        map.put(
            RUNTIME_ANNOTATION_NOT_SUPPORTED,
            "Reflection is not supported in JavaScript target, therefore you won't be able to read this annotation in run-time"
        )

        map.checkMissingMessages(FirJsErrors)
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.wasm

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticRenderers.TO_STRING
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnosticRenderers
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.ASSOCIATED_OBJECT_INVALID_BINDING
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.EXPORT_DECLARATION_WITH_CONTEXT_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.JSCODE_INVALID_PARAMETER_NAME
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.JSCODE_UNSUPPORTED_FUNCTION_KIND
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.JSCODE_WRONG_CONTEXT
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.JS_MODULE_PROHIBITED_ON_NON_EXTERNAL
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.NESTED_WASM_EXPORT
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.NESTED_WASM_IMPORT
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASM_EXPORT_ON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASM_IMPORT_EXPORT_VARARG_PARAMETER
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WRONG_JS_FUN_TARGET
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors.WRONG_JS_INTEROP_TYPE

@Suppress("unused")
object FirWasmErrorsDefaultMessages : BaseDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("FIR") { map ->
        map.put(JS_MODULE_PROHIBITED_ON_NON_EXTERNAL, "'@JsModule' annotation is prohibited for non-external declarations.")
        map.put(
            NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN,
            "Annotation ''{0}'' is only allowed on member functions of declarations annotated with ''kotlin.js.native'' functions.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(
            NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE,
            "Non-external type extends external type ''{0}''.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(
            WRONG_JS_INTEROP_TYPE,
            "Type ''{0}'' cannot be used as {1}. Only external, primitive, string, and function types are supported in Kotlin/Wasm JS interop.",
            FirDiagnosticRenderers.RENDER_TYPE, TO_STRING
        )

        map.put(WRONG_JS_FUN_TARGET, "Only top-level external functions can be implemented using '@JsFun'.")

        map.put(
            JSCODE_WRONG_CONTEXT,
            "Calls to 'js(code)' must be a single expression inside a top-level function body or a property initializer in Kotlin/Wasm."
        )
        map.put(
            JSCODE_UNSUPPORTED_FUNCTION_KIND,
            "Calls to ''js(code)'' are not supported in {0} in Kotlin/Wasm.",
            TO_STRING
        )
        map.put(
            JSCODE_INVALID_PARAMETER_NAME,
            "Parameters passed to 'js(code)' must have a valid JavaScript name."
        )

        map.put(NESTED_WASM_EXPORT, "Only top-level functions can be exported with '@WasmExport'.")
        map.put(WASM_EXPORT_ON_EXTERNAL_DECLARATION, "Functions annotated with '@WasmExport' cannot be external.")
        map.put(JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION, "Cannot use '@WasmExport' and '@JsExport' for same function.")
        map.put(NESTED_WASM_IMPORT, "Only top-level functions can be imported with '@WasmImport'.")
        map.put(WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION, "Functions annotated with '@WasmImport' must be external.")
        map.put(
            WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE,
            "Default parameter values are not supported with '@WasmImport' and '@WasmExport'."
        )
        map.put(WASM_IMPORT_EXPORT_VARARG_PARAMETER, "Vararg parameters are not supported with '@WasmImport' and '@WasmExport'.")
        map.put(
            WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE,
            "Unsupported ''@WasmImport'' and ''@WasmExport'' parameter type ''{0}''.",
            FirDiagnosticRenderers.RENDER_TYPE
        )
        map.put(
            WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE,
            "Unsupported ''@WasmImport'' and ''@WasmExport'' return type ''{0}''.",
            FirDiagnosticRenderers.RENDER_TYPE
        )

        map.put(WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION, "Only top-level functions can be external.")
        map.put(WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT, "External functions must be annotated with '@WasmImport'.")

        map.put(ASSOCIATED_OBJECT_INVALID_BINDING, "Invalid associated object binding.")

        map.put(EXTERNAL_DECLARATION_WITH_CONTEXT_PARAMETERS, "External declaration cannot have context parameters.")
        map.put(EXPORT_DECLARATION_WITH_CONTEXT_PARAMETERS, "Exported declaration cannot have context parameters.")
    }
}

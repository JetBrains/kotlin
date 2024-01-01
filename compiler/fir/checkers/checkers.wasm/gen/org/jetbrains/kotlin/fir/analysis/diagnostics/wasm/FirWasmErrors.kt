/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.wasm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory2
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WASM_DIAGNOSTICS_LIST]
 */
object FirWasmErrors {
    // Annotations
    val JS_MODULE_PROHIBITED_ON_VAR: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val JS_MODULE_PROHIBITED_ON_NON_EXTERNAL: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NESTED_JS_MODULE_PROHIBITED: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // Externals
    val NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 by error0<PsiElement>()
    val WRONG_JS_INTEROP_TYPE: KtDiagnosticFactory2<String, ConeKotlinType> by error2<KtElement, String, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // JsFun
    val WRONG_JS_FUN_TARGET: KtDiagnosticFactory0 by error0<PsiElement>()

    // JsCode
    val JSCODE_WRONG_CONTEXT: KtDiagnosticFactory0 by error0<KtElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val JSCODE_UNSUPPORTED_FUNCTION_KIND: KtDiagnosticFactory1<String> by error1<KtElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val JSCODE_INVALID_PARAMETER_NAME: KtDiagnosticFactory0 by error0<KtElement>()

    // Wasm interop
    val NESTED_WASM_EXPORT: KtDiagnosticFactory0 by error0<KtElement>()
    val WASM_EXPORT_ON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 by error0<KtElement>()
    val JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION: KtDiagnosticFactory0 by error0<KtElement>()
    val NESTED_WASM_IMPORT: KtDiagnosticFactory0 by error0<KtElement>()
    val WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 by error0<KtElement>()
    val WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE: KtDiagnosticFactory0 by error0<KtElement>()
    val WASM_IMPORT_EXPORT_VARARG_PARAMETER: KtDiagnosticFactory0 by error0<KtElement>()
    val WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE: KtDiagnosticFactory1<ConeKotlinType> by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // WASI
    val WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION: KtDiagnosticFactory0 by error0<KtElement>()
    val WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT: KtDiagnosticFactory0 by error0<KtElement>()

    init {
        RootDiagnosticRendererFactory.registerFactory(FirWasmErrorsDefaultMessages)
    }
}

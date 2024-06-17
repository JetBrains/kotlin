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
import org.jetbrains.kotlin.diagnostics.Severity.ERROR
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
    val JS_MODULE_PROHIBITED_ON_VAR: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_MODULE_PROHIBITED_ON_VAR", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val JS_MODULE_PROHIBITED_ON_NON_EXTERNAL: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_MODULE_PROHIBITED_ON_NON_EXTERNAL", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val NESTED_JS_MODULE_PROHIBITED: KtDiagnosticFactory0 = KtDiagnosticFactory0("NESTED_JS_MODULE_PROHIBITED", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)

    // Externals
    val NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)
    val WRONG_JS_INTEROP_TYPE: KtDiagnosticFactory2<ConeKotlinType, String> = KtDiagnosticFactory2("WRONG_JS_INTEROP_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("NON_EXTERNAL_DECLARATION_IN_INAPPROPRIATE_FILE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)

    // JsFun
    val WRONG_JS_FUN_TARGET: KtDiagnosticFactory0 = KtDiagnosticFactory0("WRONG_JS_FUN_TARGET", ERROR, SourceElementPositioningStrategies.DEFAULT, PsiElement::class)

    // JsCode
    val JSCODE_WRONG_CONTEXT: KtDiagnosticFactory0 = KtDiagnosticFactory0("JSCODE_WRONG_CONTEXT", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, KtElement::class)
    val JSCODE_UNSUPPORTED_FUNCTION_KIND: KtDiagnosticFactory1<String> = KtDiagnosticFactory1("JSCODE_UNSUPPORTED_FUNCTION_KIND", ERROR, SourceElementPositioningStrategies.NAME_IDENTIFIER, KtElement::class)
    val JSCODE_INVALID_PARAMETER_NAME: KtDiagnosticFactory0 = KtDiagnosticFactory0("JSCODE_INVALID_PARAMETER_NAME", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    // Wasm interop
    val NESTED_WASM_EXPORT: KtDiagnosticFactory0 = KtDiagnosticFactory0("NESTED_WASM_EXPORT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WASM_EXPORT_ON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("WASM_EXPORT_ON_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val NESTED_WASM_IMPORT: KtDiagnosticFactory0 = KtDiagnosticFactory0("NESTED_WASM_IMPORT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION: KtDiagnosticFactory0 = KtDiagnosticFactory0("WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE: KtDiagnosticFactory0 = KtDiagnosticFactory0("WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WASM_IMPORT_EXPORT_VARARG_PARAMETER: KtDiagnosticFactory0 = KtDiagnosticFactory0("WASM_IMPORT_EXPORT_VARARG_PARAMETER", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)
    val WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE: KtDiagnosticFactory1<ConeKotlinType> = KtDiagnosticFactory1("WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE", ERROR, SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT, KtElement::class)

    // WASI
    val WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION: KtDiagnosticFactory0 = KtDiagnosticFactory0("WASI_EXTERNAL_NOT_TOP_LEVEL_FUNCTION", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)
    val WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT: KtDiagnosticFactory0 = KtDiagnosticFactory0("WASI_EXTERNAL_FUNCTION_WITHOUT_IMPORT", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    // Associated object
    val ASSOCIATED_OBJECT_INVALID_BINDING: KtDiagnosticFactory0 = KtDiagnosticFactory0("ASSOCIATED_OBJECT_INVALID_BINDING", ERROR, SourceElementPositioningStrategies.DEFAULT, KtElement::class)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirWasmErrorsDefaultMessages)
    }
}

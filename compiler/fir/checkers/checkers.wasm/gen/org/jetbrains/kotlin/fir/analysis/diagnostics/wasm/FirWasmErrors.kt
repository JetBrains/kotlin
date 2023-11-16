/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics.wasm

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory
import org.jetbrains.kotlin.fir.analysis.diagnostics.*
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement

/**
 * Generated from: [org.jetbrains.kotlin.fir.checkers.generator.diagnostics.WASM_DIAGNOSTICS_LIST]
 */
object FirWasmErrors {
    // Externals
    val NON_EXTERNAL_TYPE_EXTENDS_EXTERNAL_TYPE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION by error0<PsiElement>()
    val WRONG_JS_INTEROP_TYPE by error2<KtElement, String, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    // JsFun
    val WRONG_JS_FUN_TARGET by error0<PsiElement>()

    // JsCode
    val JSCODE_WRONG_CONTEXT by error0<KtElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val JSCODE_UNSUPPORTED_FUNCTION_KIND by error1<KtElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val JSCODE_INVALID_PARAMETER_NAME by error0<KtElement>()

    // Wasm interop
    val NESTED_WASM_EXPORT by error0<KtElement>()
    val WASM_EXPORT_ON_EXTERNAL_DECLARATION by error0<KtElement>()
    val JS_AND_WASM_EXPORTS_ON_SAME_DECLARATION by error0<KtElement>()
    val NESTED_WASM_IMPORT by error0<KtElement>()
    val WASM_IMPORT_ON_NON_EXTERNAL_DECLARATION by error0<KtElement>()
    val WASM_IMPORT_EXPORT_PARAMETER_DEFAULT_VALUE by error0<KtElement>()
    val WASM_IMPORT_EXPORT_VARARG_PARAMETER by error0<KtElement>()
    val WASM_IMPORT_EXPORT_UNSUPPORTED_PARAMETER_TYPE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)
    val WASM_IMPORT_EXPORT_UNSUPPORTED_RETURN_TYPE by error1<KtElement, ConeKotlinType>(SourceElementPositioningStrategies.DECLARATION_SIGNATURE_OR_DEFAULT)

    init {
        RootDiagnosticRendererFactory.registerFactory(FirWasmErrorsDefaultMessages)
    }
}

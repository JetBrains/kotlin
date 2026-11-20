/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.wasm.checkers

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInstanceExtension
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.js.common.RESERVED_KEYWORDS
import org.jetbrains.kotlin.wasm.WasmSpecialExportNames

fun FirNamedFunction.unsupportedExportedFunctionKind(session: FirSession): String? = when {
    symbol.isEffectivelyExternal(session) -> "external function"
    isInline && typeParameters.any { it.symbol.isReified } -> "inline function with reified type parameters"
    isSuspend -> "suspend function"
    isInstanceExtension -> "extension function"
    else -> null
}

// In Wasm/JS, WasmExport and JsExport declarations are exported via `export const { name } = exports`.
// Reserved words as export names make JS module invalid.
// Names used by Kotlin/Wasm compiler internally are dropped from the JS module.
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkNameExportedToJs(name: String, source: KtSourceElement?) {
    when {
        // Unlike K/JS, it's impossible to export "default" with the pure @JsName("default").
        name in RESERVED_KEYWORDS -> reporter.reportOn(source, FirWasmErrors.RESERVED_WORD_AS_JS_EXPORT_NAME, name)
        WasmSpecialExportNames.isInternal(name) -> reporter.reportOn(source, FirWasmErrors.INTERNAL_WASM_EXPORT_NAME_AS_JS_EXPORT_NAME, name)
    }
}

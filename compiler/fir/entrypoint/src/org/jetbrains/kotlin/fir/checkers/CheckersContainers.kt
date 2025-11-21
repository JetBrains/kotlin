/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.js.FirJsErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.jvm.FirJvmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.native.FirNativeErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.wasm.FirWasmErrors
import org.jetbrains.kotlin.fir.analysis.diagnostics.web.common.FirWebCommonErrors
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmTypeCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeDeclarationExtraCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeTypeCheckers
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.*
import org.jetbrains.kotlin.fir.builder.FirSyntaxErrors
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.platform.wasm.WasmTarget

fun FirSessionConfigurator.registerCommonCheckers() {
    useCheckers(CommonDeclarationCheckers)
    useCheckers(CommonExpressionCheckers)
    useCheckers(CommonTypeCheckers)
    useCheckers(CommonLanguageVersionSettingsCheckers)
    registerDiagnosticContainers(FirErrors, FirSyntaxErrors)
}

fun FirSessionConfigurator.registerExtraCommonCheckers() {
    useCheckers(ExtraExpressionCheckers)
    useCheckers(ExtraDeclarationCheckers)
    useCheckers(ExtraLanguageVersionSettingsCheckers)
    registerDiagnosticContainers(FirErrors)
}

fun FirSessionConfigurator.registerExperimentalCheckers() {
    useCheckers(ExperimentalExpressionCheckers)
    useCheckers(ExperimentalTypeCheckers)
    useCheckers(ExperimentalLanguageVersionSettingsCheckers)
    registerDiagnosticContainers(FirErrors)
}

fun FirSessionConfigurator.registerJvmCheckers() {
    useCheckers(JvmDeclarationCheckers)
    useCheckers(JvmExpressionCheckers)
    useCheckers(JvmTypeCheckers)
    registerDiagnosticContainers(FirJvmErrors)
}

fun FirSessionConfigurator.registerJsCheckers() {
    useCheckers(JsDeclarationCheckers)
    useCheckers(JsExpressionCheckers)
    registerDiagnosticContainers(FirWebCommonErrors, FirJsErrors)
}

fun FirSessionConfigurator.registerNativeCheckers() {
    useCheckers(NativeDeclarationCheckers)
    useCheckers(NativeExpressionCheckers)
    useCheckers(NativeTypeCheckers)
    registerDiagnosticContainers(FirNativeErrors)
}

fun FirSessionConfigurator.registerExtraNativeCheckers() {
    useCheckers(NativeDeclarationExtraCheckers)
    registerDiagnosticContainers(FirNativeErrors)
}

fun FirSessionConfigurator.registerWasmCheckers(target: WasmTarget) {
    useCheckers(WasmBaseDeclarationCheckers)
    useCheckers(WasmBaseExpressionCheckers)
    useCheckers(WasmBaseTypeCheckers)
    registerDiagnosticContainers(FirWebCommonErrors, FirWasmErrors)

    when (target) {
        WasmTarget.JS -> {
            useCheckers(WasmJsDeclarationCheckers)
            useCheckers(WasmJsExpressionCheckers)
        }
        WasmTarget.WASI -> {
            useCheckers(WasmWasiDeclarationCheckers)
        }
        WasmTarget.SPEC -> {
            useCheckers(WasmSpecDeclarationCheckers)
        }
    }
}

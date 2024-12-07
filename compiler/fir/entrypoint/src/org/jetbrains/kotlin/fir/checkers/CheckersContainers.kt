/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.js.checkers.JsExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.jvm.checkers.JvmTypeCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeDeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.native.checkers.NativeTypeCheckers
import org.jetbrains.kotlin.fir.analysis.wasm.checkers.*
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.platform.wasm.WasmTarget

fun FirSessionConfigurator.registerCommonCheckers() {
    useCheckers(CommonDeclarationCheckers)
    useCheckers(CommonExpressionCheckers)
    useCheckers(CommonTypeCheckers)
    useCheckers(CommonLanguageVersionSettingsCheckers)
}

fun FirSessionConfigurator.registerExtraCommonCheckers() {
    useCheckers(ExtraExpressionCheckers)
    useCheckers(ExtraDeclarationCheckers)
    useCheckers(ExtraTypeCheckers)
    useCheckers(ExtraLanguageVersionSettingsCheckers)
}

fun FirSessionConfigurator.registerExperimentalCheckers() {
    useCheckers(ExperimentalExpressionCheckers)
    useCheckers(ExperimentalDeclarationCheckers)
    useCheckers(ExperimentalTypeCheckers)
    useCheckers(ExperimentalLanguageVersionSettingsCheckers)
}

fun FirSessionConfigurator.registerJvmCheckers() {
    useCheckers(JvmDeclarationCheckers)
    useCheckers(JvmExpressionCheckers)
    useCheckers(JvmTypeCheckers)
}

fun FirSessionConfigurator.registerJsCheckers() {
    useCheckers(JsDeclarationCheckers)
    useCheckers(JsExpressionCheckers)
}

fun FirSessionConfigurator.registerNativeCheckers() {
    useCheckers(NativeDeclarationCheckers)
    useCheckers(NativeExpressionCheckers)
    useCheckers(NativeTypeCheckers)
}

fun FirSessionConfigurator.registerWasmCheckers(target: WasmTarget) {
    useCheckers(WasmBaseDeclarationCheckers)
    useCheckers(WasmBaseExpressionCheckers)
    useCheckers(WasmBaseTypeCheckers)

    when (target) {
        WasmTarget.JS -> {
            useCheckers(WasmJsDeclarationCheckers)
            useCheckers(WasmJsExpressionCheckers)
        }
        WasmTarget.WASI -> {
            useCheckers(WasmWasiDeclarationCheckers)
        }
    }
}
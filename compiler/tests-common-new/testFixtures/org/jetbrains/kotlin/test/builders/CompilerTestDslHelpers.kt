/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.CLASSIC_FRONTEND_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.DESERIALIZED_IR_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.FIR_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.JS_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.JVM_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.KLIB_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.LOWERED_IR_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.NATIVE_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.RAW_IR_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.WASM_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.services.CompilationStage

object CompilerStepsNames {
    const val CLASSIC_FRONTEND_HANDLERS_STEP_NAME = "classic frontend handlers"
    const val FIR_HANDLERS_STEP_NAME = "FIR frontend handlers"

    /** The IR that is the outcome of Psi2Ir or Fir2Ir. */
    const val RAW_IR_HANDLERS_STEP_NAME = "raw IR handlers"

    /** The IR that is the outcome of the "common lowerings prefix" at the "first stage" of compilation. */
    const val LOWERED_IR_HANDLERS_STEP_NAME = "lowered IR handlers"

    /** The IR that is the outcome of KLIB deserializer. */
    const val DESERIALIZED_IR_HANDLERS_STEP_NAME = "deserialized IR handlers"

    const val JVM_ARTIFACTS_HANDLERS_STEP_NAME = "jvm artifacts handlers"
    const val NATIVE_ARTIFACTS_HANDLERS_STEP_NAME = "native artifacts handlers"
    const val JS_ARTIFACTS_HANDLERS_STEP_NAME = "js artifacts handlers"
    const val WASM_ARTIFACTS_HANDLERS_STEP_NAME = "wasm artifacts handlers"
    const val KLIB_ARTIFACTS_HANDLERS_STEP_NAME = "klib artifacts handlers"

}

// --------------------- default compiler steps ---------------------

fun TestConfigurationBuilder.classicFrontendStep() {
    facadeStep(::ClassicFrontendFacade)
}

// --------------------- default handlers steps ---------------------

// use those ones to define new step
inline fun TestConfigurationBuilder.classicFrontendHandlersStep(
    init: HandlersStepBuilder<ClassicFrontendOutputArtifact, FrontendKinds.ClassicFrontend>.() -> Unit = {}
) {
    namedHandlersStep(CLASSIC_FRONTEND_HANDLERS_STEP_NAME, FrontendKinds.ClassicFrontend, CompilationStage.FIRST, init)
}

inline fun TestConfigurationBuilder.firHandlersStep(
    init: HandlersStepBuilder<FirOutputArtifact, FrontendKinds.FIR>.() -> Unit = {}
) {
    namedHandlersStep(FIR_HANDLERS_STEP_NAME, FrontendKinds.FIR, CompilationStage.FIRST, init)
}

inline fun TestConfigurationBuilder.irHandlersStep(
    init: HandlersStepBuilder<IrBackendInput, BackendKinds.IrBackend>.() -> Unit = {}
) {
    namedHandlersStep(RAW_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, CompilationStage.FIRST, init)
}

inline fun TestConfigurationBuilder.loweredIrHandlersStep(
    init: HandlersStepBuilder<IrBackendInput, BackendKinds.IrBackend>.() -> Unit = {}
) {
    namedHandlersStep(LOWERED_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, CompilationStage.FIRST, init)
}

inline fun TestConfigurationBuilder.deserializedIrHandlersStep(
    init: HandlersStepBuilder<IrBackendInput, BackendKinds.IrBackend>.() -> Unit = {}
) {
    namedHandlersStep(DESERIALIZED_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, CompilationStage.SECOND, init)
}

inline fun TestConfigurationBuilder.jvmArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.() -> Unit = {}
) {
    namedHandlersStep(JVM_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Jvm, CompilationStage.FIRST, init)
}

inline fun TestConfigurationBuilder.nativeArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Native, ArtifactKinds.Native>.() -> Unit = {}
) {
    namedHandlersStep(NATIVE_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Native, CompilationStage.SECOND, init)
}

inline fun TestConfigurationBuilder.jsArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Js, ArtifactKinds.Js>.() -> Unit = {}
) {
    namedHandlersStep(JS_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Js, CompilationStage.SECOND, init)
}

inline fun TestConfigurationBuilder.wasmArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Wasm, ArtifactKinds.Wasm>.() -> Unit = {}
) {
    namedHandlersStep(WASM_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Wasm, CompilationStage.SECOND, init)
}

inline fun TestConfigurationBuilder.klibArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.KLib, ArtifactKinds.KLib>.() -> Unit = {}
) {
    namedHandlersStep(KLIB_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.KLib, CompilationStage.FIRST, init)
}

// and those ones to configure already defined step
inline fun TestConfigurationBuilder.configureClassicFrontendHandlersStep(
    skipMissingStep: Boolean = false,
    init: HandlersStepBuilder<ClassicFrontendOutputArtifact, FrontendKinds.ClassicFrontend>.() -> Unit = {}
) {
    configureNamedHandlersStep(CLASSIC_FRONTEND_HANDLERS_STEP_NAME, FrontendKinds.ClassicFrontend, skipMissingStep, init)
}

inline fun TestConfigurationBuilder.configureFirHandlersStep(
    init: HandlersStepBuilder<FirOutputArtifact, FrontendKinds.FIR>.() -> Unit = {}
) {
    configureNamedHandlersStep(FIR_HANDLERS_STEP_NAME, FrontendKinds.FIR, skipMissingStep = false, init)
}

inline fun TestConfigurationBuilder.configureIrHandlersStep(
    init: HandlersStepBuilder<IrBackendInput, BackendKinds.IrBackend>.() -> Unit = {}
) {
    configureNamedHandlersStep(RAW_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, skipMissingStep = false, init)
}

inline fun TestConfigurationBuilder.configureLoweredIrHandlersStep(
    init: HandlersStepBuilder<IrBackendInput, BackendKinds.IrBackend>.() -> Unit = {}
) {
    configureNamedHandlersStep(LOWERED_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, skipMissingStep = false, init)
}

inline fun TestConfigurationBuilder.configureJvmArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Jvm, ArtifactKinds.Jvm>.() -> Unit = {}
) {
    configureNamedHandlersStep(JVM_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Jvm, skipMissingStep = false, init)
}

inline fun TestConfigurationBuilder.configureJsArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Js, ArtifactKinds.Js>.() -> Unit = {}
) {
    configureNamedHandlersStep(JS_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Js, skipMissingStep = false, init)
}

inline fun TestConfigurationBuilder.configureWasmArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Wasm, ArtifactKinds.Wasm>.() -> Unit = {}
) {
    configureNamedHandlersStep(WASM_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Wasm, skipMissingStep = false, init)
}

inline fun TestConfigurationBuilder.configureKlibArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.KLib, ArtifactKinds.KLib>.() -> Unit = {}
) {
    configureNamedHandlersStep(KLIB_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.KLib, skipMissingStep = false, init)
}

/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.builders

import org.jetbrains.kotlin.test.HandlersStepBuilder
import org.jetbrains.kotlin.test.backend.classic.ClassicJvmBackendFacade
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendFacade
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.CLASSIC_FRONTEND_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.FIR_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.JS_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.JVM_ARTIFACTS_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.builders.CompilerStepsNames.RAW_IR_HANDLERS_STEP_NAME
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2ClassicBackendConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontend2IrConverter
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendFacade
import org.jetbrains.kotlin.test.frontend.classic.ClassicFrontendOutputArtifact
import org.jetbrains.kotlin.test.frontend.fir.Fir2IrResultsConverter
import org.jetbrains.kotlin.test.frontend.fir.FirFrontendFacade
import org.jetbrains.kotlin.test.frontend.fir.FirOutputArtifact
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds

object CompilerStepsNames {
    const val FRONTEND_STEP_NAME = "frontend"
    const val CLASSIC_FRONTEND_HANDLERS_STEP_NAME = "classic frontend handlers"
    const val FIR_HANDLERS_STEP_NAME = "FIR frontend handlers"

    const val CONVERTER_STEP_NAME = "converter"
    const val RAW_IR_HANDLERS_STEP_NAME = "raw IR handlers"

    const val JVM_BACKEND_STEP_NAME = "jvm backend"
    const val JVM_ARTIFACTS_HANDLERS_STEP_NAME = "jvm artifacts handlers"
    const val JS_ARTIFACTS_HANDLERS_STEP_NAME = "js artifacts handlers"

}

// --------------------- default compiler steps ---------------------

fun TestConfigurationBuilder.classicFrontendStep() {
    facadeStep(::ClassicFrontendFacade)
}

fun TestConfigurationBuilder.firFrontendStep() {
    facadeStep(::FirFrontendFacade)
}

fun TestConfigurationBuilder.psi2ClassicBackendStep() {
    facadeStep(::ClassicFrontend2ClassicBackendConverter)
}

fun TestConfigurationBuilder.psi2IrStep() {
    facadeStep(::ClassicFrontend2IrConverter)
}

fun TestConfigurationBuilder.fir2IrStep() {
    facadeStep(::Fir2IrResultsConverter)
}

fun TestConfigurationBuilder.classicJvmBackendStep() {
    facadeStep(::ClassicJvmBackendFacade)
}

fun TestConfigurationBuilder.jvmIrBackendStep() {
    facadeStep(::JvmIrBackendFacade)
}

// --------------------- default handlers steps ---------------------

// use those ones to define new step
inline fun TestConfigurationBuilder.classicFrontendHandlersStep(
    init: HandlersStepBuilder<ClassicFrontendOutputArtifact>.() -> Unit = {}
) {
    namedHandlersStep(CLASSIC_FRONTEND_HANDLERS_STEP_NAME, FrontendKinds.ClassicFrontend, init)
}

inline fun TestConfigurationBuilder.firHandlersStep(
    init: HandlersStepBuilder<FirOutputArtifact>.() -> Unit = {}
) {
    namedHandlersStep(FIR_HANDLERS_STEP_NAME, FrontendKinds.FIR, init)
}

inline fun TestConfigurationBuilder.irHandlersStep(
    init: HandlersStepBuilder<IrBackendInput>.() -> Unit = {}
) {
    namedHandlersStep(RAW_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, init)
}

inline fun TestConfigurationBuilder.jvmArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Jvm>.() -> Unit = {}
) {
    namedHandlersStep(JVM_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Jvm, init)
}

inline fun TestConfigurationBuilder.jsArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Js>.() -> Unit = {}
) {
    namedHandlersStep(JS_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Js, init)
}

// and those ones to configure already defined step
inline fun TestConfigurationBuilder.configureClassicFrontendHandlersStep(
    init: HandlersStepBuilder<ClassicFrontendOutputArtifact>.() -> Unit = {}
) {
    configureNamedHandlersStep(CLASSIC_FRONTEND_HANDLERS_STEP_NAME, FrontendKinds.ClassicFrontend, init)
}

inline fun TestConfigurationBuilder.configureFirHandlersStep(
    init: HandlersStepBuilder<FirOutputArtifact>.() -> Unit = {}
) {
    configureNamedHandlersStep(FIR_HANDLERS_STEP_NAME, FrontendKinds.FIR, init)
}

inline fun TestConfigurationBuilder.configureIrHandlersStep(
    init: HandlersStepBuilder<IrBackendInput>.() -> Unit = {}
) {
    configureNamedHandlersStep(RAW_IR_HANDLERS_STEP_NAME, BackendKinds.IrBackend, init)
}

inline fun TestConfigurationBuilder.configureJvmArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Jvm>.() -> Unit = {}
) {
    configureNamedHandlersStep(JVM_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Jvm, init)
}

inline fun TestConfigurationBuilder.configureJsArtifactsHandlersStep(
    init: HandlersStepBuilder<BinaryArtifacts.Js>.() -> Unit = {}
) {
    configureNamedHandlersStep(JS_ARTIFACTS_HANDLERS_STEP_NAME, ArtifactKinds.Js, init)
}

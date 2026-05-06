/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.serialization.KotlinFileSerializedData
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.cli.pipeline.LoadedIrPipelineArtifact
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import java.io.File

data class JsIrAfterFrontendBackendInput(
    override val irModuleFragment: IrModuleFragment,
    override val irBuiltIns: IrBuiltIns,
    val icData: List<KotlinFileSerializedData>,
    override val diagnosticReporter: BaseDiagnosticsCollector,
    val hasErrors: Boolean,
    override val irMangler: KotlinMangler.IrMangler,
    val metadataSerializer: KlibSingleFileMetadataSerializer<*>,
) : IrBackendInput()

data class WasmAfterFrontendBackendInput(
    override val irModuleFragment: IrModuleFragment,
    override val irBuiltIns: IrBuiltIns,
    val icData: List<KotlinFileSerializedData>,
    override val diagnosticReporter: BaseDiagnosticsCollector,
    val hasErrors: Boolean,
    override val irMangler: KotlinMangler.IrMangler,
    val metadataSerializer: KlibSingleFileMetadataSerializer<*>,
) : IrBackendInput()

class JvmIrBackendInput(
    val state: GenerationState,
    val codegenFactory: JvmIrCodegenFactory,
    val backendInput: JvmIrCodegenFactory.BackendInput,
    val sourceFiles: List<KtSourceFile>,
    override val irMangler: KotlinMangler.IrMangler,
) : IrBackendInput() {
    override val irModuleFragment: IrModuleFragment
        get() = backendInput.irModuleFragment

    override val irBuiltIns: IrBuiltIns
        get() = backendInput.irBuiltIns

    override val diagnosticReporter: BaseDiagnosticsCollector
        get() = state.diagnosticReporter as BaseDiagnosticsCollector
}

data class DeserializedFromKlibBackendInput<A : LoadedIrPipelineArtifact>(
    val cliArtifact: A,
    val klib: File,
) : IrBackendInput() {
    override val diagnosticReporter: BaseDiagnosticsCollector = DiagnosticsCollectorStub()

    val moduleInfo: IrModuleInfo
        get() = cliArtifact.moduleInfo

    override val irModuleFragment: IrModuleFragment
        get() = moduleInfo.module

    override val irBuiltIns: IrBuiltIns
        get() = moduleInfo.bultins

    override val irMangler: KotlinMangler.IrMangler
        get() = moduleInfo.deserializer.fakeOverrideBuilder.mangler
}

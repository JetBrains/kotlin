/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizedResult
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact

// IR backend (JVM, JS, Native, Wasm)
sealed class IrBackendInput : ResultingArtifact.BackendInput<IrBackendInput>() {
    override val kind: BackendKind<IrBackendInput>
        get() = BackendKinds.IrBackend

    abstract val irModuleFragment: IrModuleFragment

    /**
     * It's actual only with MPP where every source module is separated
     */
    abstract val dependentIrModuleFragments: List<IrModuleFragment>

    /**
     * Here plugin context can be used as a service for inspecting resulting IR module
     */
    abstract val irPluginContext: IrPluginContext

    /**
     * The mangler instance that was used to build declaration signatures from (possibly deserialized) K1 descriptors for this backend.
     *
     * This instance can be used to verify signatures in tests.
     *
     * @see org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
     * @see org.jetbrains.kotlin.ir.util.IdSignature
     */
    abstract val descriptorMangler: KotlinMangler.DescriptorMangler

    /**
     * The mangler instance that was used to build declaration signatures from IR declarations for this backend.
     *
     * @see org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
     * @see org.jetbrains.kotlin.ir.util.IdSignature
     */
    abstract val irMangler: KotlinMangler.IrMangler

    /**
     * The mangler instance that was used to build declaration signatures from K2 (FIR) declarations for this backend, or `null` if
     * this artifact was compiled using the classic frontend.
     *
     * This instance can be used to verify signatures in tests.
     *
     * @see org.jetbrains.kotlin.fir.backend.FirMangleComputer
     * @see org.jetbrains.kotlin.ir.util.IdSignature
     */
    abstract val firMangler: FirMangler?

    /**
     * Results of actualization, came from IrActulizer
     *
     * @see org.jetbrains.kotlin.backend.common.actualizer
     * @see org.jetbrains.kotlin.test.backend.ir.IrActualizerAndPluginsFacade
     */
    abstract var irActualizerResult: IrActualizedResult?

    abstract val diagnosticReporter: BaseDiagnosticsCollector

    class JsIrBackendInput(
        override val irModuleFragment: IrModuleFragment,
        override val dependentIrModuleFragments: List<IrModuleFragment>,
        override val irPluginContext: IrPluginContext,
        val sourceFiles: List<KtSourceFile>,
        val icData: List<KotlinFileSerializedData>,
        override val diagnosticReporter: BaseDiagnosticsCollector,
        val hasErrors: Boolean,
        override val descriptorMangler: KotlinMangler.DescriptorMangler,
        override val irMangler: KotlinMangler.IrMangler,
        override val firMangler: FirMangler?,
        override var irActualizerResult: IrActualizedResult? = null,
        val serializeSingleFile: (KtSourceFile, IrActualizedResult?) -> ProtoBuf.PackageFragment,
    ) : IrBackendInput()

    data class JsIrDeserializedFromKlibBackendInput(
        override val irModuleFragment: IrModuleFragment,
        override val dependentIrModuleFragments: List<IrModuleFragment>,
        override val irPluginContext: IrPluginContext,
        override val diagnosticReporter: BaseDiagnosticsCollector,
        override val descriptorMangler: KotlinMangler.DescriptorMangler,
        override val irMangler: KotlinMangler.IrMangler,
        override val firMangler: FirMangler?,
    ) : IrBackendInput() {

        override val kind: BackendKind<IrBackendInput>
            get() = BackendKinds.DeserializedIrBackend

        override var irActualizerResult: IrActualizedResult? = null
    }

    class WasmBackendInput(
        override val irModuleFragment: IrModuleFragment,
        override val dependentIrModuleFragments: List<IrModuleFragment>,
        override val irPluginContext: IrPluginContext,
        val sourceFiles: List<KtSourceFile>,
        val icData: List<KotlinFileSerializedData>,
        override val diagnosticReporter: BaseDiagnosticsCollector,
        val hasErrors: Boolean,
        override val descriptorMangler: KotlinMangler.DescriptorMangler,
        override val irMangler: KotlinMangler.IrMangler,
        override val firMangler: FirMangler?,
        override var irActualizerResult: IrActualizedResult? = null,
        val serializeSingleFile: (KtSourceFile, IrActualizedResult?) -> ProtoBuf.PackageFragment,
    ) : IrBackendInput()

    class JvmIrBackendInput(
        val state: GenerationState,
        val codegenFactory: JvmIrCodegenFactory,
        val backendInput: JvmIrCodegenFactory.JvmIrBackendInput,
        override val dependentIrModuleFragments: List<IrModuleFragment>,
        val sourceFiles: List<KtSourceFile>,
        override val descriptorMangler: KotlinMangler.DescriptorMangler,
        override val irMangler: KotlinMangler.IrMangler,
        override val firMangler: FirMangler?,
        override var irActualizerResult: IrActualizedResult? = null,
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = backendInput.irModuleFragment

        override val irPluginContext: IrPluginContext
            get() = backendInput.pluginContext

        override val diagnosticReporter: BaseDiagnosticsCollector
            get() = state.diagnosticReporter as BaseDiagnosticsCollector
    }
}

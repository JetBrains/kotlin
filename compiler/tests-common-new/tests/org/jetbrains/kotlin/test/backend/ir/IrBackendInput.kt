/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.IrModuleInfo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.KotlinFileSerializedData
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorHolder
import org.jetbrains.kotlin.test.diagnostics.DiagnosticsCollectorStub
import org.jetbrains.kotlin.test.model.BackendKind
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact
import java.io.File

// IR backend (JVM, JS, Native, Wasm)
abstract class IrBackendInput : ResultingArtifact.BackendInput<IrBackendInput>(), DiagnosticsCollectorHolder {
    override val kind: BackendKind<IrBackendInput>
        get() = BackendKinds.IrBackend

    abstract val irModuleFragment: IrModuleFragment

    /**
     * Here plugin context can be used as a service for inspecting resulting IR module
     */
    abstract val irPluginContext: IrPluginContext

    /**
     * The mangler instance that was used to build declaration signatures from (possibly deserialized) K1 descriptors for this backend,
     * or `null` if this artifact was compiled using the K2 frontend.
     *
     * This instance can be used to verify signatures in tests.
     *
     * @see org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.DescriptorMangleComputer
     * @see org.jetbrains.kotlin.ir.util.IdSignature
     */
    abstract val descriptorMangler: KotlinMangler.DescriptorMangler?

    /**
     * The mangler instance that was used to build declaration signatures from IR declarations for this backend.
     *
     * @see org.jetbrains.kotlin.backend.common.serialization.mangle.ir.IrMangleComputer
     * @see org.jetbrains.kotlin.ir.util.IdSignature
     */
    abstract val irMangler: KotlinMangler.IrMangler

    abstract override val diagnosticReporter: BaseDiagnosticsCollector

    sealed interface DeserializedFromKlib {
        val klib: File
        val moduleInfo: IrModuleInfo
    }

    sealed class JsIrBackendInput : IrBackendInput()

    data class JsIrAfterFrontendBackendInput(
        override val irModuleFragment: IrModuleFragment,
        override val irPluginContext: IrPluginContext,
        val icData: List<KotlinFileSerializedData>,
        override val diagnosticReporter: BaseDiagnosticsCollector,
        val hasErrors: Boolean,
        override val descriptorMangler: KotlinMangler.DescriptorMangler?,
        override val irMangler: KotlinMangler.IrMangler,
        val metadataSerializer: KlibSingleFileMetadataSerializer<*>,
    ) : JsIrBackendInput()

    class JsIrDeserializedFromKlibBackendInput(
        override val moduleInfo: IrModuleInfo,
        override val klib: File,
        override val irPluginContext: IrPluginContext,
    ) : DeserializedFromKlib, JsIrBackendInput() {
        override val diagnosticReporter: BaseDiagnosticsCollector = DiagnosticsCollectorStub()
        override val irModuleFragment: IrModuleFragment
            get() = moduleInfo.module

        override val descriptorMangler: KotlinMangler.DescriptorMangler?
            get() = moduleInfo.symbolTable.signaturer?.mangler

        override val irMangler: KotlinMangler.IrMangler
            get() = moduleInfo.deserializer.fakeOverrideBuilder.mangler
    }

    sealed class WasmBackendInput : IrBackendInput()

    class WasmAfterFrontendBackendInput(
        override val irModuleFragment: IrModuleFragment,
        override val irPluginContext: IrPluginContext,
        val icData: List<KotlinFileSerializedData>,
        override val diagnosticReporter: BaseDiagnosticsCollector,
        val hasErrors: Boolean,
        override val descriptorMangler: KotlinMangler.DescriptorMangler?,
        override val irMangler: KotlinMangler.IrMangler,
        val metadataSerializer: KlibSingleFileMetadataSerializer<*>,
    ) : WasmBackendInput()

    class WasmDeserializedFromKlibBackendInput(
        override val moduleInfo: IrModuleInfo,
        override val klib: File,
        override val irPluginContext: IrPluginContext,
    ) : DeserializedFromKlib, WasmBackendInput() {
        override val diagnosticReporter: BaseDiagnosticsCollector = DiagnosticsCollectorStub()
        override val irModuleFragment: IrModuleFragment
            get() = moduleInfo.module

        override val descriptorMangler: KotlinMangler.DescriptorMangler?
            get() = moduleInfo.symbolTable.signaturer?.mangler

        override val irMangler: KotlinMangler.IrMangler
            get() = moduleInfo.deserializer.fakeOverrideBuilder.mangler
    }

    class JvmIrBackendInput(
        val state: GenerationState,
        val codegenFactory: JvmIrCodegenFactory,
        val backendInput: JvmIrCodegenFactory.BackendInput,
        val sourceFiles: List<KtSourceFile>,
        override val descriptorMangler: KotlinMangler.DescriptorMangler?,
        override val irMangler: KotlinMangler.IrMangler,
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = backendInput.irModuleFragment

        override val irPluginContext: IrPluginContext
            get() = backendInput.pluginContext!!

        override val diagnosticReporter: BaseDiagnosticsCollector
            get() = state.diagnosticReporter as BaseDiagnosticsCollector
    }

    sealed class NativeBackendInput : IrBackendInput()

    /**
     * Note: For the classic frontend [metadataSerializer] is null, since the Native backend uses
     * [org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer] which serializes a whole module.
     *
     * @property usedLibrariesForManifest - The list of dependency libraries that should be written to the produced KLIB
     *   manifest's `depends=` property. This list includes:
     *   - direct dependencies (ones that were explicitly specified by `// MODULE` test directives in test data)
     *   - and "default" dependencies (anything that is implicitly added by the Kotlin/Native compiler, ex: stdlib & platform libraries),
     *     BUT only if such libraries were actually used during the compilation.
     */
    data class NativeAfterFrontendBackendInput(
        override val irModuleFragment: IrModuleFragment,
        override val irPluginContext: IrPluginContext,
        override val diagnosticReporter: BaseDiagnosticsCollector,
        override val descriptorMangler: KotlinMangler.DescriptorMangler?,
        override val irMangler: KotlinMangler.IrMangler,
        val metadataSerializer: KlibSingleFileMetadataSerializer<*>?,
        val usedLibrariesForManifest: List<KotlinLibrary>,
    ) : NativeBackendInput()

    class NativeDeserializedFromKlibBackendInput(
        override val moduleInfo: IrModuleInfo,
        override val klib: File,
        override val irPluginContext: IrPluginContext,
    ) : DeserializedFromKlib, NativeBackendInput() {
        override val diagnosticReporter: BaseDiagnosticsCollector = DiagnosticsCollectorStub()
        override val irModuleFragment: IrModuleFragment
            get() = moduleInfo.module

        override val descriptorMangler: KotlinMangler.DescriptorMangler?
            get() = moduleInfo.symbolTable.signaturer?.mangler

        override val irMangler: KotlinMangler.IrMangler
            get() = moduleInfo.deserializer.fakeOverrideBuilder.mangler
    }
}

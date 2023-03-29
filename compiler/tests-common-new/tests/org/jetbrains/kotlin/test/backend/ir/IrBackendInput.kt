/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.ir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.actualizer.IrActualizationResult
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.signaturer.FirMangler
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.ResultingArtifact

// IR backend (JVM, JS, Native)
sealed class IrBackendInput : ResultingArtifact.BackendInput<IrBackendInput>() {
    override val kind: BackendKinds.IrBackend
        get() = BackendKinds.IrBackend

    abstract val irModuleFragment: IrModuleFragment

    /**
     * Here plugin context can be used as a service for inspecting resulting IR module
     */
    abstract val irPluginContext: IrPluginContext

    /**
     * The mangler used to build declaration signatures from K1 descriptors for this backend
     */
    abstract val descriptorMangler: KotlinMangler.DescriptorMangler

    /**
     * The mangler over IR declarations for this backend
     */
    abstract val irMangler: KotlinMangler.IrMangler

    /**
     * The mangler used to build declaration signatures from K2 declarations for this backend
     */
    abstract val firMangler: FirMangler?

    data class JsIrBackendInput(
        val mainModuleFragment: IrModuleFragment,
        val dependentModuleFragments: List<IrModuleFragment>,
        override val irPluginContext: IrPluginContext,
        val sourceFiles: List<KtSourceFile>,
        val icData: List<KotlinFileSerializedData>,
        val expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>, // TODO: abstract from descriptors
        val diagnosticsCollector: BaseDiagnosticsCollector,
        val hasErrors: Boolean,
        override val descriptorMangler: KotlinMangler.DescriptorMangler,
        override val irMangler: KotlinMangler.IrMangler,
        override val firMangler: FirMangler?,
        val serializeSingleFile: (KtSourceFile, IrActualizationResult?) -> ProtoBuf.PackageFragment,
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = mainModuleFragment
    }

    data class JvmIrBackendInput(
        val state: GenerationState,
        val codegenFactory: JvmIrCodegenFactory,
        val dependentInputs: List<JvmIrCodegenFactory.JvmIrBackendInput>,
        val backendInput: JvmIrCodegenFactory.JvmIrBackendInput,
        val sourceFiles: List<KtSourceFile>,
        override val descriptorMangler: KotlinMangler.DescriptorMangler,
        override val irMangler: KotlinMangler.IrMangler,
        override val firMangler: FirMangler?,
    ) : IrBackendInput() {
        override val irModuleFragment: IrModuleFragment
            get() = backendInput.irModuleFragment

        override val irPluginContext: IrPluginContext
            get() = backendInput.pluginContext
    }
}

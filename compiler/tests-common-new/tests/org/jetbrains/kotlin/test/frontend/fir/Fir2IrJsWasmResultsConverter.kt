/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.extractFirDeclarations
import org.jetbrains.kotlin.fir.backend.js.FirJsKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.KotlinFileSerializedData
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.utils.metadataVersion

abstract class Fir2IrJsWasmResultsConverter(testServices: TestServices) : AbstractFir2IrNonJvmResultsConverter(testServices) {
    protected abstract val artifactFactory: (
        IrModuleFragment,
        IrPluginContext,
        List<KtSourceFile>,
        List<KotlinFileSerializedData>,
        BaseDiagnosticsCollector,
        Boolean,
        KotlinMangler.DescriptorMangler,
        KotlinMangler.IrMangler,
        FirMangler?,
        (KtSourceFile) -> ProtoBuf.PackageFragment
    ) -> IrBackendInput

    override fun createDescriptorMangler(): KotlinMangler.DescriptorMangler {
        return JsManglerDesc
    }

    override fun createIrMangler(): KotlinMangler.IrMangler {
        return JsManglerIr
    }

    override fun createFirMangler(): FirMangler {
        return FirJsKotlinMangler()
    }

    override val klibFactories: KlibMetadataFactories
        get() = JsFactories

    override fun createBackendInput(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        firFilesAndComponentsBySourceFile: Map<KtSourceFile, Pair<FirFile, Fir2IrComponents>>,
        sourceFiles: List<KtSourceFile>,
    ): IrBackendInput {
        val languageVersionSettings = compilerConfiguration.languageVersionSettings
        val metadataVersion = compilerConfiguration.metadataVersion(languageVersionSettings.languageVersion)
        val fir2IrComponents = fir2IrResult.components
        val manglers = fir2IrComponents.manglers
        return artifactFactory(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext,
            sourceFiles,
            compilerConfiguration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList(),
            diagnosticReporter,
            inputArtifact.hasErrors,
            manglers.descriptorMangler,
            manglers.irMangler,
            manglers.firMangler,
        ) { file ->
            val (firFile, components) = firFilesAndComponentsBySourceFile[file]
                ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
            val actualizedExpectDeclarations = fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
            serializeSingleFirFile(
                firFile,
                components.session,
                components.scopeSession,
                actualizedExpectDeclarations,
                FirKLibSerializerExtension(
                    components.session, metadataVersion,
                    ConstValueProviderImpl(components),
                    allowErrorTypes = false, exportKDoc = false,
                    components.annotationsFromPluginRegistrar.createAdditionalMetadataProvider()
                ),
                languageVersionSettings,
            )
        }
    }
}

class Fir2IrJsResultsConverter(testServices: TestServices) : Fir2IrJsWasmResultsConverter(testServices) {
    override val artifactFactory: (IrModuleFragment, IrPluginContext, List<KtSourceFile>, List<KotlinFileSerializedData>, BaseDiagnosticsCollector, Boolean, KotlinMangler.DescriptorMangler, KotlinMangler.IrMangler, FirMangler?, (KtSourceFile) -> ProtoBuf.PackageFragment) -> IrBackendInput
        get() = IrBackendInput::JsIrBackendInput

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        return resolveLibraries(compilerConfiguration, getAllJsDependenciesPaths(module, testServices))
    }
}


class Fir2IrWasmResultsConverter(testServices: TestServices) : Fir2IrJsWasmResultsConverter(testServices) {
    override val artifactFactory: (IrModuleFragment, IrPluginContext, List<KtSourceFile>, List<KotlinFileSerializedData>, BaseDiagnosticsCollector, Boolean, KotlinMangler.DescriptorMangler, KotlinMangler.IrMangler, FirMangler?, (KtSourceFile) -> ProtoBuf.PackageFragment) -> IrBackendInput
        get() = IrBackendInput::WasmBackendInput

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        return resolveWasmLibraries(module, testServices, compilerConfiguration)
    }
}

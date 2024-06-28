/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.KotlinFileSerializedData
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices

internal abstract class Fir2IrJsWasmResultsConverter(testServices: TestServices) : AbstractFir2IrResultsConverter(testServices) {
    protected abstract val artifactFactory: (
        IrModuleFragment,
        IrPluginContext,
        List<KotlinFileSerializedData>,
        BaseDiagnosticsCollector,
        Boolean,
        KotlinMangler.DescriptorMangler?,
        KotlinMangler.IrMangler,
        KlibSingleFileMetadataSerializer<*>,
    ) -> IrBackendInput

    override fun createIrMangler(): KotlinMangler.IrMangler = JsManglerIr
    override fun createFir2IrExtensions(compilerConfiguration: CompilerConfiguration): Fir2IrExtensions = Fir2IrExtensions.Default
    override fun createFir2IrVisibilityConverter(): Fir2IrVisibilityConverter = Fir2IrVisibilityConverter.Default
    override fun createTypeSystemContextProvider(): (IrBuiltIns) -> IrTypeSystemContext = ::IrTypeSystemContextImpl
    override fun createSpecialAnnotationsProvider(): IrSpecialAnnotationsProvider? = null
    override fun createExtraActualDeclarationExtractorInitializer(): (Fir2IrComponents) -> IrExtraActualDeclarationExtractor? = { null }

    override val klibFactories: KlibMetadataFactories
        get() = JsFactories

    override fun createBackendInput(
        module: TestModule,
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        return artifactFactory(
            fir2IrResult.irModuleFragment,
            fir2IrResult.pluginContext,
            compilerConfiguration.incrementalDataProvider?.getSerializedData(fir2KlibMetadataSerializer.sourceFiles) ?: emptyList(),
            diagnosticReporter,
            testServices.firDiagnosticCollectorService.containsErrors(inputArtifact),
            /*descriptorMangler = */null,
            fir2IrResult.components.irMangler,
            fir2KlibMetadataSerializer,
        )
    }
}

@InternalFir2IrConverterAPI
internal class Fir2IrJsResultsConverter(testServices: TestServices) : Fir2IrJsWasmResultsConverter(testServices) {
    override val artifactFactory: (IrModuleFragment, IrPluginContext, List<KotlinFileSerializedData>, BaseDiagnosticsCollector, Boolean, KotlinMangler.DescriptorMangler?, KotlinMangler.IrMangler, KlibSingleFileMetadataSerializer<*>) -> IrBackendInput
        get() = IrBackendInput::JsIrAfterFrontendBackendInput

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        return resolveLibraries(compilerConfiguration, getAllJsDependenciesPaths(module, testServices))
    }
}

@InternalFir2IrConverterAPI
internal class Fir2IrWasmResultsConverter(testServices: TestServices) : Fir2IrJsWasmResultsConverter(testServices) {
    override val artifactFactory: (IrModuleFragment, IrPluginContext, List<KotlinFileSerializedData>, BaseDiagnosticsCollector, Boolean, KotlinMangler.DescriptorMangler?, KotlinMangler.IrMangler, KlibSingleFileMetadataSerializer<*>) -> IrBackendInput.WasmAfterFrontendBackendInput
        get() = IrBackendInput::WasmAfterFrontendBackendInput

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        return resolveWasmLibraries(module, testServices, compilerConfiguration)
    }
}

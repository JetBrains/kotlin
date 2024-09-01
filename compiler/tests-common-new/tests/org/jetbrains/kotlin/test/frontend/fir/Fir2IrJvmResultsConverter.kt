/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider

@InternalFir2IrConverterAPI
internal class Fir2IrJvmResultsConverter(testServices: TestServices) : AbstractFir2IrResultsConverter(testServices) {
    override fun createIrMangler(): KotlinMangler.IrMangler = JvmIrMangler

    override fun createFir2IrExtensions(compilerConfiguration: CompilerConfiguration): JvmFir2IrExtensions {
        return JvmFir2IrExtensions(compilerConfiguration, JvmIrDeserializerImpl())
    }

    override fun createFir2IrVisibilityConverter(): Fir2IrVisibilityConverter {
        return FirJvmVisibilityConverter
    }

    override fun createTypeSystemContextProvider(): (IrBuiltIns) -> IrTypeSystemContext {
        return ::JvmIrTypeSystemContext
    }

    override fun createSpecialAnnotationsProvider(): IrSpecialAnnotationsProvider {
        return JvmIrSpecialAnnotationSymbolProvider
    }

    override fun createExtraActualDeclarationExtractorInitializer(): (Fir2IrComponents) -> IrExtraActualDeclarationExtractor? {
        return FirJvmBuiltinProviderActualDeclarationExtractor.Companion::initializeIfNeeded
    }

    override fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary> {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        // Create and initialize the module and its dependencies
        compilerConfigurationProvider.getProject(module)
        return emptyList()
    }

    override val klibFactories: KlibMetadataFactories
        get() = error("Should not be called")

    override fun createBackendInput(
        module: TestModule,
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        val phaseConfig = compilerConfiguration.get(CLIConfigurationKeys.PHASE_CONFIG)
        // TODO: handle fir from light tree
        val sourceFiles = inputArtifact.mainFirFiles.mapNotNull { it.value.sourceFile }

        val backendInput = JvmIrCodegenFactory.JvmIrBackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.symbolTable,
            phaseConfig,
            fir2IrResult.components.irProviders,
            createFir2IrExtensions(compilerConfiguration),
            FirJvmBackendExtension(
                fir2IrResult.components,
                fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations(),
            ),
            fir2IrResult.pluginContext,
            notifyCodegenStart = {},
        )

        val project = testServices.compilerConfigurationProvider.getProject(module)
        val codegenFactory = JvmIrCodegenFactory(compilerConfiguration, phaseConfig)
        val generationState = GenerationState.Builder(
            project, ClassBuilderFactories.TEST,
            fir2IrResult.irModuleFragment.descriptor, NoScopeRecordCliBindingTrace(project).bindingContext, compilerConfiguration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(fir2IrResult.components)
        ).diagnosticReporter(
            diagnosticReporter
        ).build()

        return IrBackendInput.JvmIrBackendInput(
            generationState,
            codegenFactory,
            backendInput,
            sourceFiles,
            descriptorMangler = null,
            irMangler = fir2IrResult.components.irMangler,
        )
    }
}

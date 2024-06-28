/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
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

class Fir2IrJvmResultsConverter(testServices: TestServices) : AbstractFir2IrNonJvmResultsConverter(testServices) {
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
        return emptyList()
    }

    override val klibFactories: KlibMetadataFactories
        get() = error("Should not be called")

    override fun transformInternal(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput.JvmIrBackendInput {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val irMangler = JvmIrMangler
        val fir2IrExtensions = createFir2IrExtensions(configuration)

        // Create and initialize the module and its dependencies
        val project = compilerConfigurationProvider.getProject(module)
        // TODO: handle fir from light tree
        val sourceFiles = inputArtifact.mainFirFiles.mapNotNull { it.value.sourceFile }

        val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)

        val diagnosticReporter = DiagnosticReporterFactory.createReporter()

        val compilerConfiguration = compilerConfigurationProvider.getCompilerConfiguration(module)
        val fir2IrConfiguration = Fir2IrConfiguration.forJvmCompilation(compilerConfiguration, diagnosticReporter)

        val fir2irResult = inputArtifact.toFirResult().convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            module.irGenerationExtensions(testServices),
            irMangler,
            createFir2IrVisibilityConverter(),
            DefaultBuiltIns.Instance,
            createTypeSystemContextProvider(),
            createSpecialAnnotationsProvider(),
            createExtraActualDeclarationExtractorInitializer(),
        )

        val backendInput = JvmIrCodegenFactory.JvmIrBackendInput(
            fir2irResult.irModuleFragment,
            fir2irResult.symbolTable,
            phaseConfig,
            fir2irResult.components.irProviders,
            fir2IrExtensions,
            FirJvmBackendExtension(
                fir2irResult.components,
                fir2irResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations(),
            ),
            fir2irResult.pluginContext,
            notifyCodegenStart = {},
        )

        val codegenFactory = JvmIrCodegenFactory(configuration, phaseConfig)
        val generationState = GenerationState.Builder(
            project, ClassBuilderFactories.TEST,
            fir2irResult.irModuleFragment.descriptor, NoScopeRecordCliBindingTrace(project).bindingContext, configuration
        ).isIrBackend(
            true
        ).jvmBackendClassResolver(
            FirJvmBackendClassResolver(fir2irResult.components)
        ).diagnosticReporter(
            diagnosticReporter
        ).build()

        return IrBackendInput.JvmIrBackendInput(
            generationState,
            codegenFactory,
            backendInput,
            sourceFiles,
            descriptorMangler = null,
            irMangler = fir2irResult.components.irMangler,
        )
    }

    override fun createBackendInput(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        TODO()
    }
}

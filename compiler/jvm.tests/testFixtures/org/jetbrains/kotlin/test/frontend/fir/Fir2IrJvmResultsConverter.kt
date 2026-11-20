/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.JvmIrCodegenFactory
import org.jetbrains.kotlin.backend.jvm.JvmIrSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.backend.jvm.JvmIrTypeSystemContext
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.ClassBuilderFactories
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.jvm.*
import org.jetbrains.kotlin.fir.backend.utils.extractFirDeclarations
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.Fir2IrActualizedResult
import org.jetbrains.kotlin.fir.pipeline.Fir2KlibMetadataSerializer
import org.jetbrains.kotlin.fir.pipeline.SingleModuleFrontendOutput
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name.special
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.backend.ir.JvmIrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class Fir2IrJvmResultsConverter(testServices: TestServices) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {

    private fun resolveLibraries(module: TestModule): List<KotlinLibrary> {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        // Create and initialize the module and its dependencies
        compilerConfigurationProvider.getProject(module)
        return emptyList()
    }

    override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::FirDiagnosticCollectorService),
        )

    override fun transform(module: TestModule, inputArtifact: FirOutputArtifact): IrBackendInput? =
        try {
            transformInternal(module, inputArtifact)
        } catch (e: Throwable) {
            if (
                CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS in module.directives &&
                testServices.firDiagnosticCollectorService.containsErrors(inputArtifact)
            ) {
                null
            } else {
                throw e
            }
        }

    private fun transformInternal(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)

        val irMangler = JvmIrMangler
        val diagnosticReporter = DiagnosticsCollectorImpl()

        val fir2IrExtensions = JvmFir2IrExtensions()

        val libraries: List<KotlinLibrary> = resolveLibraries(module)
        val [dependencies: List<ModuleDescriptor>, builtIns: KotlinBuiltIns?] = loadModuleDescriptors(
            libraries,
            testServices.targetPlatformProvider.getTargetPlatform(module),
        )

        val fir2IrConfiguration = createFir2IrConfiguration(compilerConfiguration, diagnosticReporter)

        val outputs = inputArtifact.partsForDependsOnModules.map {
            SingleModuleFrontendOutput(it.session, it.scopeSession, it.firFilesByTestFile.values.toList())
        }
        val firResult = AllModulesFrontendOutput(outputs)
        val fir2irResult = firResult.convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            compilerConfiguration.getCompilerExtensions(IrGenerationExtension),
            irMangler,
            FirJvmVisibilityConverter,
            builtIns ?: DefaultBuiltIns.Instance, // TODO: consider passing externally,
            ::JvmIrTypeSystemContext,
            createSpecialAnnotationsProvider = ::JvmIrSpecialAnnotationSymbolProvider,
            extraActualDeclarationExtractorsInitializer = { it: Fir2IrComponents ->
                listOfNotNull(FirDirectJavaActualDeclarationExtractor.initializeIfNeeded(it))
            },
        ).also {
            (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = dependencies }
        }

        return createBackendInput(
            module,
            compilerConfiguration,
            diagnosticReporter,
            inputArtifact,
            fir2irResult,
            Fir2KlibMetadataSerializer(
                compilerConfiguration,
                firResult.outputs,
                fir2irResult,
                produceHeaderKlib = false,
            ),
        )
    }

    private  fun createFir2IrConfiguration(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
    ): Fir2IrConfiguration = Fir2IrConfiguration.forJvmCompilation(compilerConfiguration, diagnosticReporter)

    private fun createBackendInput(
        module: TestModule,
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput {
        // TODO: handle fir from light tree
        val sourceFiles = inputArtifact.mainFirFilesByTestFile.mapNotNull { it.value.sourceFile }

        val backendInput = JvmIrCodegenFactory.BackendInput(
            fir2IrResult.irModuleFragment,
            fir2IrResult.irBuiltIns,
            fir2IrResult.symbolTable,
            debuggerExtensions = null,
            FirJvmBackendExtension(
                fir2IrResult.components,
                fir2IrResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations(),
            ),
            fir2IrResult.pluginContext,
        )

        val project = testServices.compilerConfigurationProvider.getProject(module)
        val generationState = GenerationState(
            project, fir2IrResult.irModuleFragment.descriptor, compilerConfiguration, ClassBuilderFactories.TEST,
            jvmBackendClassResolver = FirJvmBackendClassResolver(fir2IrResult.components),
            diagnosticReporter = diagnosticReporter,
        )

        return JvmIrBackendInput(
            generationState,
            JvmIrCodegenFactory(compilerConfiguration),
            backendInput,
            sourceFiles,
            irMangler = fir2IrResult.components.irMangler,
        )
    }

    private fun loadModuleDescriptors(
        libraries: List<KotlinLibrary>,
        targetPlatform: TargetPlatform,
    ): Pair<List<ModuleDescriptor>, KotlinBuiltIns?> {
        val stdlib: KotlinLibrary? = libraries.firstOrNull { it.isAnyPlatformStdlib }

        var builtIns: KotlinBuiltIns? = null
        val dependencies = mutableListOf<ModuleDescriptorImpl>()

        fun loadDescriptor(library: KotlinLibrary): ModuleDescriptorImpl {
            val moduleName = special("<${library.uniqueName}>")
            val moduleOrigin = DeserializedKlibModuleOrigin(library)
            val builtInsToUse = builtIns ?: object : KotlinBuiltIns(LockBasedStorageManager.NO_LOCKS) {}
            val moduleDescriptor = ModuleDescriptorImpl(
                moduleName,
                LockBasedStorageManager.NO_LOCKS,
                builtInsToUse,
                capabilities = mapOf(KlibModuleOrigin.CAPABILITY to moduleOrigin),
                platform = targetPlatform
            )

            if (builtIns == null) {
                builtInsToUse.builtInsModule = moduleDescriptor
            }

            dependencies += moduleDescriptor
            moduleDescriptor.setDependencies(dependencies.toList())

            return moduleDescriptor
        }

        val moduleDescriptors = mutableListOf<ModuleDescriptorImpl>()
        if (stdlib != null) {
            val stdlibModuleDescriptor = loadDescriptor(stdlib)
            moduleDescriptors += stdlibModuleDescriptor
            builtIns = stdlibModuleDescriptor.builtIns
        }

        libraries.forEach { library ->
            if (library == stdlib) return@forEach
            moduleDescriptors += loadDescriptor(library)
        }

        return moduleDescriptors to builtIns
    }
}

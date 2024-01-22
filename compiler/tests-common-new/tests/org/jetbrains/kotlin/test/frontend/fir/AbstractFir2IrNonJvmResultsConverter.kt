/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.frontend.fir.handlers.FirDiagnosticCollectorService
import org.jetbrains.kotlin.test.frontend.fir.handlers.firDiagnosticCollectorService
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

abstract class AbstractFir2IrNonJvmResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    protected abstract fun createIrMangler(): KotlinMangler.IrMangler
    protected abstract fun createFirMangler(): FirMangler
    protected abstract fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinResolvedLibrary>
    protected abstract val klibFactories: KlibMetadataFactories

    final override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(service(::FirDiagnosticCollectorService))

    final override fun transform(module: TestModule, inputArtifact: FirOutputArtifact): IrBackendInput? =
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

        val irMangler = createIrMangler()
        val diagnosticReporter = DiagnosticReporterFactory.createReporter()

        val libraries = resolveLibraries(module, compilerConfiguration)
        val (dependencies, builtIns) = loadResolvedLibraries(libraries, compilerConfiguration.languageVersionSettings, testServices)

        val fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(
            compilerConfiguration,
            diagnosticReporter,
        )
        val firResult = inputArtifact.toFirResult()
        val fir2irResult = firResult.convertToIrAndActualize(
            Fir2IrExtensions.Default,
            fir2IrConfiguration,
            module.irGenerationExtensions(testServices),
            irMangler,
            createFirMangler(),
            Fir2IrVisibilityConverter.Default,
            builtIns ?: DefaultBuiltIns.Instance, // TODO: consider passing externally,
            ::IrTypeSystemContextImpl
        ).also {
            (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = dependencies }
        }

        return createBackendInput(
            compilerConfiguration,
            diagnosticReporter,
            inputArtifact,
            fir2irResult,
            Fir2KlibMetadataSerializer(
                compilerConfiguration,
                firResult.outputs,
                fir2irResult,
                exportKDoc = false,
                produceHeaderKlib = false,
            ),
        )
    }

    protected abstract fun createBackendInput(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput

    private fun loadResolvedLibraries(
        resolvedLibraries: List<KotlinResolvedLibrary>,
        languageVersionSettings: LanguageVersionSettings,
        testServices: TestServices
    ): Pair<List<ModuleDescriptor>, KotlinBuiltIns?> {
        var builtInsModule: KotlinBuiltIns? = null
        val dependencies = mutableListOf<ModuleDescriptorImpl>()

        return resolvedLibraries.map { resolvedLibrary ->
            testServices.libraryProvider.getOrCreateStdlibByPath(resolvedLibrary.library.libraryFile.absolutePath) {
                // TODO: check safety of the approach of creating a separate storage manager per library
                val storageManager = LockBasedStorageManager("ModulesStructure")

                val moduleDescriptor = klibFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                    resolvedLibrary.library,
                    languageVersionSettings,
                    storageManager,
                    builtInsModule,
                    packageAccessHandler = null,
                    lookupTracker = LookupTracker.DO_NOTHING
                )
                dependencies += moduleDescriptor
                moduleDescriptor.setDependencies(ArrayList(dependencies))

                Pair(moduleDescriptor, resolvedLibrary.library)
            }.also {
                val isBuiltIns = resolvedLibrary.library.unresolvedDependencies.isEmpty()
                if (isBuiltIns) builtInsModule = it.builtIns
            }
        } to builtInsModule
    }
}

// TODO: move somewhere
fun TestModule.irGenerationExtensions(testServices: TestServices): Collection<IrGenerationExtension> {
    return IrGenerationExtension.getInstances(testServices.compilerConfigurationProvider.getProject(this))
}

fun FirOutputArtifact.toFirResult(): FirResult {
    val outputs = partsForDependsOnModules.map {
        ModuleCompilerAnalyzedOutput(it.session, it.firAnalyzerFacade.scopeSession, it.firFiles.values.toList())
    }
    return FirResult(outputs)
}

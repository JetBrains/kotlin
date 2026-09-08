/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.IrSpecialAnnotationsProvider
import org.jetbrains.kotlin.backend.common.actualizer.IrExtraActualDeclarationExtractor
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.DiagnosticsCollectorImpl
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrTypeSystemContext
import org.jetbrains.kotlin.ir.util.KotlinMangler
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.isAnyPlatformStdlib
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name.special
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
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

abstract class AbstractFir2IrResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    protected abstract fun createIrMangler(): KotlinMangler.IrMangler
    protected abstract fun createFir2IrExtensions(compilerConfiguration: CompilerConfiguration): Fir2IrExtensions
    protected abstract fun createFir2IrVisibilityConverter(): Fir2IrVisibilityConverter
    protected abstract fun createTypeSystemContextProvider(): (IrBuiltIns) -> IrTypeSystemContext
    protected abstract fun createSpecialAnnotationsProvider(): ((IrModuleFragment) -> IrSpecialAnnotationsProvider)?
    protected abstract fun createExtraActualDeclarationExtractorInitializer(): (Fir2IrComponents) -> List<IrExtraActualDeclarationExtractor>

    protected abstract fun resolveLibraries(module: TestModule, compilerConfiguration: CompilerConfiguration): List<KotlinLibrary>

    final override val additionalServices: List<ServiceRegistrationData>
        get() = listOf(
            service(::FirDiagnosticCollectorService),
        )

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
        val diagnosticReporter = DiagnosticsCollectorImpl()

        val fir2IrExtensions = createFir2IrExtensions(compilerConfiguration)

        val libraries: List<KotlinLibrary> = resolveLibraries(module, compilerConfiguration)
        val [dependencies: List<ModuleDescriptor>, builtIns: KotlinBuiltIns?] = loadModuleDescriptors(
            libraries,
            testServices.targetPlatformProvider.getTargetPlatform(module),
        )

        val fir2IrConfiguration = createFir2IrConfiguration(compilerConfiguration, diagnosticReporter)

        val firResult = inputArtifact.toFirResult()
        val fir2irResult = firResult.convertToIrAndActualize(
            fir2IrExtensions,
            fir2IrConfiguration,
            compilerConfiguration.getCompilerExtensions(IrGenerationExtension),
            irMangler,
            createFir2IrVisibilityConverter(),
            builtIns ?: DefaultBuiltIns.Instance, // TODO: consider passing externally,
            createTypeSystemContextProvider(),
            createSpecialAnnotationsProvider = createSpecialAnnotationsProvider(),
            extraActualDeclarationExtractorsInitializer = createExtraActualDeclarationExtractorInitializer(),
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

    protected abstract fun createFir2IrConfiguration(
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
    ): Fir2IrConfiguration

    protected abstract fun createBackendInput(
        module: TestModule,
        compilerConfiguration: CompilerConfiguration,
        diagnosticReporter: BaseDiagnosticsCollector,
        inputArtifact: FirOutputArtifact,
        fir2IrResult: Fir2IrActualizedResult,
        fir2KlibMetadataSerializer: Fir2KlibMetadataSerializer,
    ): IrBackendInput

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
                capabilities = mapOf(
                    KlibModuleOrigin.CAPABILITY to moduleOrigin,
                    @OptIn(K1Deprecation::class)
                    ImplicitIntegerCoercion.MODULE_CAPABILITY to moduleOrigin.isCInteropLibrary()
                ),
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

fun FirOutputArtifact.toFirResult(): AllModulesFrontendOutput {
    val outputs = partsForDependsOnModules.map {
        SingleModuleFrontendOutput(it.session, it.scopeSession, it.firFilesByTestFile.values.toList())
    }
    return AllModulesFrontendOutput(outputs)
}

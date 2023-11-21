/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.js.FirJsKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.pipeline.FirResult
import org.jetbrains.kotlin.fir.pipeline.ModuleCompilerAnalyzedOutput
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.libraryProvider
import org.jetbrains.kotlin.utils.metadataVersion

class Fir2IrJsResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {

    override fun transform(module: TestModule, inputArtifact: FirOutputArtifact): IrBackendInput? =
        try {
            transformInternal(module, inputArtifact)
        } catch (e: Throwable) {
            if (CodegenTestDirectives.IGNORE_FIR2IR_EXCEPTIONS_IF_FIR_CONTAINS_ERRORS in module.directives && inputArtifact.hasErrors) {
                null
            } else {
                throw e
            }
        }

    private fun transformInternal(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val configuration = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
        val sourceFiles = mutableListOf<KtSourceFile>()
        val firFilesAndComponentsBySourceFile = mutableMapOf<KtSourceFile, Pair<FirFile, Fir2IrComponents>>()

        val irMangler = JsManglerIr
        val diagnosticReporter = DiagnosticReporterFactory.createReporter()

        val libraries = resolveLibraries(configuration, getAllJsDependenciesPaths(module, testServices))
        val (dependencies, builtIns) = loadResolvedLibraries(libraries, configuration.languageVersionSettings, testServices)

        val fir2IrConfiguration = Fir2IrConfiguration(
            languageVersionSettings = configuration.languageVersionSettings,
            diagnosticReporter = diagnosticReporter,
            linkViaSignatures = true,
            evaluatedConstTracker = configuration
                .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
            inlineConstTracker = null,
            expectActualTracker = configuration[CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER],
            allowNonCachedDeclarations = false,
            useIrFakeOverrideBuilder = module.shouldUseIrFakeOverrideBuilderInConvertToIr()
        )

        val fir2irResult = inputArtifact.toFirResult().convertToIrAndActualize(
            Fir2IrExtensions.Default,
            fir2IrConfiguration,
            module.irGenerationExtensions(testServices),
            IdSignatureDescriptor(JsManglerDesc),
            JsManglerIr,
            FirJsKotlinMangler(),
            Fir2IrVisibilityConverter.Default,
            builtIns ?: DefaultBuiltIns.Instance, // TODO: consider passing externally,
            ::IrTypeSystemContextImpl
        ) { firPart, irPart ->
            sourceFiles.addAll(firPart.fir.mapNotNull { it.sourceFile })
            for (firFile in firPart.fir) {
                firFilesAndComponentsBySourceFile[firFile.sourceFile!!] = firFile to irPart.components
            }
        }.also {
            (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = dependencies }
        }

        val metadataVersion = configuration.metadataVersion(module.languageVersionSettings.languageVersion)

        return IrBackendInput.JsIrBackendInput(
            fir2irResult.irModuleFragment,
            fir2irResult.pluginContext,
            sourceFiles,
            configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList(),
            diagnosticReporter = diagnosticReporter,
            hasErrors = inputArtifact.hasErrors,
            descriptorMangler = fir2irResult.components.symbolTable.signaturer.mangler,
            irMangler = irMangler,
            firMangler = fir2irResult.components.signatureComposer.mangler,
        ) { file ->
            val (firFile, components) = firFilesAndComponentsBySourceFile[file]
                ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
            val actualizedExpectDeclarations = fir2irResult.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
            serializeSingleFirFile(
                firFile,
                components.session,
                components.scopeSession,
                actualizedExpectDeclarations,
                FirKLibSerializerExtension(
                    components.session, metadataVersion,
                    ConstValueProviderImpl(components),
                    allowErrorTypes = false, exportKDoc = false,
                    components.annotationsFromPluginRegistrar.createMetadataAnnotationsProvider()
                ),
                configuration.languageVersionSettings,
            )
        }
    }
}

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

            val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
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

fun TestModule.irGenerationExtensions(testServices: TestServices): Collection<IrGenerationExtension> {
    return IrGenerationExtension.getInstances(testServices.compilerConfigurationProvider.getProject(this))
}

fun FirOutputArtifact.toFirResult(): FirResult {
    val outputs = partsForDependsOnModules.map {
        ModuleCompilerAnalyzedOutput(it.session, it.firAnalyzerFacade.scopeSession, it.firFiles.values.toList())
    }
    return FirResult(outputs)
}

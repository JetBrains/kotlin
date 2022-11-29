/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.JsFactories
import org.jetbrains.kotlin.ir.backend.js.getSerializedData
import org.jetbrains.kotlin.ir.backend.js.incrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.backend.js.serializeSingleFirFile
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.BackendKinds
import org.jetbrains.kotlin.test.model.Frontend2BackendConverter
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.compilerConfigurationProvider
import org.jetbrains.kotlin.test.services.jsLibraryProvider

class Fir2IrJsResultsConverter(
    testServices: TestServices
) : Frontend2BackendConverter<FirOutputArtifact, IrBackendInput>(
    testServices,
    FrontendKinds.FIR,
    BackendKinds.IrBackend
) {
    override fun transform(
        module: TestModule,
        inputArtifact: FirOutputArtifact
    ): IrBackendInput {
        val compilerConfigurationProvider = testServices.compilerConfigurationProvider
        val configuration = compilerConfigurationProvider.getCompilerConfiguration(module)

        val fir2IrExtensions = Fir2IrExtensions.Default
        val firFiles = inputArtifact.allFirFiles.values
        val (irModuleFragment, components) =
            inputArtifact.firAnalyzerFacade.convertToJsIr(firFiles, fir2IrExtensions, module, configuration, testServices)

        val sourceFiles = firFiles.mapNotNull { it.sourceFile }
        val firFilesBySourceFile = firFiles.associateBy { it.sourceFile }

        val icData = configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val metadataVersion =
            configuration.get(CommonConfigurationKeys.METADATA_VERSION)
                ?: GenerationState.LANGUAGE_TO_METADATA_VERSION.getValue(module.languageVersionSettings.languageVersion)

        return IrBackendInput.JsIrBackendInput(
            irModuleFragment,
            sourceFiles,
            icData,
            expectDescriptorToSymbol,
            hasErrors = false // TODO: implement error check
        ) { file ->
            val firFile = firFilesBySourceFile[file] ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
            serializeSingleFirFile(firFile, components.session, components.scopeSession, metadataVersion)
        }
    }
}

fun AbstractFirAnalyzerFacade.convertToJsIr(
    firFiles: Collection<FirFile>,
    fir2IrExtensions: Fir2IrExtensions,
    module: TestModule,
    configuration: CompilerConfiguration,
    testServices: TestServices
): Fir2IrResult {
    this as FirAnalyzerFacade
    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val commonFirFiles = session.moduleData.dependsOnDependencies
        .map { it.session }
        .filter { it.kind == FirSession.Kind.Source }
        .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

    // TODO: consider avoiding repeated libraries resolution
    val libraries = resolveJsLibraries(module, testServices, configuration)
    val (dependencies, builtIns) = loadResolvedLibraries(libraries, configuration.languageVersionSettings, testServices)

    return Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
        session, scopeSession, firFiles + commonFirFiles,
        languageVersionSettings, signaturer,
        fir2IrExtensions,
        FirJvmKotlinMangler(session), // TODO: replace with potentially simpler JS version
        JsManglerIr, IrFactoryImpl,
        Fir2IrVisibilityConverter.Default,
        Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation
        irGeneratorExtensions,
        generateSignatures = false,
        kotlinBuiltIns = builtIns ?: DefaultBuiltIns.Instance // TODO: consider passing externally
    ).also {
        (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = dependencies }
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
        // resolvedLibrary.library.libraryName in fact resolves to (modified) file path, which is confising and maybe should be refactored
        testServices.jsLibraryProvider.getOrCreateStdlibByPath(resolvedLibrary.library.libraryName) {
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

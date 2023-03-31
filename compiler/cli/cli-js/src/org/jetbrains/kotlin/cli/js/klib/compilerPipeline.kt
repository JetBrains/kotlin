/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.klib

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.fileBelongsToModuleForPsi
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.isCommonSourceForPsi
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.prepareJsSessions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.extractFirDeclarations
import org.jetbrains.kotlin.fir.backend.js.FirJsKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.metadataVersion
import java.nio.file.Paths

fun compileModuleToAnalyzedFir(
    moduleStructure: ModulesStructure,
    ktFiles: List<KtFile>,
    libraries: List<String>,
    friendLibraries: List<String>,
    messageCollector: MessageCollector,
    diagnosticsReporter: BaseDiagnosticsCollector
): List<ModuleCompilerAnalyzedOutput>? {
    val renderDiagnosticNames = moduleStructure.compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

    // FIR
    val extensionRegistrars = FirExtensionRegistrar.getInstances(moduleStructure.project)

    val mainModuleName = moduleStructure.compilerConfiguration.get(CommonConfigurationKeys.MODULE_NAME)!!
    val escapedMainModuleName = Name.special("<$mainModuleName>")

    val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
        AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
    }

    val binaryModuleData = BinaryModuleData.initialize(escapedMainModuleName, JsPlatforms.defaultJsPlatform, JsPlatformAnalyzerServices)
    val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
        dependencies(libraries.map { Paths.get(it).toAbsolutePath() })
        friendDependencies(friendLibraries.map { Paths.get(it).toAbsolutePath() })
        // TODO: !!! dependencies module data?
    }

    val resolvedLibraries = moduleStructure.allDependencies

    val sessionsWithSources = prepareJsSessions(
        ktFiles, moduleStructure.compilerConfiguration, escapedMainModuleName,
        resolvedLibraries, dependencyList, extensionRegistrars, isCommonSourceForPsi, fileBelongsToModuleForPsi
    )

    val outputs = sessionsWithSources.map {
        buildResolveAndCheckFir(it.session, it.files, diagnosticsReporter)
    }

    if (syntaxErrors || diagnosticsReporter.hasErrors) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        return null
    }

    return outputs
}

fun transformFirToIr(
    moduleStructure: ModulesStructure,
    firOutputs: List<ModuleCompilerAnalyzedOutput>,
    diagnosticsReporter: PendingDiagnosticsCollectorWithSuppress,
): Fir2IrActualizedResult {
    val fir2IrExtensions = Fir2IrExtensions.Default

    var builtInsModule: KotlinBuiltIns? = null
    val dependencies = mutableListOf<ModuleDescriptorImpl>()

    val librariesDescriptors = moduleStructure.allDependencies.map { resolvedLibrary ->
        val storageManager = LockBasedStorageManager("ModulesStructure")

        val moduleDescriptor = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            resolvedLibrary,
            moduleStructure.compilerConfiguration.languageVersionSettings,
            storageManager,
            builtInsModule,
            packageAccessHandler = null,
            lookupTracker = LookupTracker.DO_NOTHING
        )
        dependencies += moduleDescriptor
        moduleDescriptor.setDependencies(ArrayList(dependencies))

        val isBuiltIns = resolvedLibrary.unresolvedDependencies.isEmpty()
        if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

        moduleDescriptor
    }

    val firResult = FirResult(firOutputs)
    return firResult.convertToIrAndActualize(
        fir2IrExtensions,
        IrGenerationExtension.getInstances(moduleStructure.project),
        linkViaSignatures = false,
        signatureComposerCreator = null,
        irMangler = JsManglerIr,
        firManglerCreator = ::FirJsKotlinMangler,
        visibilityConverter = Fir2IrVisibilityConverter.Default,
        kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
        diagnosticReporter = diagnosticsReporter,
        languageVersionSettings = moduleStructure.compilerConfiguration.languageVersionSettings,
        fir2IrResultPostCompute = {
            (this.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
        }
    )
}

fun serializeFirKlib(
    moduleStructure: ModulesStructure,
    firOutputs: List<ModuleCompilerAnalyzedOutput>,
    fir2IrActualizedResult: Fir2IrActualizedResult,
    outputKlibPath: String,
    messageCollector: MessageCollector,
    diagnosticsReporter: BaseDiagnosticsCollector,
    jsOutputName: String?
) {
    val sourceFiles = mutableListOf<KtSourceFile>()
    val firFilesAndSessionsBySourceFile = mutableMapOf<KtSourceFile, Triple<FirFile, FirSession, ScopeSession>>()

    for (output in firOutputs) {
        output.fir.forEach {
            sourceFiles.add(it.sourceFile!!)
            firFilesAndSessionsBySourceFile[it.sourceFile!!] = Triple(it, output.session, output.scopeSession)
        }
    }

    val icData = moduleStructure.compilerConfiguration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()

    val metadataVersion = moduleStructure.compilerConfiguration.metadataVersion()

    val actualizedExpectDeclarations = fir2IrActualizedResult.irActualizationResult.extractFirDeclarations()

    serializeModuleIntoKlib(
        moduleStructure.compilerConfiguration[CommonConfigurationKeys.MODULE_NAME]!!,
        moduleStructure.compilerConfiguration,
        moduleStructure.compilerConfiguration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
        sourceFiles,
        klibPath = outputKlibPath,
        moduleStructure.allDependencies,
        fir2IrActualizedResult.irModuleFragment,
        expectDescriptorToSymbol = mutableMapOf(),
        cleanFiles = icData,
        nopack = true,
        perFile = false,
        containsErrorCode = messageCollector.hasErrors() || diagnosticsReporter.hasErrors,
        abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
        jsOutputName = jsOutputName
    ) { file ->
        val (firFile, session, scopeSession) = firFilesAndSessionsBySourceFile[file]
            ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
        serializeSingleFirFile(
            firFile,
            session,
            scopeSession,
            actualizedExpectDeclarations,
            FirKLibSerializerExtension(
                session, metadataVersion,
                ConstValueProviderImpl(fir2IrActualizedResult.components),
                allowErrorTypes = false, exportKDoc = false
            ),
            moduleStructure.compilerConfiguration.languageVersionSettings,
        )
    }
}

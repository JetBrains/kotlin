/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js.klib

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.constant.EvaluatedConstTracker
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.js.FirJsKotlinMangler
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.fir.session.KlibIcData
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.metadataVersion
import java.io.File
import java.nio.file.Paths

inline fun <F> compileModuleToAnalyzedFir(
    moduleStructure: ModulesStructure,
    files: List<F>,
    libraries: List<String>,
    friendLibraries: List<String>,
    messageCollector: MessageCollector,
    diagnosticsReporter: BaseDiagnosticsCollector,
    incrementalDataProvider: IncrementalDataProvider?,
    lookupTracker: LookupTracker?,
    fileHasSyntaxErrors: (F) -> Boolean,
    noinline isCommonSource: (F) -> Boolean,
    noinline fileBelongsToModule: (F, String) -> Boolean,
    buildResolveAndCheckFir: (FirSession, List<F>) -> ModuleCompilerAnalyzedOutput,
): List<ModuleCompilerAnalyzedOutput>? {
    val renderDiagnosticNames = moduleStructure.compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

    // FIR
    val extensionRegistrars = FirExtensionRegistrar.getInstances(moduleStructure.project)

    val mainModuleName = moduleStructure.compilerConfiguration.get(CommonConfigurationKeys.MODULE_NAME)!!
    val escapedMainModuleName = Name.special("<$mainModuleName>")

    val syntaxErrors = files.fold(false) { errorsFound, file ->
        fileHasSyntaxErrors(file) or errorsFound
    }

    val binaryModuleData = BinaryModuleData.initialize(escapedMainModuleName, JsPlatforms.defaultJsPlatform, JsPlatformAnalyzerServices)
    val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
        dependencies(libraries.map { Paths.get(it).toAbsolutePath() })
        friendDependencies(friendLibraries.map { Paths.get(it).toAbsolutePath() })
        // TODO: !!! dependencies module data?
    }

    val resolvedLibraries = moduleStructure.allDependencies
    val sessionsWithSources = prepareJsSessions(
        files, moduleStructure.compilerConfiguration, escapedMainModuleName,
        resolvedLibraries, dependencyList, extensionRegistrars,
        isCommonSource = isCommonSource,
        fileBelongsToModule = fileBelongsToModule,
        lookupTracker,
        icData = incrementalDataProvider?.let(::KlibIcData),
    )

    val outputs = sessionsWithSources.map {
        buildResolveAndCheckFir(it.session, it.files)
    }

    if (syntaxErrors || diagnosticsReporter.hasErrors) {
        FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
        return null
    }

    return outputs
}

fun compileModuleToAnalyzedFirWithPsi(
    moduleStructure: ModulesStructure,
    ktFiles: List<KtFile>,
    libraries: List<String>,
    friendLibraries: List<String>,
    messageCollector: MessageCollector,
    diagnosticsReporter: BaseDiagnosticsCollector,
    incrementalDataProvider: IncrementalDataProvider?,
    lookupTracker: LookupTracker?,
): List<ModuleCompilerAnalyzedOutput>? {
    return compileModuleToAnalyzedFir(
        moduleStructure,
        ktFiles,
        libraries,
        friendLibraries,
        messageCollector,
        diagnosticsReporter,
        incrementalDataProvider,
        lookupTracker,
        fileHasSyntaxErrors = { AnalyzerWithCompilerReport.reportSyntaxErrors(it, messageCollector).isHasErrors },
        isCommonSource = isCommonSourceForPsi,
        fileBelongsToModule = fileBelongsToModuleForPsi,
        buildResolveAndCheckFir = { session, files ->
            buildResolveAndCheckFirFromKtFiles(session, files, diagnosticsReporter)
        },
    )
}

fun compileModulesToAnalyzedFirWithLightTree(
    moduleStructure: ModulesStructure,
    groupedSources: GroupedKtSources,
    ktSourceFiles: List<KtSourceFile>,
    libraries: List<String>,
    friendLibraries: List<String>,
    messageCollector: MessageCollector,
    diagnosticsReporter: BaseDiagnosticsCollector,
    incrementalDataProvider: IncrementalDataProvider?,
    lookupTracker: LookupTracker?,
): List<ModuleCompilerAnalyzedOutput>? {
    return compileModuleToAnalyzedFir(
        moduleStructure,
        ktSourceFiles,
        libraries,
        friendLibraries,
        messageCollector,
        diagnosticsReporter,
        incrementalDataProvider,
        lookupTracker,
        fileHasSyntaxErrors = { false },
        isCommonSource = { groupedSources.isCommonSourceForLt(it) },
        fileBelongsToModule = { file, it -> groupedSources.fileBelongsToModuleForLt(file, it) },
        buildResolveAndCheckFir = { session, files ->
            buildResolveAndCheckFirViaLightTree(session, files, diagnosticsReporter, null)
        },
    )
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
        Fir2IrConfiguration(
            languageVersionSettings = moduleStructure.compilerConfiguration.languageVersionSettings,
            linkViaSignatures = false,
            evaluatedConstTracker = moduleStructure.compilerConfiguration
                .putIfAbsent(CommonConfigurationKeys.EVALUATED_CONST_TRACKER, EvaluatedConstTracker.create()),
        ),
        IrGenerationExtension.getInstances(moduleStructure.project),
        signatureComposer = DescriptorSignatureComposerStub(JsManglerDesc),
        irMangler = JsManglerIr,
        firMangler = FirJsKotlinMangler(),
        visibilityConverter = Fir2IrVisibilityConverter.Default,
        kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
        diagnosticReporter = diagnosticsReporter,
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

    val actualizedExpectDeclarations = fir2IrActualizedResult.irActualizedResult.extractFirDeclarations()

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

fun shouldGoToNextIcRound(
    moduleStructure: ModulesStructure,
    firOutputs: List<ModuleCompilerAnalyzedOutput>,
    fir2IrActualizedResult: Fir2IrActualizedResult,
    config: CompilerConfiguration,
): Boolean {
    val sourceFiles = mutableListOf<KtSourceFile>()
    val firFilesAndSessionsBySourceFile = mutableMapOf<KtSourceFile, Triple<FirFile, FirSession, ScopeSession>>()

    for (output in firOutputs) {
        output.fir.forEach {
            sourceFiles.add(it.sourceFile!!)
            firFilesAndSessionsBySourceFile[it.sourceFile!!] = Triple(it, output.session, output.scopeSession)
        }
    }

    val metadataVersion = moduleStructure.compilerConfiguration.metadataVersion()

    val actualizedExpectDeclarations = fir2IrActualizedResult.irActualizedResult.extractFirDeclarations()

    val nextRoundChecker = config.get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return false

    for (ktFile in sourceFiles) {

        val (firFile, session, scopeSession) = firFilesAndSessionsBySourceFile[ktFile]
            ?: error("cannot find FIR file by source file ${ktFile.name} (${ktFile.path})")

        val packageFragment = serializeSingleFirFile(
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

        // to minimize a number of IC rounds, we should inspect all proto for changes first,
        // then go to a next round if needed, with all new dirty files
        nextRoundChecker.checkProtoChanges(File(ktFile.path!!), packageFragment.toByteArray())
    }

    return nextRoundChecker.shouldGoToNextRound()
}
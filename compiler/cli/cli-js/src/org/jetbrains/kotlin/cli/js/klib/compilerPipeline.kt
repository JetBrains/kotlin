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
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.impl.PendingDiagnosticsCollectorWithSuppress
import org.jetbrains.kotlin.fir.BinaryModuleData
import org.jetbrains.kotlin.fir.DependencyListForCliModule
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.js.FirJsKotlinMangler
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.pipeline.*
import org.jetbrains.kotlin.fir.session.KlibIcData
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.js.config.WasmTarget
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.wasm.resolve.WasmJsPlatformAnalyzerServices
import org.jetbrains.kotlin.wasm.resolve.WasmWasiPlatformAnalyzerServices
import java.nio.file.Paths

inline fun <F> compileModuleToAnalyzedFir(
    moduleStructure: ModulesStructure,
    files: List<F>,
    libraries: List<String>,
    friendLibraries: List<String>,
    incrementalDataProvider: IncrementalDataProvider?,
    lookupTracker: LookupTracker?,
    noinline isCommonSource: (F) -> Boolean,
    noinline fileBelongsToModule: (F, String) -> Boolean,
    buildResolveAndCheckFir: (FirSession, List<F>) -> ModuleCompilerAnalyzedOutput,
    useWasmPlatform: Boolean,
): List<ModuleCompilerAnalyzedOutput> {
    // FIR
    val extensionRegistrars = FirExtensionRegistrar.getInstances(moduleStructure.project)

    val mainModuleName = moduleStructure.compilerConfiguration.get(CommonConfigurationKeys.MODULE_NAME)!!
    val escapedMainModuleName = Name.special("<$mainModuleName>")
    val platform = if (useWasmPlatform) WasmPlatforms.Default else JsPlatforms.defaultJsPlatform
    val platformAnalyzerServices = if (useWasmPlatform) {
        when (moduleStructure.compilerConfiguration.get(JSConfigurationKeys.WASM_TARGET, WasmTarget.JS)) {
            WasmTarget.JS -> WasmJsPlatformAnalyzerServices
            WasmTarget.WASI -> WasmWasiPlatformAnalyzerServices
        }
    } else JsPlatformAnalyzerServices

    val binaryModuleData = BinaryModuleData.initialize(escapedMainModuleName, platform, platformAnalyzerServices)
    val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
        dependencies(libraries.map { Paths.get(it).toAbsolutePath() })
        friendDependencies(friendLibraries.map { Paths.get(it).toAbsolutePath() })
        // TODO: !!! dependencies module data?
    }

    val resolvedLibraries = moduleStructure.allDependencies

    val sessionsWithSources = if (useWasmPlatform) {
        prepareWasmSessions(
            files, moduleStructure.compilerConfiguration, escapedMainModuleName,
            resolvedLibraries, dependencyList, extensionRegistrars,
            isCommonSource = isCommonSource,
            fileBelongsToModule = fileBelongsToModule,
            lookupTracker,
            icData = incrementalDataProvider?.let(::KlibIcData),
        )
    } else {
        prepareJsSessions(
            files, moduleStructure.compilerConfiguration, escapedMainModuleName,
            resolvedLibraries, dependencyList, extensionRegistrars,
            isCommonSource = isCommonSource,
            fileBelongsToModule = fileBelongsToModule,
            lookupTracker,
            icData = incrementalDataProvider?.let(::KlibIcData),
        )
    }

    val outputs = sessionsWithSources.map {
        buildResolveAndCheckFir(it.session, it.files)
    }

    return outputs
}

internal fun reportCollectedDiagnostics(
    compilerConfiguration: CompilerConfiguration,
    diagnosticsReporter: BaseDiagnosticsCollector,
    messageCollector: MessageCollector
) {
    val renderName = compilerConfiguration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderName)
}

open class AnalyzedFirOutput(val output: List<ModuleCompilerAnalyzedOutput>) {
    protected open fun checkSyntaxErrors(messageCollector: MessageCollector) = false

    fun reportCompilationErrors(
        moduleStructure: ModulesStructure,
        diagnosticsReporter: BaseDiagnosticsCollector,
        messageCollector: MessageCollector,
    ): Boolean {
        if (checkSyntaxErrors(messageCollector) || diagnosticsReporter.hasErrors) {
            reportCollectedDiagnostics(moduleStructure.compilerConfiguration, diagnosticsReporter, messageCollector)
            return true
        }

        return false
    }
}

class AnalyzedFirWithPsiOutput(
    output: List<ModuleCompilerAnalyzedOutput>,
    private val compiledFiles: List<KtFile>
) : AnalyzedFirOutput(output) {
    override fun checkSyntaxErrors(messageCollector: MessageCollector): Boolean {
        return compiledFiles.fold(false) { errorsFound, file ->
            AnalyzerWithCompilerReport.reportSyntaxErrors(file, messageCollector).isHasErrors or errorsFound
        }
    }
}

fun compileModuleToAnalyzedFirWithPsi(
    moduleStructure: ModulesStructure,
    ktFiles: List<KtFile>,
    libraries: List<String>,
    friendLibraries: List<String>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    incrementalDataProvider: IncrementalDataProvider?,
    lookupTracker: LookupTracker?,
    useWasmPlatform: Boolean,
): AnalyzedFirWithPsiOutput {
    val output = compileModuleToAnalyzedFir(
        moduleStructure,
        ktFiles,
        libraries,
        friendLibraries,
        incrementalDataProvider,
        lookupTracker,
        isCommonSource = isCommonSourceForPsi,
        fileBelongsToModule = fileBelongsToModuleForPsi,
        buildResolveAndCheckFir = { session, files ->
            buildResolveAndCheckFirFromKtFiles(session, files, diagnosticsReporter)
        },
        useWasmPlatform = useWasmPlatform,
    )
    output.runPlatformCheckers(diagnosticsReporter)
    return AnalyzedFirWithPsiOutput(output, ktFiles)
}

fun compileModulesToAnalyzedFirWithLightTree(
    moduleStructure: ModulesStructure,
    groupedSources: GroupedKtSources,
    ktSourceFiles: List<KtSourceFile>,
    libraries: List<String>,
    friendLibraries: List<String>,
    diagnosticsReporter: BaseDiagnosticsCollector,
    incrementalDataProvider: IncrementalDataProvider?,
    lookupTracker: LookupTracker?,
    useWasmPlatform: Boolean,
): AnalyzedFirOutput {
    val output = compileModuleToAnalyzedFir(
        moduleStructure,
        ktSourceFiles,
        libraries,
        friendLibraries,
        incrementalDataProvider,
        lookupTracker,
        isCommonSource = { groupedSources.isCommonSourceForLt(it) },
        fileBelongsToModule = { file, it -> groupedSources.fileBelongsToModuleForLt(file, it) },
        buildResolveAndCheckFir = { session, files ->
            buildResolveAndCheckFirViaLightTree(session, files, diagnosticsReporter, null)
        },
        useWasmPlatform = useWasmPlatform,
    )
    output.runPlatformCheckers(diagnosticsReporter)
    return AnalyzedFirOutput(output)
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
        Fir2IrConfiguration.forKlibCompilation(moduleStructure.compilerConfiguration, diagnosticsReporter),
        IrGenerationExtension.getInstances(moduleStructure.project),
        irMangler = JsManglerIr,
        firMangler = FirJsKotlinMangler(),
        visibilityConverter = Fir2IrVisibilityConverter.Default,
        kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance,
        actualizerTypeContextProvider = ::IrTypeSystemContextImpl
    ) { _, irPart ->
        (irPart.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
    }
}

fun serializeFirKlib(
    moduleStructure: ModulesStructure,
    firOutputs: List<ModuleCompilerAnalyzedOutput>,
    fir2IrActualizedResult: Fir2IrActualizedResult,
    outputKlibPath: String,
    nopack: Boolean,
    messageCollector: MessageCollector,
    diagnosticsReporter: BaseDiagnosticsCollector,
    jsOutputName: String?,
    useWasmPlatform: Boolean,
) {
    val fir2KlibMetadataSerializer = Fir2KlibMetadataSerializer(
        moduleStructure.compilerConfiguration,
        firOutputs,
        fir2IrActualizedResult,
        exportKDoc = false,
        produceHeaderKlib = false,
    )
    val icData = moduleStructure.compilerConfiguration.incrementalDataProvider?.getSerializedData(fir2KlibMetadataSerializer.sourceFiles)

    serializeModuleIntoKlib(
        moduleStructure.compilerConfiguration[CommonConfigurationKeys.MODULE_NAME]!!,
        moduleStructure.compilerConfiguration,
        diagnosticsReporter,
        fir2KlibMetadataSerializer,
        klibPath = outputKlibPath,
        moduleStructure.allDependencies,
        fir2IrActualizedResult.irModuleFragment,
        cleanFiles = icData ?: emptyList(),
        nopack = nopack,
        perFile = false,
        containsErrorCode = messageCollector.hasErrors() || diagnosticsReporter.hasErrors,
        abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
        jsOutputName = jsOutputName,
        builtInsPlatform = if (useWasmPlatform) BuiltInsPlatform.WASM else BuiltInsPlatform.JS,
    )
}

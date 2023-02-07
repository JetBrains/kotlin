/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.js

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.backend.common.CompilationException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.wasm.compileToLoweredIr
import org.jetbrains.kotlin.backend.wasm.compileWasm
import org.jetbrains.kotlin.backend.wasm.dce.eliminateDeadDeclarations
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.backend.wasm.writeCompilationResult
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.ExitCode.*
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_EXCEPTION
import org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.RUNTIME_DIAGNOSTIC_LOG
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.cli.js.klib.generateIrForKlibSerialization
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrCommonMemberStorage
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.checkers.registerExtendedCommonCheckers
import org.jetbrains.kotlin.fir.descriptors.FirModuleDescriptor
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.pipeline.buildFirFromKtFiles
import org.jetbrains.kotlin.fir.pipeline.runCheckers
import org.jetbrains.kotlin.fir.pipeline.runResolution
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.serialization.FirElementAwareSerializableStringTable
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.fir.session.FirJsSessionFactory
import org.jetbrains.kotlin.fir.session.FirSessionConfigurator
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.incremental.web.IncrementalNextRoundChecker
import org.jetbrains.kotlin.incremental.web.IncrementalResultsConsumer
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.codegen.JsGenerationGranularity
import org.jetbrains.kotlin.ir.backend.js.ic.CacheUpdater
import org.jetbrains.kotlin.ir.backend.js.ic.DirtyFileState
import org.jetbrains.kotlin.ir.backend.js.ic.JsExecutableProducer
import org.jetbrains.kotlin.ir.backend.js.ic.ModuleArtifact
import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.WebManglerIr
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.CompilationOutputsBuilt
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.JsCodeGenerator
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.TranslationMode
import org.jetbrains.kotlin.ir.backend.web.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImplForJsIC
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.js.analyzer.WebAnalysisResult
import org.jetbrains.kotlin.js.config.*
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.library.unresolvedDependencies
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.KotlinPaths
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.join
import org.jetbrains.kotlin.utils.metadataVersion
import org.jetbrains.kotlin.web.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
import java.io.File
import java.io.IOException
import java.nio.file.Paths

private val K2JSCompilerArguments.granularity: JsGenerationGranularity
    get() = when {
        this.irPerModule -> JsGenerationGranularity.PER_MODULE
        this.irPerFile -> JsGenerationGranularity.PER_FILE
        else -> JsGenerationGranularity.WHOLE_PROGRAM
    }

class K2JsIrCompiler : CLICompiler<K2JSCompilerArguments>() {

    override val defaultPerformanceManager: CommonCompilerPerformanceManager =
        object : CommonCompilerPerformanceManager("Kotlin to JS (IR) Compiler") {}

    override fun createArguments(): K2JSCompilerArguments {
        return K2JSCompilerArguments()
    }

    private class Ir2JsTransformer(
        val arguments: K2JSCompilerArguments,
        val module: ModulesStructure,
        val phaseConfig: PhaseConfig,
        val messageCollector: MessageCollector,
        val mainCallArguments: List<String>?
    ) {
        private fun lowerIr(): LoweredIr {
            return compile(
                module,
                phaseConfig,
                IrFactoryImplForJsIC(WholeWorldStageController()),
                keep = arguments.irKeep?.split(",")
                    ?.filterNot { it.isEmpty() }
                    ?.toSet()
                    ?: emptySet(),
                dceRuntimeDiagnostic = RuntimeDiagnostic.resolve(
                    arguments.irDceRuntimeDiagnostic,
                    messageCollector
                ),
                safeExternalBoolean = arguments.irSafeExternalBoolean,
                safeExternalBooleanDiagnostic = RuntimeDiagnostic.resolve(
                    arguments.irSafeExternalBooleanDiagnostic,
                    messageCollector
                ),
                granularity = arguments.granularity,
                es6mode = arguments.useEsClasses
            )
        }

        private fun makeJsCodeGenerator(): JsCodeGenerator {
            val ir = lowerIr()
            val transformer = IrModuleToJsTransformer(ir.context, mainCallArguments, ir.moduleFragmentToUniqueName)

            val mode = TranslationMode.fromFlags(arguments.irDce, arguments.irPerModule, arguments.irMinimizedMemberNames)
            return transformer.makeJsCodeGenerator(ir.allModules, mode)
        }

        fun compileAndTransformIrNew(): CompilationOutputsBuilt {
            return makeJsCodeGenerator().generateJsCode(relativeRequirePath = true, outJsProgram = false)
        }
    }


    override fun doExecute(
        arguments: K2JSCompilerArguments,
        configuration: CompilerConfiguration,
        rootDisposable: Disposable,
        paths: KotlinPaths?
    ): ExitCode {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        val pluginLoadResult = loadPlugins(paths, arguments, configuration)
        if (pluginLoadResult != OK) return pluginLoadResult

        if (arguments.script) {
            messageCollector.report(ERROR, "K/JS does not support Kotlin script (*.kts) files")
            return COMPILATION_ERROR
        }

        if (arguments.freeArgs.isEmpty() && !(incrementalCompilationIsEnabledForJs(arguments))) {
            if (arguments.version) {
                return OK
            }
            if (arguments.includes.isNullOrEmpty()) {
                messageCollector.report(ERROR, "Specify at least one source file or directory", null)
                return COMPILATION_ERROR
            }
        }

        val libraries: List<String> = configureLibraries(arguments.libraries) + listOfNotNull(arguments.includes)
        val friendLibraries: List<String> = configureLibraries(arguments.friendModules)

        configuration.put(JSConfigurationKeys.LIBRARIES, libraries)
        configuration.put(JSConfigurationKeys.TRANSITIVE_LIBRARIES, libraries)

        configuration.put(WebConfigurationKeys.PARTIAL_LINKAGE, arguments.partialLinkage)

        configuration.put(JSConfigurationKeys.WASM_ENABLE_ARRAY_RANGE_CHECKS, arguments.wasmEnableArrayRangeChecks)
        configuration.put(JSConfigurationKeys.WASM_ENABLE_ASSERTS, arguments.wasmEnableAsserts)
        configuration.put(JSConfigurationKeys.WASM_GENERATE_WAT, arguments.wasmGenerateWat)

        val commonSourcesArray = arguments.commonSources
        val commonSources = commonSourcesArray?.toSet() ?: emptySet()
        for (arg in arguments.freeArgs) {
            configuration.addKotlinSourceRoot(arg, commonSources.contains(arg))
        }

        arguments.relativePathBases?.let {
            configuration.put(CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES, it.toList())
        }

        configuration.put(CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH, arguments.normalizeAbsolutePath)
        configuration.put(CommonConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS, arguments.enableSignatureClashChecks)

        // ----

        val environmentForJS =
            KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JS_CONFIG_FILES)
        val projectJs = environmentForJS.project
        val configurationJs = environmentForJS.configuration
        val sourcesFiles = environmentForJS.getSourceFiles()

        configurationJs.put(CLIConfigurationKeys.ALLOW_KOTLIN_PACKAGE, arguments.allowKotlinPackage)
        configurationJs.put(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME, arguments.renderInternalDiagnosticNames)
        configurationJs.put(WebConfigurationKeys.PROPERTY_LAZY_INITIALIZATION, arguments.irPropertyLazyInitialization)
        configurationJs.put(JSConfigurationKeys.GENERATE_POLYFILLS, arguments.generatePolyfills)
        configurationJs.put(JSConfigurationKeys.GENERATE_DTS, arguments.generateDts)
        configurationJs.put(JSConfigurationKeys.GENERATE_INLINE_ANONYMOUS_FUNCTIONS, arguments.irGenerateInlineAnonymousFunctions)

        if (!checkKotlinPackageUsage(environmentForJS.configuration, sourcesFiles)) return COMPILATION_ERROR

        val outputDirPath = arguments.outputDir
        val outputName = arguments.moduleName
        if (outputDirPath == null) {
            messageCollector.report(ERROR, "IR: Specify output dir via -ir-output-dir", null)
            return COMPILATION_ERROR
        }

        if (outputName == null) {
            messageCollector.report(ERROR, "IR: Specify output name via -ir-output-name", null)
            return COMPILATION_ERROR
        }

        if (messageCollector.hasErrors()) {
            return COMPILATION_ERROR
        }

        if (sourcesFiles.isEmpty() && (!incrementalCompilationIsEnabledForJs(arguments)) && arguments.includes.isNullOrEmpty()) {
            messageCollector.report(ERROR, "No source files", null)
            return COMPILATION_ERROR
        }

        if (arguments.verbose) {
            reportCompiledSourcesList(messageCollector, sourcesFiles)
        }

        val moduleName = arguments.irModuleName ?: outputName
        configurationJs.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

        val outputDir = File(outputDirPath)

        try {
            configurationJs.put(JSConfigurationKeys.OUTPUT_DIR, outputDir.canonicalFile)
        } catch (e: IOException) {
            messageCollector.report(ERROR, "Could not resolve output directory", null)
            return COMPILATION_ERROR
        }

        // TODO: Handle non-empty main call arguments
        val mainCallArguments = if (K2JsArgumentConstants.NO_CALL == arguments.main) null else emptyList<String>()

        val icCaches = prepareIcCaches(
            arguments = arguments,
            messageCollector = messageCollector,
            outputDir = outputDir,
            libraries = libraries,
            friendLibraries = friendLibraries,
            configurationJs = configurationJs,
            mainCallArguments = mainCallArguments
        )

        // Run analysis if main module is sources
        var sourceModule: ModulesStructure? = null
        val includes = arguments.includes
        if (includes == null) {
            val outputKlibPath =
                if (arguments.irProduceKlibFile) outputDir.resolve("$outputName.klib").normalize().absolutePath
                else outputDirPath
            if (configuration.get(CommonConfigurationKeys.USE_FIR) == true) {
                sourceModule = processSourceModuleWithK2(environmentForJS, libraries, friendLibraries, arguments, outputKlibPath)
            } else {
                sourceModule = processSourceModule(environmentForJS, libraries, friendLibraries, arguments, outputKlibPath)

                if (!sourceModule.webFrontEndResult.analysisResult.shouldGenerateCode)
                    return OK
            }

        }

        if (arguments.irProduceJs) {
            val moduleKind = configurationJs[JSConfigurationKeys.MODULE_KIND] ?: error("cannot get 'module kind' from configuration")

            messageCollector.report(INFO, "Produce executable: $outputDirPath")
            messageCollector.report(INFO, "Cache directory: ${arguments.cacheDirectory}")

            if (icCaches.isNotEmpty()) {
                val beforeIc2Js = System.currentTimeMillis()

                val jsExecutableProducer = JsExecutableProducer(
                    mainModuleName = moduleName,
                    moduleKind = moduleKind,
                    sourceMapsInfo = SourceMapsInfo.from(configurationJs),
                    caches = icCaches,
                    relativeRequirePath = true
                )

                val (outputs, rebuiltModules) = jsExecutableProducer.buildExecutable(arguments.irPerModule, outJsProgram = false)
                outputs.writeAll(outputDir, outputName, arguments.generateDts, moduleName, moduleKind)

                messageCollector.report(INFO, "Executable production duration (IC): ${System.currentTimeMillis() - beforeIc2Js}ms")
                for ((event, duration) in jsExecutableProducer.getStopwatchLaps()) {
                    messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
                }

                for (module in rebuiltModules) {
                    messageCollector.report(INFO, "IC module builder rebuilt JS for module [${File(module).name}]")
                }

                return OK
            }

            val phaseConfig = createPhaseConfig(jsPhases, arguments, messageCollector)

            val module = if (includes != null) {
                if (sourcesFiles.isNotEmpty()) {
                    messageCollector.report(ERROR, "Source files are not supported when -Xinclude is present")
                }
                val includesPath = File(includes).canonicalPath
                val mainLibPath = libraries.find { File(it).canonicalPath == includesPath }
                    ?: error("No library with name $includes ($includesPath) found")
                val kLib = MainModule.Klib(mainLibPath)
                ModulesStructure(
                    projectJs,
                    kLib,
                    configurationJs,
                    libraries,
                    friendLibraries
                )
            } else {
                sourceModule!!
            }

            if (arguments.wasm) {
                val (allModules, backendContext) = compileToLoweredIr(
                    depsDescriptors = module,
                    phaseConfig = PhaseConfig(wasmPhases),
                    irFactory = IrFactoryImpl,
                    exportedDeclarations = setOf(FqName("main")),
                    propertyLazyInitialization = arguments.irPropertyLazyInitialization,
                )
                if (arguments.irDce) {
                    eliminateDeadDeclarations(allModules, backendContext)
                }

                val generateSourceMaps = configuration.getBoolean(JSConfigurationKeys.SOURCE_MAP)

                val res = compileWasm(
                    allModules = allModules,
                    backendContext = backendContext,
                    baseFileName = outputName,
                    emitNameSection = arguments.wasmDebug,
                    allowIncompleteImplementations = arguments.irDce,
                    generateWat = configuration.get(JSConfigurationKeys.WASM_GENERATE_WAT, false),
                    generateSourceMaps = generateSourceMaps
                )

                writeCompilationResult(
                    result = res,
                    dir = outputDir,
                    fileNameBase = outputName,
                )

                return OK
            }

            val start = System.currentTimeMillis()

            try {
                val ir2JsTransformer = Ir2JsTransformer(arguments, module, phaseConfig, messageCollector, mainCallArguments)
                val outputs = ir2JsTransformer.compileAndTransformIrNew()

                messageCollector.report(INFO, "Executable production duration: ${System.currentTimeMillis() - start}ms")

                outputs.writeAll(outputDir, outputName, arguments.generateDts, moduleName, moduleKind)
            } catch (e: CompilationException) {
                messageCollector.report(
                    ERROR,
                    e.stackTraceToString(),
                    CompilerMessageLocation.create(
                        path = e.path,
                        line = e.line,
                        column = e.column,
                        lineContent = e.content
                    )
                )
                return INTERNAL_ERROR
            }
        }

        return OK
    }

    private fun processSourceModule(
        environmentForJS: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2JSCompilerArguments,
        outputKlibPath: String
    ): ModulesStructure {
        lateinit var sourceModule: ModulesStructure
        do {
            sourceModule = prepareAnalyzedSourceModule(
                environmentForJS.project,
                environmentForJS.getSourceFiles(),
                environmentForJS.configuration,
                libraries,
                friendLibraries,
                AnalyzerWithCompilerReport(environmentForJS.configuration)
            )
            val result = sourceModule.webFrontEndResult.analysisResult
            if (result is WebAnalysisResult.RetryWithAdditionalRoots) {
                environmentForJS.addKotlinSourceRoots(result.additionalKotlinRoots)
            }
        } while (result is WebAnalysisResult.RetryWithAdditionalRoots)

        if (sourceModule.webFrontEndResult.analysisResult.shouldGenerateCode && (arguments.irProduceKlibDir || arguments.irProduceKlibFile)) {
            val moduleSourceFiles = (sourceModule.mainModule as MainModule.SourceFiles).files
            val icData = environmentForJS.configuration.incrementalDataProvider?.getSerializedData(moduleSourceFiles) ?: emptyList()
            val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

            val (moduleFragment, _) = generateIrForKlibSerialization(
                environmentForJS.project,
                moduleSourceFiles,
                environmentForJS.configuration,
                sourceModule.webFrontEndResult.analysisResult,
                sourceModule.allDependencies,
                icData,
                expectDescriptorToSymbol,
                IrFactoryImpl,
                verifySignatures = true
            ) {
                sourceModule.getModuleDescriptor(it)
            }

            val metadataSerializer =
                KlibMetadataIncrementalSerializer(environmentForJS.configuration, sourceModule.project, sourceModule.webFrontEndResult.hasErrors)

            generateKLib(
                sourceModule,
                outputKlibPath,
                nopack = arguments.irProduceKlibDir,
                jsOutputName = arguments.irPerModuleOutputName,
                icData = icData,
                expectDescriptorToSymbol = expectDescriptorToSymbol,
                moduleFragment = moduleFragment
            ) { file ->
                metadataSerializer.serializeScope(file, sourceModule.webFrontEndResult.bindingContext, moduleFragment.descriptor)
            }
        }
        return sourceModule
    }

    private fun processSourceModuleWithK2(
        environmentForJS: KotlinCoreEnvironment,
        libraries: List<String>,
        friendLibraries: List<String>,
        arguments: K2JSCompilerArguments,
        outputKlibPath: String
    ): ModulesStructure? {
        val configuration = environmentForJS.configuration
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()
        val renderDiagnosticNames = configuration.getBoolean(CLIConfigurationKeys.RENDER_DIAGNOSTIC_INTERNAL_NAME)

        // FIR

        val sessionProvider = FirProjectSessionProvider()
        val extensionRegistrars = FirExtensionRegistrar.getInstances(environmentForJS.project)
        val sessionConfigurator: FirSessionConfigurator.() -> Unit = {
            if (arguments.extendedCompilerChecks) {
                registerExtendedCommonCheckers()
            }
        }

        val mainModuleName = Name.special("<${configuration.get(CommonConfigurationKeys.MODULE_NAME)!!}>")

        val ktFiles = environmentForJS.getSourceFiles()
        val syntaxErrors = ktFiles.fold(false) { errorsFound, ktFile ->
            AnalyzerWithCompilerReport.reportSyntaxErrors(ktFile, messageCollector).isHasErrors or errorsFound
        }

        val mainModule = MainModule.SourceFiles(environmentForJS.getSourceFiles())

        val binaryModuleData = BinaryModuleData.initialize(mainModuleName, JsPlatforms.defaultJsPlatform, JsPlatformAnalyzerServices)
        val dependencyList = DependencyListForCliModule.build(binaryModuleData) {
            dependencies(libraries.map { Paths.get(it).toAbsolutePath() })
            friendDependencies(friendLibraries.map { Paths.get(it).toAbsolutePath() })
            // TODO: !!! dependencies module data?
        }

        val logger = configuration.resolverLogger
        val resolvedLibraries = webResolveLibraries(libraries + friendLibraries, logger).getFullResolvedList()

        FirJsSessionFactory.createJsLibrarySession(
            mainModuleName,
            resolvedLibraries,
            sessionProvider,
            dependencyList.moduleDataProvider,
            configuration.languageVersionSettings,
            registerExtraComponents = {},
        )

        val mainModuleData = FirModuleDataImpl(
            mainModuleName,
            dependencyList.regularDependencies,
            dependencyList.dependsOnDependencies,
            dependencyList.friendsDependencies,
            JsPlatforms.defaultJsPlatform,
            JsPlatformAnalyzerServices
        )

        val session = FirJsSessionFactory.createJsModuleBasedSession(
            mainModuleData,
            sessionProvider,
            extensionRegistrars,
            configuration.languageVersionSettings,
            null,
            registerExtraComponents = {},
            init = sessionConfigurator,
        )

        val rawFirFiles = session.buildFirFromKtFiles(ktFiles)
        val (scopeSession, firFiles) = session.runResolution(rawFirFiles)
        session.runCheckers(scopeSession, firFiles, diagnosticsReporter)

        if (syntaxErrors || diagnosticsReporter.hasErrors) {
            FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)
            return null
        }

        // FIR2IR

        val fir2IrExtensions = Fir2IrExtensions.Default
        val commonFirFiles = session.moduleData.dependsOnDependencies
            .map { it.session }
            .filter { it.kind == FirSession.Kind.Source }
            .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

        var builtInsModule: KotlinBuiltIns? = null
        val dependencies = mutableListOf<ModuleDescriptorImpl>()

        val librariesDescriptors = resolvedLibraries.map { resolvedLibrary ->
            val storageManager = LockBasedStorageManager("ModulesStructure")

            val moduleDescriptor = WebFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
                resolvedLibrary.library,
                configuration.languageVersionSettings,
                storageManager,
                builtInsModule,
                packageAccessHandler = null,
                lookupTracker = LookupTracker.DO_NOTHING
            )
            dependencies += moduleDescriptor
            moduleDescriptor.setDependencies(ArrayList(dependencies))

            val isBuiltIns = resolvedLibrary.library.unresolvedDependencies.isEmpty()
            if (isBuiltIns) builtInsModule = moduleDescriptor.builtIns

            moduleDescriptor
        }

        val commonMemberStorage = Fir2IrCommonMemberStorage(
            generateSignatures = false,
            signatureComposerCreator = null,
            manglerCreator = { FirJvmKotlinMangler() } // TODO: replace with potentially simpler JS version
        )

        val fir2irResult = Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            session, scopeSession, firFiles + commonFirFiles,
            configuration.languageVersionSettings,
            fir2IrExtensions,
            WebManglerIr, IrFactoryImpl,
            Fir2IrVisibilityConverter.Default,
            Fir2IrJvmSpecialAnnotationSymbolProvider(), // TODO: replace with appropriate (probably empty) implementation
            IrGenerationExtension.getInstances(environmentForJS.project),
            generateSignatures = false,
            kotlinBuiltIns = builtInsModule ?: DefaultBuiltIns.Instance, // TODO: consider passing externally
            commonMemberStorage = commonMemberStorage,
            initializedIrBuiltIns = null
        ).also {
            (it.irModuleFragment.descriptor as? FirModuleDescriptor)?.let { it.allDependencyModules = librariesDescriptors }
        }

        // Serialize klib

        if (/*sourceModule.jsFrontEndResult.jsAnalysisResult.shouldGenerateCode && */(arguments.irProduceKlibDir || arguments.irProduceKlibFile)) {
            val sourceFiles = firFiles.mapNotNull { it.sourceFile }
            val firFilesBySourceFile = firFiles.associateBy { it.sourceFile }

            val icData = environmentForJS.configuration.incrementalDataProvider?.getSerializedData(sourceFiles) ?: emptyList()
            // TODO: expect -> actual mapping
            val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

            val metadataVersion = configuration.metadataVersion()

            serializeModuleIntoKlib(
                configuration[CommonConfigurationKeys.MODULE_NAME]!!,
                configuration,
                configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None,
                sourceFiles,
                klibPath = outputKlibPath,
                resolvedLibraries.map { it.library },
                fir2irResult.irModuleFragment,
                expectDescriptorToSymbol,
                cleanFiles = icData,
                nopack = true,
                perFile = false,
                containsErrorCode = messageCollector.hasErrors() || diagnosticsReporter.hasErrors,
                abiVersion = KotlinAbiVersion.CURRENT, // TODO get from test file data
                jsOutputName = null
            ) { file ->
                val firFile = firFilesBySourceFile[file] ?: error("cannot find FIR file by source file ${file.name} (${file.path})")
                serializeSingleFirFile(
                    firFile,
                    session,
                    scopeSession,
                    FirKLibSerializerExtension(session, metadataVersion, FirElementAwareSerializableStringTable()),
                    configuration.languageVersionSettings,
                )
            }
        }

        return ModulesStructure(
            environmentForJS.project, mainModule, configuration, libraries, friendLibraries
        )
    }

    private fun prepareIcCaches(
        arguments: K2JSCompilerArguments,
        messageCollector: MessageCollector,
        outputDir: File,
        libraries: List<String>,
        friendLibraries: List<String>,
        configurationJs: CompilerConfiguration,
        mainCallArguments: List<String>?
    ): List<ModuleArtifact> {
        val cacheDirectory = arguments.cacheDirectory

        // TODO: Use JS IR IC infrastructure for WASM?
        val icCaches = if (!arguments.wasm && cacheDirectory != null) {
            messageCollector.report(INFO, "")
            messageCollector.report(INFO, "Building cache:")
            messageCollector.report(INFO, "to: $outputDir")
            messageCollector.report(INFO, "cache directory: $cacheDirectory")
            messageCollector.report(INFO, libraries.toString())

            val start = System.currentTimeMillis()

            val cacheUpdater = CacheUpdater(
                mainModule = arguments.includes!!,
                allModules = libraries,
                mainModuleFriends = friendLibraries,
                cacheDir = cacheDirectory,
                compilerConfiguration = configurationJs,
                irFactory = { IrFactoryImplForJsIC(WholeWorldStageController()) },
                mainArguments = mainCallArguments,
                compilerInterfaceFactory = { mainModule, cfg ->
                    JsIrCompilerWithIC(
                        mainModule,
                        cfg,
                        arguments.granularity,
                        PhaseConfig(jsPhases),
                        es6mode = arguments.useEsClasses
                    )
                }
            )

            val artifacts = cacheUpdater.actualizeCaches()
            messageCollector.report(INFO, "IC rebuilt overall time: ${System.currentTimeMillis() - start}ms")
            for ((event, duration) in cacheUpdater.getStopwatchLastLaps()) {
                messageCollector.report(INFO, "  $event: ${(duration / 1e6).toInt()}ms")
            }

            var libIndex = 0
            for ((libFile, srcFiles) in cacheUpdater.getDirtyFileLastStats()) {
                val singleState = srcFiles.values.firstOrNull()?.singleOrNull()?.let { singleState ->
                    singleState.takeIf { srcFiles.values.all { it.singleOrNull() == singleState } }
                }

                val (msg, showFiles) = when {
                    singleState == DirtyFileState.NON_MODIFIED_IR -> continue
                    singleState == DirtyFileState.REMOVED_FILE -> "removed" to emptyMap()
                    singleState == DirtyFileState.ADDED_FILE -> "built clean" to emptyMap()
                    srcFiles.values.any { it.singleOrNull() == DirtyFileState.NON_MODIFIED_IR } -> "partially rebuilt" to srcFiles
                    else -> "fully rebuilt" to srcFiles
                }
                messageCollector.report(INFO, "${++libIndex}) module [${File(libFile.path).name}] was $msg")
                var fileIndex = 0
                for ((srcFile, stat) in showFiles) {
                    val filteredStats = stat.filter { it != DirtyFileState.NON_MODIFIED_IR }
                    val statStr = filteredStats.takeIf { it.isNotEmpty() }?.joinToString { it.str } ?: continue
                    // Use index, because MessageCollector ignores already reported messages
                    messageCollector.report(INFO, "  $libIndex.${++fileIndex}) file [${File(srcFile.path).name}]: ($statStr)")
                }
            }

            artifacts
        } else emptyList()
        return icCaches
    }

    override fun setupPlatformSpecificArgumentsAndServices(
        configuration: CompilerConfiguration,
        arguments: K2JSCompilerArguments,
        services: Services
    ) {
        val messageCollector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        if (arguments.target != null) {
            assert("v5" == arguments.target) { "Unsupported ECMA version: " + arguments.target!! }
        }
        configuration.put(JSConfigurationKeys.TARGET, EcmaVersion.defaultVersion())

        if (arguments.sourceMap) {
            configuration.put(JSConfigurationKeys.SOURCE_MAP, true)
            if (arguments.sourceMapPrefix != null) {
                configuration.put(JSConfigurationKeys.SOURCE_MAP_PREFIX, arguments.sourceMapPrefix!!)
            }

            var sourceMapSourceRoots = arguments.sourceMapBaseDirs
            if (sourceMapSourceRoots == null && StringUtil.isNotEmpty(arguments.sourceMapPrefix)) {
                sourceMapSourceRoots = K2JSCompiler.calculateSourceMapSourceRoot(messageCollector, arguments)
            }

            if (sourceMapSourceRoots != null) {
                val sourceMapSourceRootList = StringUtil.split(sourceMapSourceRoots, File.pathSeparator)
                configuration.put(JSConfigurationKeys.SOURCE_MAP_SOURCE_ROOTS, sourceMapSourceRootList)
            }

        } else {
            if (arguments.sourceMapPrefix != null) {
                messageCollector.report(WARNING, "source-map-prefix argument has no effect without source map", null)
            }
            if (arguments.sourceMapBaseDirs != null) {
                messageCollector.report(WARNING, "source-map-source-root argument has no effect without source map", null)
            }
        }

        if (arguments.metaInfo) {
            configuration.put(JSConfigurationKeys.META_INFO, true)
        }

        configuration.put(JSConfigurationKeys.TYPED_ARRAYS_ENABLED, arguments.typedArrays)

        configuration.put(JSConfigurationKeys.FRIEND_PATHS_DISABLED, arguments.friendModulesDisabled)

        configuration.put(JSConfigurationKeys.GENERATE_STRICT_IMPLICIT_EXPORT, arguments.strictImplicitExportType)


        val friendModules = arguments.friendModules
        if (!arguments.friendModulesDisabled && friendModules != null) {
            val friendPaths = friendModules
                .split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .filterNot { it.isEmpty() }

            configuration.put(JSConfigurationKeys.FRIEND_PATHS, friendPaths)
        }

        val moduleKindName = arguments.moduleKind
        var moduleKind: ModuleKind? = if (moduleKindName != null) moduleKindMap[moduleKindName] else ModuleKind.PLAIN
        if (moduleKind == null) {
            messageCollector.report(
                ERROR, "Unknown module kind: $moduleKindName. Valid values are: plain, amd, commonjs, umd, es", null
            )
            moduleKind = ModuleKind.PLAIN
        }
        if (arguments.wasm) {
            // K/Wasm support ES modules only.
            moduleKind = ModuleKind.ES
        }
        configuration.put(JSConfigurationKeys.MODULE_KIND, moduleKind)

        configuration.putIfNotNull(WebConfigurationKeys.INCREMENTAL_DATA_PROVIDER, services[IncrementalDataProvider::class.java])
        configuration.putIfNotNull(WebConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER, services[IncrementalResultsConsumer::class.java])
        configuration.putIfNotNull(WebConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER, services[IncrementalNextRoundChecker::class.java])
        configuration.putIfNotNull(CommonConfigurationKeys.LOOKUP_TRACKER, services[LookupTracker::class.java])
        configuration.putIfNotNull(CommonConfigurationKeys.EXPECT_ACTUAL_TRACKER, services[ExpectActualTracker::class.java])

        val errorTolerancePolicy = arguments.errorTolerancePolicy?.let { ErrorTolerancePolicy.resolvePolicy(it) }
        configuration.putIfNotNull(WebConfigurationKeys.ERROR_TOLERANCE_POLICY, errorTolerancePolicy)

        if (errorTolerancePolicy?.allowErrors == true) {
            configuration.put(JSConfigurationKeys.DEVELOPER_MODE, true)
        }

        val sourceMapEmbedContentString = arguments.sourceMapEmbedSources
        var sourceMapContentEmbedding: SourceMapSourceEmbedding? = if (sourceMapEmbedContentString != null)
            sourceMapContentEmbeddingMap[sourceMapEmbedContentString]
        else
            SourceMapSourceEmbedding.INLINING
        if (sourceMapContentEmbedding == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map source embedding mode: $sourceMapEmbedContentString. Valid values are: ${sourceMapContentEmbeddingMap.keys.joinToString()}"
            )
            sourceMapContentEmbedding = SourceMapSourceEmbedding.INLINING
        }
        configuration.put(JSConfigurationKeys.SOURCE_MAP_EMBED_SOURCES, sourceMapContentEmbedding)

        if (!arguments.sourceMap && sourceMapEmbedContentString != null) {
            messageCollector.report(WARNING, "source-map-embed-sources argument has no effect without source map", null)
        }

        val sourceMapNamesPolicyString = arguments.sourceMapNamesPolicy
        var sourceMapNamesPolicy: SourceMapNamesPolicy? = if (sourceMapNamesPolicyString != null)
            sourceMapNamesPolicyMap[sourceMapNamesPolicyString]
        else
            SourceMapNamesPolicy.SIMPLE_NAMES
        if (sourceMapNamesPolicy == null) {
            messageCollector.report(
                ERROR,
                "Unknown source map names policy: $sourceMapNamesPolicyString. Valid values are: ${sourceMapNamesPolicyMap.keys.joinToString()}"
            )
            sourceMapNamesPolicy = SourceMapNamesPolicy.SIMPLE_NAMES
        }
        configuration.put(JSConfigurationKeys.SOURCEMAP_NAMES_POLICY, sourceMapNamesPolicy)

        configuration.put(WebConfigurationKeys.PRINT_REACHABILITY_INFO, arguments.irDcePrintReachabilityInfo)
        configuration.put(WebConfigurationKeys.FAKE_OVERRIDE_VALIDATOR, arguments.fakeOverrideValidator)
    }

    override fun executableScriptFileName(): String {
        TODO("Provide a proper way to run the compiler with IR BE")
    }

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion {
        return KlibMetadataVersion(*versionArray)
    }

    override fun MutableList<String>.addPlatformOptions(arguments: K2JSCompilerArguments) {}

    companion object {
        private val moduleKindMap = mapOf(
            K2JsArgumentConstants.MODULE_PLAIN to ModuleKind.PLAIN,
            K2JsArgumentConstants.MODULE_COMMONJS to ModuleKind.COMMON_JS,
            K2JsArgumentConstants.MODULE_AMD to ModuleKind.AMD,
            K2JsArgumentConstants.MODULE_UMD to ModuleKind.UMD,
            K2JsArgumentConstants.MODULE_ES to ModuleKind.ES,
        )
        private val sourceMapContentEmbeddingMap = mapOf(
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_ALWAYS to SourceMapSourceEmbedding.ALWAYS,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_NEVER to SourceMapSourceEmbedding.NEVER,
            K2JsArgumentConstants.SOURCE_MAP_SOURCE_CONTENT_INLINING to SourceMapSourceEmbedding.INLINING
        )

        private val sourceMapNamesPolicyMap = mapOf(
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_NO to SourceMapNamesPolicy.NO,
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_SIMPLE_NAMES to SourceMapNamesPolicy.SIMPLE_NAMES,
            K2JsArgumentConstants.SOURCE_MAP_NAMES_POLICY_FQ_NAMES to SourceMapNamesPolicy.FULLY_QUALIFIED_NAMES
        )

        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JsIrCompiler(), args)
        }

        private fun reportCompiledSourcesList(messageCollector: MessageCollector, sourceFiles: List<KtFile>) {
            val fileNames = sourceFiles.map { file ->
                val virtualFile = file.virtualFile
                if (virtualFile != null) {
                    MessageUtil.virtualFileToPath(virtualFile)
                } else {
                    file.name + " (no virtual file)"
                }
            }
            messageCollector.report(LOGGING, "Compiling source files: " + join(fileNames, ", "), null)
        }

        private fun configureLibraries(libraryString: String?): List<String> =
            libraryString?.splitByPathSeparator() ?: emptyList()

        private fun String.splitByPathSeparator(): List<String> {
            return this.split(File.pathSeparator.toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
                .filterNot { it.isEmpty() }
        }
    }
}

fun RuntimeDiagnostic.Companion.resolve(
    value: String?,
    messageCollector: MessageCollector
): RuntimeDiagnostic? = when (value?.lowercase()) {
    RUNTIME_DIAGNOSTIC_LOG -> RuntimeDiagnostic.LOG
    RUNTIME_DIAGNOSTIC_EXCEPTION -> RuntimeDiagnostic.EXCEPTION
    null -> null
    else -> {
        messageCollector.report(STRONG_WARNING, "Unknown runtime diagnostic '$value'")
        null
    }
}

fun loadPluginsForTests(configuration: CompilerConfiguration): ExitCode {
    var pluginClasspath: Iterable<String> = emptyList()
    val kotlinPaths = PathUtil.kotlinPathsForCompiler
    val libPath = kotlinPaths.libPath.takeIf { it.exists() && it.isDirectory } ?: File(".")
    val (jars, _) =
        PathUtil.KOTLIN_SCRIPTING_PLUGIN_CLASSPATH_JARS.map { File(libPath, it) }.partition { it.exists() }
    pluginClasspath = jars.map { it.canonicalPath } + pluginClasspath

    return PluginCliParser.loadPluginsSafe(pluginClasspath, listOf(), listOf(), configuration)
}

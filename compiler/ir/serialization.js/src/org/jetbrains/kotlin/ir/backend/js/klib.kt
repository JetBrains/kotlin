/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.CommonKLibResolver
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.linkage.partial.createPartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.common.toLogger
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.js.IncrementalDataProvider
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.checkers.JsKlibCheckers
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.*
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.linkage.partial.partialLinkageConfig
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.analyze.AbstractTopDownAnalyzerFacadeForWeb
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.platform.wasm.WasmTarget
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.utils.memoryOptimizedFilter
import org.jetbrains.kotlin.utils.toSmartList
import java.io.File

val KotlinLibrary.moduleName: String
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)

// Considering library built-ins if it has no dependencies.
// All non-built-ins libraries must have built-ins as a dependency.
val KotlinLibrary.isBuiltIns: Boolean
    get() = manifestProperties
        .propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true)
        .isEmpty()

val KotlinLibrary.jsOutputName: String?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_JS_OUTPUT_NAME)

val KotlinLibrary.serializedIrFileFingerprints: List<SerializedIrFileFingerprint>?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS)?.parseSerializedIrFileFingerprints()

val KotlinLibrary.serializedKlibFingerprint: SerializedKlibFingerprint?
    get() = manifestProperties.getProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT)?.let { SerializedKlibFingerprint.fromString(it) }

internal val SerializedIrFile.fileMetadata: ByteArray
    get() = backendSpecificMetadata ?: error("Expect file caches to have backendSpecificMetadata, but '$path' doesn't")

fun generateKLib(
    depsDescriptors: ModulesStructure,
    outputKlibPath: String,
    nopack: Boolean,
    abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT,
    jsOutputName: String?,
    icData: List<KotlinFileSerializedData>,
    moduleFragment: IrModuleFragment,
    diagnosticReporter: DiagnosticReporter,
    builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS,
    wasmTarget: WasmTarget? = null,
) {
    val configuration = depsDescriptors.compilerConfiguration
    val allDependencies = depsDescriptors.allDependencies

    serializeModuleIntoKlib(
        configuration[CommonConfigurationKeys.MODULE_NAME]!!,
        configuration,
        diagnosticReporter,
        KlibMetadataIncrementalSerializer(depsDescriptors, moduleFragment),
        outputKlibPath,
        allDependencies,
        moduleFragment,
        icData,
        nopack,
        perFile = false,
        depsDescriptors.jsFrontEndResult.hasErrors,
        abiVersion,
        jsOutputName,
        builtInsPlatform,
        wasmTarget,
    )
}

data class IrModuleInfo(
    val module: IrModuleFragment,
    val allDependencies: List<IrModuleFragment>,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: JsIrLinker,
    val moduleFragmentToUniqueName: Map<IrModuleFragment, String>,
)

fun sortDependencies(moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>>): Collection<KotlinLibrary> {
    return DFS.topologicalOrder(moduleDependencies.keys) { m ->
        moduleDependencies.getValue(m)
    }.reversed()
}

fun deserializeDependencies(
    sortedDependencies: Collection<KotlinLibrary>,
    irLinker: JsIrLinker,
    mainModuleLib: KotlinLibrary?,
    filesToLoad: Set<String>?,
    mapping: (KotlinLibrary) -> ModuleDescriptor
): Map<IrModuleFragment, KotlinLibrary> {
    return sortedDependencies.associateBy { klib ->
        val descriptor = mapping(klib)
        when {
            mainModuleLib == null -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
            filesToLoad != null && klib == mainModuleLib -> irLinker.deserializeDirtyFiles(descriptor, klib, filesToLoad)
            filesToLoad != null && klib != mainModuleLib -> irLinker.deserializeHeadersWithInlineBodies(descriptor, klib)
            klib == mainModuleLib -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.ALL })
            else -> irLinker.deserializeIrModuleHeader(descriptor, klib, { DeserializationStrategy.EXPLICITLY_EXPORTED })
        }
    }
}

fun loadIr(
    depsDescriptors: ModulesStructure,
    irFactory: IrFactory,
    verifySignatures: Boolean,
    filesToLoad: Set<String>? = null,
    loadFunctionInterfacesIntoStdlib: Boolean = false,
): IrModuleInfo {
    val project = depsDescriptors.project
    val mainModule = depsDescriptors.mainModule
    val configuration = depsDescriptors.compilerConfiguration
    val allDependencies = depsDescriptors.allDependencies
    val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
    val messageLogger = configuration.messageCollector
    val partialLinkageEnabled = configuration.partialLinkageConfig.isEnabled

    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)

    when (mainModule) {
        is MainModule.SourceFiles -> {
            assert(filesToLoad == null)
            val psi2IrContext = preparePsi2Ir(depsDescriptors, errorPolicy, symbolTable, partialLinkageEnabled)
            val friendModules =
                mapOf(psi2IrContext.moduleDescriptor.name.asString() to depsDescriptors.friendDependencies.map { it.uniqueName })

            return getIrModuleInfoForSourceFiles(
                psi2IrContext,
                project,
                configuration,
                mainModule.files,
                sortDependencies(depsDescriptors.moduleDependencies),
                friendModules,
                symbolTable,
                messageLogger,
                loadFunctionInterfacesIntoStdlib,
                verifySignatures,
            ) { depsDescriptors.getModuleDescriptor(it) }
        }
        is MainModule.Klib -> {
            val mainPath = File(mainModule.libPath).canonicalPath
            val mainModuleLib = allDependencies.find { it.libraryFile.canonicalPath == mainPath }
                ?: error("No module with ${mainModule.libPath} found")
            val moduleDescriptor = depsDescriptors.getModuleDescriptor(mainModuleLib)
            val sortedDependencies = sortDependencies(depsDescriptors.moduleDependencies)
            val friendModules = mapOf(mainModuleLib.uniqueName to depsDescriptors.friendDependencies.map { it.uniqueName })

            return getIrModuleInfoForKlib(
                moduleDescriptor,
                sortedDependencies,
                friendModules,
                filesToLoad,
                configuration,
                symbolTable,
                messageLogger,
                loadFunctionInterfacesIntoStdlib,
            ) { depsDescriptors.getModuleDescriptor(it) }
        }
    }
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun getIrModuleInfoForKlib(
    moduleDescriptor: ModuleDescriptor,
    sortedDependencies: Collection<KotlinLibrary>,
    friendModules: Map<String, List<String>>,
    filesToLoad: Set<String>?,
    configuration: CompilerConfiguration,
    symbolTable: SymbolTable,
    messageCollector: MessageCollector,
    loadFunctionInterfacesIntoStdlib: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleInfo {
    val mainModuleLib = sortedDependencies.last()
    val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
    val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)
    val errorPolicy = configuration[JSConfigurationKeys.ERROR_TOLERANCE_POLICY] ?: ErrorTolerancePolicy.DEFAULT

    val irLinker = JsIrLinker(
        currentModule = null,
        messageCollector = messageCollector,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageSupport = createPartialLinkageSupportForLinker(
            partialLinkageConfig = configuration.partialLinkageConfig,
            allowErrorTypes = errorPolicy.allowErrors,
            builtIns = irBuiltIns,
            messageCollector = messageCollector
        ),
        translationPluginContext = null,
        icData = null,
        friendModules = friendModules
    )

    val deserializedModuleFragmentsToLib = deserializeDependencies(sortedDependencies, irLinker, mainModuleLib, filesToLoad, mapping)
    val deserializedModuleFragments = deserializedModuleFragmentsToLib.keys.toList()
    irBuiltIns.functionFactory = IrDescriptorBasedFunctionFactory(
        irBuiltIns,
        symbolTable,
        typeTranslator,
        loadFunctionInterfacesIntoStdlib.ifTrue {
            FunctionTypeInterfacePackages().makePackageAccessor(deserializedModuleFragments.first())
        },
        true
    )

    val moduleFragment = deserializedModuleFragments.last()

    irLinker.init(null, emptyList())
    ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
    irLinker.postProcess(inOrAfterLinkageStep = true)

    return IrModuleInfo(
        moduleFragment,
        deserializedModuleFragments,
        irBuiltIns,
        symbolTable,
        irLinker,
        deserializedModuleFragmentsToLib.getUniqueNameForEachFragment()
    )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun getIrModuleInfoForSourceFiles(
    psi2IrContext: GeneratorContext,
    project: Project,
    configuration: CompilerConfiguration,
    files: List<KtFile>,
    allSortedDependencies: Collection<KotlinLibrary>,
    friendModules: Map<String, List<String>>,
    symbolTable: SymbolTable,
    messageCollector: MessageCollector,
    loadFunctionInterfacesIntoStdlib: Boolean,
    verifySignatures: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor
): IrModuleInfo {
    val irBuiltIns = psi2IrContext.irBuiltIns
    val feContext = psi2IrContext.run {
        JsIrLinker.JsFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
    }
    val errorPolicy = configuration[JSConfigurationKeys.ERROR_TOLERANCE_POLICY] ?: ErrorTolerancePolicy.DEFAULT

    val irLinker = JsIrLinker(
        currentModule = psi2IrContext.moduleDescriptor,
        messageCollector = messageCollector,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageSupport = createPartialLinkageSupportForLinker(
            partialLinkageConfig = configuration.partialLinkageConfig,
            allowErrorTypes = errorPolicy.allowErrors,
            builtIns = irBuiltIns,
            messageCollector = messageCollector
        ),
        translationPluginContext = feContext,
        icData = null,
        friendModules = friendModules,
    )
    val deserializedModuleFragmentsToLib = deserializeDependencies(allSortedDependencies, irLinker, null, null, mapping)
    val deserializedModuleFragments = deserializedModuleFragmentsToLib.keys.toList()
    (irBuiltIns as IrBuiltInsOverDescriptors).functionFactory =
        IrDescriptorBasedFunctionFactory(
            irBuiltIns,
            symbolTable,
            psi2IrContext.typeTranslator,
            loadFunctionInterfacesIntoStdlib.ifTrue {
                FunctionTypeInterfacePackages().makePackageAccessor(deserializedModuleFragments.first())
            },
            true
        )

    val (moduleFragment, _) = psi2IrContext.generateModuleFragmentWithPlugins(project, files, irLinker, messageCollector)

    // TODO: not sure whether this check should be enabled by default. Add configuration key for it.
    val mangleChecker = ManglerChecker(JsManglerIr, Ir2DescriptorManglerAdapter(JsManglerDesc))
    if (verifySignatures) {
        moduleFragment.acceptVoid(mangleChecker)
    }

    if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
        irLinker.modules.forEach { fakeOverrideChecker.check(it) }
    }

    if (verifySignatures) {
        irBuiltIns.knownBuiltins.forEach { it.acceptVoid(mangleChecker) }
    }

    return IrModuleInfo(
        moduleFragment,
        deserializedModuleFragments,
        irBuiltIns,
        symbolTable,
        irLinker,
        deserializedModuleFragmentsToLib.getUniqueNameForEachFragment()
    )
}

private fun preparePsi2Ir(
    depsDescriptors: ModulesStructure,
    errorIgnorancePolicy: ErrorTolerancePolicy,
    symbolTable: SymbolTable,
    partialLinkageEnabled: Boolean
): GeneratorContext {
    val analysisResult = depsDescriptors.jsFrontEndResult
    val psi2Ir = Psi2IrTranslator(
        depsDescriptors.compilerConfiguration.languageVersionSettings,
        Psi2IrConfiguration(errorIgnorancePolicy.allowErrors, partialLinkageEnabled),
        depsDescriptors.compilerConfiguration::checkNoUnboundSymbols
    )
    return psi2Ir.createGeneratorContext(
        analysisResult.moduleDescriptor,
        analysisResult.bindingContext,
        symbolTable
    )
}

fun GeneratorContext.generateModuleFragmentWithPlugins(
    project: Project,
    files: List<KtFile>,
    irLinker: IrDeserializer,
    messageCollector: MessageCollector,
    stubGenerator: DeclarationStubGenerator? = null
): Pair<IrModuleFragment, IrPluginContext> {
    val psi2Ir = Psi2IrTranslator(languageVersionSettings, configuration, messageCollector::checkNoUnboundSymbols)
    val extensions = IrGenerationExtension.getInstances(project)

    // plugin context should be instantiated before postprocessing steps
    val pluginContext = IrPluginContextImpl(
        moduleDescriptor,
        bindingContext,
        languageVersionSettings,
        symbolTable,
        typeTranslator,
        irBuiltIns,
        linker = irLinker,
        messageCollector
    )
    if (extensions.isNotEmpty()) {
        for (extension in extensions) {
            psi2Ir.addPostprocessingStep { module ->
                val old = stubGenerator?.unboundSymbolGeneration
                try {
                    stubGenerator?.unboundSymbolGeneration = true
                    extension.generate(module, pluginContext)
                } finally {
                    stubGenerator?.unboundSymbolGeneration = old!!
                }
            }
        }

    }

    return psi2Ir.generateModuleFragment(
        this,
        files,
        listOf(stubGenerator ?: irLinker),
        extensions
    ) to pluginContext
}

private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}
public val JsFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)

fun getModuleDescriptorByLibrary(current: KotlinLibrary, mapping: Map<String, ModuleDescriptorImpl>): ModuleDescriptorImpl {
    val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
        current,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        null,
        packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
        lookupTracker = LookupTracker.DO_NOTHING
    )
//    if (isBuiltIns) runtimeModule = md

    val dependencies = current.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { mapping.getValue(it) }

    md.setDependencies(listOf(md) + dependencies)
    return md
}

sealed class MainModule {
    class SourceFiles(val files: List<KtFile>) : MainModule()
    class Klib(val libPath: String) : MainModule()
}

class ModulesStructure(
    val project: Project,
    val mainModule: MainModule,
    val compilerConfiguration: CompilerConfiguration,
    val dependencies: Collection<String>,
    friendDependenciesPaths: Collection<String>,
) {

    val allDependenciesResolution = CommonKLibResolver.resolveWithoutDependencies(
        dependencies,
        compilerConfiguration.messageCollector.toLogger(),
        compilerConfiguration.get(JSConfigurationKeys.ZIP_FILE_SYSTEM_ACCESSOR)
    )

    val allDependencies: List<KotlinLibrary>
        get() = allDependenciesResolution.libraries

    val friendDependencies = allDependencies.run {
        val friendAbsolutePaths = friendDependenciesPaths.map { File(it).canonicalPath }
        memoryOptimizedFilter {
            it.libraryFile.absolutePath in friendAbsolutePaths
        }
    }

    val moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>> by lazy {
        val transitives = allDependenciesResolution.resolveWithDependencies().getFullResolvedList()
        transitives.associate { klib ->
            klib.library to klib.resolvedDependencies.map { d -> d.library }
        }.toMap()
    }

    private val builtInsDep = allDependencies.find { it.isBuiltIns }

    class JsFrontEndResult(val jsAnalysisResult: AnalysisResult, val hasErrors: Boolean) {
        val moduleDescriptor: ModuleDescriptor
            get() = jsAnalysisResult.moduleDescriptor

        val bindingContext: BindingContext
            get() = jsAnalysisResult.bindingContext
    }

    lateinit var jsFrontEndResult: JsFrontEndResult

    fun runAnalysis(
        errorPolicy: ErrorTolerancePolicy,
        analyzer: AbstractAnalyzerWithCompilerReport,
        analyzerFacade: AbstractTopDownAnalyzerFacadeForWeb
    ) {
        require(mainModule is MainModule.SourceFiles)
        val files = mainModule.files

        analyzer.analyzeAndReport(files) {
            analyzerFacade.analyzeFiles(
                files,
                project,
                compilerConfiguration,
                descriptors.values.toList(),
                friendDependencies.map { getModuleDescriptor(it) },
                analyzer.targetEnvironment,
                thisIsBuiltInsModule = builtInModuleDescriptor == null,
                customBuiltInsModule = builtInModuleDescriptor
            )
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val analysisResult = analyzer.analysisResult
        if (compilerConfiguration.getBoolean(CommonConfigurationKeys.INCREMENTAL_COMPILATION)) {
            val shouldGoToNextIcRound = shouldGoToNextIcRound(compilerConfiguration) {
                KlibMetadataIncrementalSerializer(
                    files,
                    compilerConfiguration,
                    project,
                    analysisResult.bindingContext,
                    analysisResult.moduleDescriptor,
                    errorPolicy.allowErrors,
                )
            }
            if (shouldGoToNextIcRound) {
                throw IncrementalNextRoundException()
            }
        }

        var hasErrors = false
        if (analyzer.hasErrors() || analysisResult !is JsAnalysisResult) {
            if (!errorPolicy.allowErrors)
                throw CompilationErrorException()
            else hasErrors = true
        }

        hasErrors = analyzerFacade.checkForErrors(files, analysisResult.bindingContext, errorPolicy) || hasErrors

        jsFrontEndResult = JsFrontEndResult(analysisResult, hasErrors)
    }

    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    private val _descriptors: MutableMap<KotlinLibrary, ModuleDescriptorImpl> = mutableMapOf()

    init {
        val descriptors = allDependencies.map { getModuleDescriptorImpl(it) }
        val friendDescriptors = friendDependencies.mapTo(mutableSetOf(), ::getModuleDescriptorImpl)
        descriptors.forEach { descriptor ->
            descriptor.setDependencies(descriptors, friendDescriptors)
        }
    }

    // TODO: these are roughly equivalent to KlibResolvedModuleDescriptorsFactoryImpl. Refactor me.
    val descriptors: Map<KotlinLibrary, ModuleDescriptor>
        get() = _descriptors

    private fun getModuleDescriptorImpl(current: KotlinLibrary): ModuleDescriptorImpl {
        if (current in _descriptors) {
            return _descriptors.getValue(current)
        }

        val isBuiltIns = current.unresolvedDependencies.isEmpty()

        val lookupTracker = compilerConfiguration[CommonConfigurationKeys.LOOKUP_TRACKER] ?: LookupTracker.DO_NOTHING
        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            languageVersionSettings,
            storageManager,
            runtimeModule?.builtIns,
            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
            lookupTracker = lookupTracker
        )
        if (isBuiltIns) runtimeModule = md

        _descriptors[current] = md

        return md
    }

    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptor =
        getModuleDescriptorImpl(current)

    val builtInModuleDescriptor =
        if (builtInsDep != null)
            getModuleDescriptor(builtInsDep)
        else
            null // null in case compiling builtInModule itself
}

private const val FILE_FINGERPRINTS_SEPARATOR = " "

private fun List<SerializedIrFileFingerprint>.joinIrFileFingerprints(): String {
    return joinToString(FILE_FINGERPRINTS_SEPARATOR)
}

private fun String.parseSerializedIrFileFingerprints(): List<SerializedIrFileFingerprint> {
    return split(FILE_FINGERPRINTS_SEPARATOR).mapNotNull(SerializedIrFileFingerprint::fromString)
}

fun serializeModuleIntoKlib(
    moduleName: String,
    configuration: CompilerConfiguration,
    diagnosticReporter: DiagnosticReporter,
    metadataSerializer: KlibSingleFileMetadataSerializer<*>,
    klibPath: String,
    dependencies: List<KotlinLibrary>,
    moduleFragment: IrModuleFragment,
    cleanFiles: List<KotlinFileSerializedData>,
    nopack: Boolean,
    perFile: Boolean,
    containsErrorCode: Boolean = false,
    abiVersion: KotlinAbiVersion,
    jsOutputName: String?,
    builtInsPlatform: BuiltInsPlatform = BuiltInsPlatform.JS,
    wasmTarget: WasmTarget? = null,
) {
    val moduleExportedNames = moduleFragment.collectExportedNames()
    val incrementalResultsConsumer = configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    val empty = ByteArray(0)
    val serializerOutput = serializeModuleIntoKlib(
        moduleName = moduleFragment.name.asString(),
        irModuleFragment = moduleFragment,
        configuration = configuration,
        diagnosticReporter = diagnosticReporter,
        compatibilityMode = CompatibilityMode(abiVersion),
        cleanFiles = cleanFiles,
        dependencies = dependencies,
        createModuleSerializer = {
                irDiagnosticReporter,
                irBuiltins,
                compatibilityMode,
                normalizeAbsolutePaths,
                sourceBaseDirs,
                languageVersionSettings,
                shouldCheckSignaturesOnUniqueness,
            ->
            JsIrModuleSerializer(
                irDiagnosticReporter,
                irBuiltins,
                compatibilityMode,
                normalizeAbsolutePaths,
                sourceBaseDirs,
                languageVersionSettings,
                shouldCheckSignaturesOnUniqueness,
            ) { JsIrFileMetadata(moduleExportedNames[it]?.values?.toSmartList() ?: emptyList()) }
        },
        metadataSerializer = metadataSerializer,
        runKlibCheckers = { irModuleFragment, irDiagnosticReporter, compilerConfiguration ->
            if (builtInsPlatform == BuiltInsPlatform.JS) {
                val cleanFilesIrData = cleanFiles.map { it.irData ?: error("Metadata-only KLIBs are not supported in Kotlin/JS") }
                JsKlibCheckers.check(cleanFilesIrData, irModuleFragment, moduleExportedNames, irDiagnosticReporter, compilerConfiguration)
            }
        },
        processCompiledFileData = { ioFile, compiledFile ->
            incrementalResultsConsumer?.run {
                processPackagePart(ioFile, compiledFile.metadata, empty, empty)
                with(compiledFile.irData!!) {
                    processIrFile(
                        ioFile,
                        fileData,
                        types,
                        signatures,
                        strings,
                        declarations,
                        bodies,
                        fqName.toByteArray(),
                        fileMetadata,
                        debugInfo,
                    )
                }
            }
        },
        processKlibHeader = {
            incrementalResultsConsumer?.processHeader(it)
        },
    )

    val fullSerializedIr = serializerOutput.serializedIr ?: error("Metadata-only KLIBs are not supported in Kotlin/JS")

    val versions = KotlinLibraryVersioning(
        abiVersion = abiVersion,
        compilerVersion = KotlinCompilerVersion.VERSION,
        metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
    )

    val properties = Properties().also { p ->
        if (jsOutputName != null) {
            p.setProperty(KLIB_PROPERTY_JS_OUTPUT_NAME, jsOutputName)
        }
        val wasmTargets = listOfNotNull(/* in the future there might be multiple WASM targets */ wasmTarget)
        if (wasmTargets.isNotEmpty()) {
            p.setProperty(KLIB_PROPERTY_WASM_TARGETS, wasmTargets.joinToString(" ") { it.alias })
        }
        if (containsErrorCode) {
            p.setProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE, "true")
        }

        val fingerprints = fullSerializedIr.files.sortedBy { it.path }.map { SerializedIrFileFingerprint(it) }
        p.setProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS, fingerprints.joinIrFileFingerprints())
        p.setProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT, SerializedKlibFingerprint(fingerprints).klibFingerprint.toString())
    }

    buildKotlinLibrary(
        linkDependencies = serializerOutput.neededLibraries,
        ir = fullSerializedIr,
        metadata = serializerOutput.serializedMetadata ?: error("expected serialized metadata"),
        dataFlowGraph = null,
        manifestProperties = properties,
        moduleName = moduleName,
        nopack = nopack,
        perFile = perFile,
        output = klibPath,
        versions = versions,
        builtInsPlatform = builtInsPlatform
    )
}

const val KLIB_PROPERTY_JS_OUTPUT_NAME = "jsOutputName"
const val KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS = "serializedIrFileFingerprints"
const val KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT = "serializedKlibFingerprint"

fun <SourceFile> shouldGoToNextIcRound(
    compilerConfiguration: CompilerConfiguration,
    createMetadataSerializer: () -> KlibSingleFileMetadataSerializer<SourceFile>,
): Boolean {
    val nextRoundChecker = compilerConfiguration.get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return false
    createMetadataSerializer().run {
        forEachFile { _, sourceFile, ktSourceFile, _ ->
            val protoBuf = serializeSingleFileMetadata(sourceFile)
            // to minimize the number of IC rounds, we should inspect all proto for changes first,
            // then go to the next round if needed, with all new dirty files
            nextRoundChecker.checkProtoChanges(ktSourceFile.toIoFileOrNull()!!, protoBuf.toByteArray())
        }
    }
    return nextRoundChecker.shouldGoToNextRound()
}

private fun Map<IrModuleFragment, KotlinLibrary>.getUniqueNameForEachFragment(): Map<IrModuleFragment, String> {
    return this.entries.mapNotNull { (moduleFragment, klib) ->
        klib.jsOutputName?.let { moduleFragment to it }
    }.toMap()
}

fun IncrementalDataProvider.getSerializedData(newSources: List<KtSourceFile>): List<KotlinFileSerializedData> {
    val nonCompiledSources = newSources.associateBy { it.toIoFileOrNull()!! }
    val compiledIrFiles = serializedIrFiles
    val compiledMetaFiles = compiledPackageParts

    assert(compiledIrFiles.size == compiledMetaFiles.size)

    val storage = mutableListOf<KotlinFileSerializedData>()

    for (f in compiledIrFiles.keys) {
        if (f in nonCompiledSources) continue

        val irData = compiledIrFiles[f] ?: error("No Ir Data found for file $f")
        val metaFile = compiledMetaFiles[f] ?: error("No Meta Data found for file $f")
        val irFile = with(irData) {
            SerializedIrFile(
                fileData,
                String(fqn),
                f.path.replace('\\', '/'),
                types,
                signatures,
                strings,
                bodies,
                declarations,
                debugInfo,
                fileMetadata,
            )
        }
        storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
    }
    return storage
}

@JvmName("getSerializedDataByPsiFiles")
fun IncrementalDataProvider.getSerializedData(newSources: List<KtFile>): List<KotlinFileSerializedData> =
    getSerializedData(newSources.map(::KtPsiSourceFile))

val CompilerConfiguration.incrementalDataProvider: IncrementalDataProvider?
    get() = get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.web

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.KtPsiSourceFile
import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.KtVirtualFileSourceFile
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.common.serialization.metadata.*
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.web.IncrementalDataProvider
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.web.lower.serialization.ir.*
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrDescriptorBasedFunctionFactory
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.web.analyze.AbstractTopDownAnalyzerFacadeForWeb
import org.jetbrains.kotlin.web.analyzer.WebAnalysisResult
import org.jetbrains.kotlin.web.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.web.config.WebConfigurationKeys
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.metadata.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
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

private val CompilerConfiguration.metadataVersion
    get() = get(CommonConfigurationKeys.METADATA_VERSION) as? KlibMetadataVersion ?: KlibMetadataVersion.INSTANCE

private val CompilerConfiguration.expectActualLinker: Boolean
    get() = get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false

val CompilerConfiguration.resolverLogger: Logger
    get() = when (val messageLogger = this[IrMessageLogger.IR_MESSAGE_LOGGER]) {
        null -> DummyLogger
        else -> object : Logger {
            override fun log(message: String) = messageLogger.report(IrMessageLogger.Severity.INFO, message, null)
            override fun error(message: String) = messageLogger.report(IrMessageLogger.Severity.ERROR, message, null)
            override fun warning(message: String) = messageLogger.report(IrMessageLogger.Severity.WARNING, message, null)

            override fun fatal(message: String): Nothing {
                messageLogger.report(IrMessageLogger.Severity.ERROR, message, null)
                kotlin.error("FATAL ERROR: $message")
            }
        }
    }

class KotlinFileSerializedData(val metadata: ByteArray, val irData: SerializedIrFile)

fun generateKLib(
    depsDescriptors: ModulesStructure,
    outputKlibPath: String,
    nopack: Boolean,
    abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT,
    jsOutputName: String?,
    icData: List<KotlinFileSerializedData>,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    moduleFragment: IrModuleFragment,
    serializeSingleFile: (KtSourceFile) -> ProtoBuf.PackageFragment
) {
    val files = (depsDescriptors.mainModule as MainModule.SourceFiles).files.map(::KtPsiSourceFile)
    val configuration = depsDescriptors.compilerConfiguration
    val allDependencies = depsDescriptors.allDependencies
    val messageLogger = configuration.irMessageLogger

    serializeModuleIntoKlib(
        configuration[CommonConfigurationKeys.MODULE_NAME]!!,
        configuration,
        messageLogger,
        files,
        outputKlibPath,
        allDependencies,
        moduleFragment,
        expectDescriptorToSymbol,
        icData,
        nopack,
        perFile = false,
        depsDescriptors.webFrontEndResult.hasErrors,
        abiVersion,
        jsOutputName,
        serializeSingleFile
    )
}

data class IrModuleInfo(
    val module: IrModuleFragment,
    val allDependencies: List<IrModuleFragment>,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: WebIrLinker,
    val moduleFragmentToUniqueName: Map<IrModuleFragment, String>,
)

fun sortDependencies(moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>>): Collection<KotlinLibrary> {
    return DFS.topologicalOrder(moduleDependencies.keys) { m ->
        moduleDependencies.getValue(m)
    }.reversed()
}

fun deserializeDependencies(
    sortedDependencies: Collection<KotlinLibrary>,
    irLinker: WebIrLinker,
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
    val errorPolicy = configuration.get(WebConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
    val messageLogger = configuration.irMessageLogger
    val partialLinkageEnabled = configuration[WebConfigurationKeys.PARTIAL_LINKAGE] ?: false

    val signaturer = IdSignatureDescriptor(WebManglerDesc)
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
    messageLogger: IrMessageLogger,
    loadFunctionInterfacesIntoStdlib: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor,
): IrModuleInfo {
    val mainModuleLib = sortedDependencies.last()
    val typeTranslator = TypeTranslatorImpl(symbolTable, configuration.languageVersionSettings, moduleDescriptor)
    val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)

    val partialLinkageEnabled = configuration[WebConfigurationKeys.PARTIAL_LINKAGE] ?: false

    val irLinker = WebIrLinker(
        currentModule = null,
        messageLogger = messageLogger,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageEnabled = partialLinkageEnabled,
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
    irLinker.postProcess()

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
    messageLogger: IrMessageLogger,
    loadFunctionInterfacesIntoStdlib: Boolean,
    verifySignatures: Boolean,
    mapping: (KotlinLibrary) -> ModuleDescriptor
): IrModuleInfo {
    val irBuiltIns = psi2IrContext.irBuiltIns
    val feContext = psi2IrContext.run {
        WebIrLinker.WebFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
    }

    val partialLinkageEnabled = configuration[WebConfigurationKeys.PARTIAL_LINKAGE] ?: false

    val irLinker = WebIrLinker(
        currentModule = psi2IrContext.moduleDescriptor,
        messageLogger = messageLogger,
        builtIns = irBuiltIns,
        symbolTable = symbolTable,
        partialLinkageEnabled = partialLinkageEnabled,
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

    val (moduleFragment, _) = psi2IrContext.generateModuleFragmentWithPlugins(project, files, irLinker, messageLogger)

    // TODO: not sure whether this check should be enabled by default. Add configuration key for it.
    val mangleChecker = ManglerChecker(WebManglerIr, Ir2DescriptorManglerAdapter(WebManglerDesc))
    if (verifySignatures) {
        moduleFragment.acceptVoid(mangleChecker)
    }

    if (configuration.getBoolean(WebConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(WebManglerIr, WebManglerDesc)
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
    val analysisResult = depsDescriptors.webFrontEndResult
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
    messageLogger: IrMessageLogger,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null,
    stubGenerator: DeclarationStubGenerator? = null
): Pair<IrModuleFragment, IrPluginContext> {
    val psi2Ir = Psi2IrTranslator(languageVersionSettings, configuration, messageLogger::checkNoUnboundSymbols)
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
        messageLogger
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
        extensions,
        expectDescriptorToSymbol
    ) to pluginContext
}

private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}
public val WebFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)

fun getModuleDescriptorByLibrary(current: KotlinLibrary, mapping: Map<String, ModuleDescriptorImpl>): ModuleDescriptorImpl {
    val md = WebFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
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

    val allDependenciesResolution = webResolveLibrariesWithoutDependencies(
        dependencies,
        compilerConfiguration.resolverLogger
    )

    val allDependencies: List<KotlinLibrary>
        get() = allDependenciesResolution.libraries

    val friendDependencies = allDependencies.run {
        val friendAbsolutePaths = friendDependenciesPaths.map { File(it).canonicalPath }
        filter {
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

    class WebFrontEndResult(val analysisResult: WebAnalysisResult, val hasErrors: Boolean) {
        val moduleDescriptor: ModuleDescriptor
            get() = analysisResult.moduleDescriptor

        val bindingContext: BindingContext
            get() = analysisResult.bindingContext
    }

    lateinit var webFrontEndResult: WebFrontEndResult

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
            /** can throw [IncrementalNextRoundException] */
            compareMetadataAndGoToNextICRoundIfNeeded(analysisResult, compilerConfiguration, project, files, errorPolicy.allowErrors)
        }

        var hasErrors = false
        if (analyzer.hasErrors() || analysisResult !is WebAnalysisResult) {
            if (!errorPolicy.allowErrors)
                throw CompilationErrorException()
            else hasErrors = true
        }

        hasErrors = analyzerFacade.checkForErrors(files, analysisResult.bindingContext, errorPolicy) || hasErrors

        webFrontEndResult = WebFrontEndResult(analysisResult as WebAnalysisResult, hasErrors)
    }

    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    private val _descriptors: MutableMap<KotlinLibrary, ModuleDescriptorImpl> = mutableMapOf()

    init {
        val descriptors = allDependencies.map { getModuleDescriptorImpl(it) }

        descriptors.forEach { descriptor ->
            descriptor.setDependencies(descriptors)
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
        val md = WebFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
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

private fun getDescriptorForElement(
    context: BindingContext,
    element: PsiElement
): DeclarationDescriptor = BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element)

fun serializeModuleIntoKlib(
    moduleName: String,
    configuration: CompilerConfiguration,
    messageLogger: IrMessageLogger,
    files: List<KtSourceFile>,
    klibPath: String,
    dependencies: List<KotlinLibrary>,
    moduleFragment: IrModuleFragment,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    cleanFiles: List<KotlinFileSerializedData>,
    nopack: Boolean,
    perFile: Boolean,
    containsErrorCode: Boolean = false,
    abiVersion: KotlinAbiVersion,
    jsOutputName: String?,
    serializeSingleFile: (KtSourceFile) -> ProtoBuf.PackageFragment
) {
    assert(files.size == moduleFragment.files.size)

    val compatibilityMode = CompatibilityMode(abiVersion)
    val sourceBaseDirs = configuration[CommonConfigurationKeys.KLIB_RELATIVE_PATH_BASES] ?: emptyList()
    val absolutePathNormalization = configuration[CommonConfigurationKeys.KLIB_NORMALIZE_ABSOLUTE_PATH] ?: false
    val signatureClashChecks = configuration[CommonConfigurationKeys.PRODUCE_KLIB_SIGNATURES_CLASH_CHECKS] ?: false

    val serializedIr =
        WebIrModuleSerializer(
            messageLogger,
            moduleFragment.irBuiltins,
            expectDescriptorToSymbol,
            compatibilityMode,
            skipExpects = !configuration.expectActualLinker,
            normalizeAbsolutePaths = absolutePathNormalization,
            sourceBaseDirs = sourceBaseDirs,
            configuration.languageVersionSettings,
            signatureClashChecks
        ).serializedIrModule(moduleFragment)

    val moduleDescriptor = moduleFragment.descriptor

    val incrementalResultsConsumer = configuration.get(WebConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    val empty = ByteArray(0)

    fun processCompiledFileData(ioFile: File, compiledFile: KotlinFileSerializedData) {
        incrementalResultsConsumer?.run {
            processPackagePart(ioFile, compiledFile.metadata, empty, empty)
            with(compiledFile.irData) {
                processIrFile(ioFile, fileData, types, signatures, strings, declarations, bodies, fqName.toByteArray(), debugInfo)
            }
        }
    }

    val additionalFiles = mutableListOf<KotlinFileSerializedData>()

    for ((ktSourceFile, binaryFile) in files.zip(serializedIr.files)) {
        assert(ktSourceFile.path == binaryFile.path) {
            """The Kt and Ir files are put in different order
                Kt: ${ktSourceFile.path}
                Ir: ${binaryFile.path}
            """.trimMargin()
        }
        val packageFragment = serializeSingleFile(ktSourceFile)
        val compiledKotlinFile = KotlinFileSerializedData(packageFragment.toByteArray(), binaryFile)

        additionalFiles += compiledKotlinFile
        val ioFile = ktSourceFile.toIoFileOrNull()
        assert(ioFile != null) {
            "No file found for source ${ktSourceFile.path}"
        }
        processCompiledFileData(ioFile!!, compiledKotlinFile)
    }

    val compiledKotlinFiles = (cleanFiles + additionalFiles)

    val header = serializeKlibHeader(
        configuration.languageVersionSettings, moduleDescriptor,
        compiledKotlinFiles.map { it.irData.fqName }.distinct().sorted(),
        emptyList()
    ).toByteArray()

    incrementalResultsConsumer?.run {
        processHeader(header)
    }

    val serializedMetadata =
        makeSerializedKlibMetadata(
            compiledKotlinFiles.groupBy { it.irData.fqName }
                .map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap(),
            header
        )

    val fullSerializedIr = SerializedIrModule(compiledKotlinFiles.map { it.irData })

    val versions = KotlinLibraryVersioning(
        abiVersion = compatibilityMode.abiVersion,
        libraryVersion = null,
        compilerVersion = KotlinCompilerVersion.VERSION,
        metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
    )

    val properties = Properties().also { p ->
        if (jsOutputName != null) {
            p.setProperty(KLIB_PROPERTY_JS_OUTPUT_NAME, jsOutputName)
        }
        if (containsErrorCode) {
            p.setProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE, "true")
        }

        val fingerprints = fullSerializedIr.files.sortedBy { it.path }.map { SerializedIrFileFingerprint(it) }
        p.setProperty(KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS, fingerprints.joinIrFileFingerprints())
        p.setProperty(KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT, SerializedKlibFingerprint(fingerprints).klibFingerprint.toString())
    }

    buildKotlinLibrary(
        linkDependencies = dependencies,
        ir = fullSerializedIr,
        metadata = serializedMetadata,
        dataFlowGraph = null,
        manifestProperties = properties,
        moduleName = moduleName,
        nopack = nopack,
        perFile = perFile,
        output = klibPath,
        versions = versions,
        builtInsPlatform = BuiltInsPlatform.JS
    )
}

const val KLIB_PROPERTY_JS_OUTPUT_NAME = "jsOutputName"
const val KLIB_PROPERTY_SERIALIZED_IR_FILE_FINGERPRINTS = "serializedIrFileFingerprints"
const val KLIB_PROPERTY_SERIALIZED_KLIB_FINGERPRINT = "serializedKlibFingerprint"

fun KlibMetadataIncrementalSerializer.serializeScope(
    ktFile: KtFile,
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor
): ProtoBuf.PackageFragment {
    val memberScope = ktFile.declarations.map { getDescriptorForElement(bindingContext, it) }
    return serializePackageFragment(moduleDescriptor, memberScope, ktFile.packageFqName)
}

fun KlibMetadataIncrementalSerializer.serializeScope(
    ktSourceFile: KtSourceFile,
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor
): ProtoBuf.PackageFragment {
    val ktFile = (ktSourceFile as KtPsiSourceFile).psiFile as KtFile
    val memberScope = ktFile.declarations.map { getDescriptorForElement(bindingContext, it) }
    return serializePackageFragment(moduleDescriptor, memberScope, ktFile.packageFqName)
}

private fun compareMetadataAndGoToNextICRoundIfNeeded(
    analysisResult: AnalysisResult,
    config: CompilerConfiguration,
    project: Project,
    files: List<KtFile>,
    allowErrors: Boolean
) {
    val nextRoundChecker = config.get(WebConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return
    val bindingContext = analysisResult.bindingContext
    val serializer = KlibMetadataIncrementalSerializer(config, project, allowErrors)
    for (ktFile in files) {
        val packageFragment = serializer.serializeScope(ktFile, bindingContext, analysisResult.moduleDescriptor)
        // to minimize a number of IC rounds, we should inspect all proto for changes first,
        // then go to a next round if needed, with all new dirty files
        nextRoundChecker.checkProtoChanges(VfsUtilCore.virtualToIoFile(ktFile.virtualFile), packageFragment.toByteArray())
    }

    if (nextRoundChecker.shouldGoToNextRound()) throw IncrementalNextRoundException()
}

fun KlibMetadataIncrementalSerializer(configuration: CompilerConfiguration, project: Project, allowErrors: Boolean) =
    KlibMetadataIncrementalSerializer(
        languageVersionSettings = configuration.languageVersionSettings,
        metadataVersion = configuration.metadataVersion,
        project = project,
        exportKDoc = false,
        skipExpects = !configuration.expectActualLinker,
        allowErrorTypes = allowErrors
    )

private fun Map<IrModuleFragment, KotlinLibrary>.getUniqueNameForEachFragment(): Map<IrModuleFragment, String> {
    return this.entries.mapNotNull { (moduleFragment, klib) ->
        klib.jsOutputName?.let { moduleFragment to it }
    }.toMap()
}

fun KtSourceFile.toIoFileOrNull(): File? = when (this) {
    is KtIoFileSourceFile -> file
    is KtVirtualFileSourceFile -> VfsUtilCore.virtualToIoFile(virtualFile)
    is KtPsiSourceFile -> VfsUtilCore.virtualToIoFile(psiFile.virtualFile)
    else -> path?.let(::File)
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
                debugInfo
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
    get() = get(WebConfigurationKeys.INCREMENTAL_DATA_PROVIDER)

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationRemover
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataIncrementalSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.js.ic.SerializedIcData
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.IrMessageLogger
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.noUnboundLeft
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.Properties
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.buildKotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.util.DummyLogger
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

val KotlinLibrary.moduleName: String
    get() = manifestProperties.getProperty(KLIB_PROPERTY_UNIQUE_NAME)

// Considering library built-ins if it has no dependencies.
// All non-built-ins libraries must have built-ins as a dependency.
val KotlinLibrary.isBuiltIns: Boolean
    get() = manifestProperties
        .propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true)
        .isEmpty()

// TODO: The only place loadKlib() is used now is Wasm backend.
// Need to move it to SearchPathResolver too.
fun loadKlib(klibPath: String) =
    resolveSingleFileKlib(KFile(KFile(klibPath).absolutePath))

private val CompilerConfiguration.metadataVersion
    get() = get(CommonConfigurationKeys.METADATA_VERSION) as? KlibMetadataVersion ?: KlibMetadataVersion.INSTANCE

private val CompilerConfiguration.expectActualLinker: Boolean
    get() = get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false

class KotlinFileSerializedData(val metadata: ByteArray, val irData: SerializedIrFile)

fun IrMessageLogger?.toResolverLogger(): Logger {
    if (this == null) return DummyLogger

    return object : Logger {
        override fun log(message: String) {
            report(IrMessageLogger.Severity.INFO, message, null)
        }

        override fun error(message: String) {
            report(IrMessageLogger.Severity.ERROR, message, null)
        }

        override fun warning(message: String) {
            report(IrMessageLogger.Severity.WARNING, message, null)
        }

        override fun fatal(message: String): Nothing {
            report(IrMessageLogger.Severity.ERROR, message, null)
            kotlin.error("FATAL ERROR: $message")
        }
    }
}

fun generateKLib(
    depsDescriptors: ModulesStructure,
    irFactory: IrFactory,
    outputKlibPath: String,
    nopack: Boolean,
    verifySignatures: Boolean = true,
    abiVersion: KotlinAbiVersion = KotlinAbiVersion.CURRENT,
    jsOutputName: String?
) {
    val project = depsDescriptors.project
    val files = (depsDescriptors.mainModule as MainModule.SourceFiles).files
    val configuration = depsDescriptors.compilerConfiguration
    val allDependencies = depsDescriptors.allDependencies
    val incrementalDataProvider = configuration.get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)
    val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
    val messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None

    val icData: List<KotlinFileSerializedData>
    val serializedIrFiles: List<SerializedIrFile>?

    if (incrementalDataProvider != null) {
        val nonCompiledSources = files.map { VfsUtilCore.virtualToIoFile(it.virtualFile) to it }.toMap()
        val compiledIrFiles = incrementalDataProvider.serializedIrFiles
        val compiledMetaFiles = incrementalDataProvider.compiledPackageParts

        assert(compiledIrFiles.size == compiledMetaFiles.size)

        val storage = mutableListOf<KotlinFileSerializedData>()

        for (f in compiledIrFiles.keys) {
            if (f in nonCompiledSources) continue

            val irData = compiledIrFiles[f] ?: error("No Ir Data found for file $f")
            val metaFile = compiledMetaFiles[f] ?: error("No Meta Data found for file $f")
            val irFile = with(irData) {
                SerializedIrFile(fileData, String(fqn), f.path.replace('\\', '/'), types, signatures, strings, bodies, declarations, debugInfo)
            }
            storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
        }

        icData = storage
        serializedIrFiles = storage.map { it.irData }
    } else {
        icData = emptyList()
        serializedIrFiles = null
    }

    val (psi2IrContext, hasErrors) = preparePsi2Ir(depsDescriptors, errorPolicy, SymbolTable(IdSignatureDescriptor(JsManglerDesc), irFactory))
    val irBuiltIns = psi2IrContext.irBuiltIns

    val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
    val feContext = psi2IrContext.run {
        JsIrLinker.JsFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
    }
    val irLinker = JsIrLinker(
        psi2IrContext.moduleDescriptor,
        messageLogger,
        psi2IrContext.irBuiltIns,
        psi2IrContext.symbolTable,
        feContext,
        serializedIrFiles?.let { ICData(it, errorPolicy.allowErrors) }
    )

    sortDependencies(allDependencies, depsDescriptors.descriptors).map {
        irLinker.deserializeOnlyHeaderModule(depsDescriptors.getModuleDescriptor(it), it)
    }

    val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(project, files, irLinker, messageLogger, expectDescriptorToSymbol)

    if (verifySignatures) {
        moduleFragment.acceptVoid(ManglerChecker(JsManglerIr, Ir2DescriptorManglerAdapter(JsManglerDesc)))
    }
    if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
        val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
        irLinker.modules.forEach { fakeOverrideChecker.check(it) }
    }

    val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!

    if (!configuration.expectActualLinker) {
        moduleFragment.acceptVoid(ExpectDeclarationRemover(psi2IrContext.symbolTable, false))
    }

    serializeModuleIntoKlib(
        moduleName,
        project,
        configuration,
        messageLogger,
        psi2IrContext.bindingContext,
        files,
        outputKlibPath,
        allDependencies.map { it.library },
        moduleFragment,
        expectDescriptorToSymbol,
        icData,
        nopack,
        perFile = false,
        hasErrors,
        abiVersion,
        jsOutputName
    )
}

data class IrModuleInfo(
    val module: IrModuleFragment,
    val allDependencies: List<IrModuleFragment>,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: JsIrLinker,
    val moduleFragmentToUniqueName: Map<IrModuleFragment, String>,
    val loweredIrLoaded: Set<IrModuleFragment> = emptySet(),
)

private fun sortDependencies(resolvedDependencies: List<KotlinResolvedLibrary>, mapping: Map<KotlinLibrary, ModuleDescriptor>): Collection<KotlinLibrary> {
    val m2l = mapping.map { it.value to it.key }.toMap()
    val dependencies = resolvedDependencies.map { it.library }

    return DFS.topologicalOrder(dependencies) { m ->
        val descriptor = mapping[m] ?: error("No descriptor found for library ${m.libraryName}")
        descriptor.allDependencyModules.filter { it != descriptor }.map { m2l[it] }
    }.reversed()
}

interface LoweringsCacheProvider {
    fun cacheByPath(path: String): SerializedIcData?
}

object EmptyLoweringsCacheProvider : LoweringsCacheProvider {
    override fun cacheByPath(path: String): SerializedIcData? = null
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun loadIr(
    depsDescriptors: ModulesStructure,
    irFactory: IrFactory,
    verifySignatures: Boolean
): IrModuleInfo {
    val project = depsDescriptors.project
    val mainModule = depsDescriptors.mainModule
    val configuration = depsDescriptors.compilerConfiguration
    val allDependencies = depsDescriptors.allDependencies
    val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
    val messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None

    val signaturer = IdSignatureDescriptor(JsManglerDesc)
    val symbolTable = SymbolTable(signaturer, irFactory)

    when (mainModule) {
        is MainModule.SourceFiles -> {
            val (psi2IrContext, _) = preparePsi2Ir(depsDescriptors, errorPolicy, symbolTable)
            val irBuiltIns = psi2IrContext.irBuiltIns
            val feContext = psi2IrContext.run {
                JsIrLinker.JsFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
            }
            val moduleFragmentToUniqueName = mutableMapOf<IrModuleFragment, String>()
            val friendModules =
                mapOf(psi2IrContext.moduleDescriptor.name.asString() to depsDescriptors.friendDependencies.map { it.library.uniqueName })
            val irLinker =
                JsIrLinker(
                    psi2IrContext.moduleDescriptor,
                    messageLogger,
                    irBuiltIns,
                    symbolTable,
                    feContext,
                    null,
                    depsDescriptors.loweredIcData,
                    friendModules
                )
            val deserializedModuleFragments = sortDependencies(allDependencies, depsDescriptors.descriptors).map { klib ->
                irLinker.deserializeIrModuleHeader(
                    depsDescriptors.getModuleDescriptor(klib),
                    klib,
                    deserializationStrategy = DeserializationStrategy.EXPLICITLY_EXPORTED
                ).also { moduleFragment ->
                    klib.manifestProperties.getProperty(KLIB_PROPERTY_JS_OUTPUT_NAME)?.let {
                        moduleFragmentToUniqueName[moduleFragment] = it
                    }
                }
            }

            val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(project, mainModule.files, irLinker, messageLogger)
            symbolTable.noUnboundLeft("Unbound symbols left after linker")

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

            return IrModuleInfo(moduleFragment, deserializedModuleFragments, irBuiltIns, symbolTable, irLinker, moduleFragmentToUniqueName,
                                depsDescriptors.modulesWithCaches(deserializedModuleFragments))
        }
        is MainModule.Klib -> {
            val mainPath = File(mainModule.libPath).canonicalPath
            val mainModuleLib =
                depsDescriptors.allDependencies.find { it.library.libraryFile.canonicalPath == mainPath }?.library
                    ?: error("No module with ${mainModule.libPath} found")
            val moduleDescriptor = depsDescriptors.getModuleDescriptor(mainModuleLib)
            val typeTranslator =
                TypeTranslatorImpl(symbolTable, depsDescriptors.compilerConfiguration.languageVersionSettings, moduleDescriptor)
            val irBuiltIns = IrBuiltInsOverDescriptors(moduleDescriptor.builtIns, typeTranslator, symbolTable)

            val loweredIcData = if (!depsDescriptors.icUseStdlibCache && !depsDescriptors.icUseStdlibCache) emptyMap() else {
                val result = mutableMapOf<ModuleDescriptor, SerializedIcData>()

                for (lib in depsDescriptors.moduleDependencies.keys) {
                    val path = lib.libraryFile.absolutePath
                    val icData = depsDescriptors.loweringsCacheProvider.cacheByPath(path)
                    if (icData != null) {
                        val desc = depsDescriptors.getModuleDescriptor(lib)
                        result[desc] = icData
                    }
                }

                result
            }

            val friendModules = mapOf(mainModuleLib.uniqueName to depsDescriptors.friendDependencies.map { it.library.uniqueName })

            val irLinker =
                JsIrLinker(
                    null,
                    messageLogger,
                    irBuiltIns,
                    symbolTable,
                    null,
                    null,
                    loweredIcData,
                    friendModules
                )

            val moduleFragmentToUniqueName = mutableMapOf<IrModuleFragment, String>()

            val reachableDependencies = depsDescriptors.allResolvedDependencies.filterRoots {
                it.library.libraryFile.canonicalPath == mainPath
            }

            val deserializedModuleFragments = sortDependencies(reachableDependencies.getFullResolvedList(), depsDescriptors.descriptors).map { klib ->
                val strategy =
                    if (klib == mainModuleLib)
                        DeserializationStrategy.ALL
                    else
                        DeserializationStrategy.EXPLICITLY_EXPORTED

                irLinker.deserializeIrModuleHeader(depsDescriptors.getModuleDescriptor(klib), klib, strategy).also { moduleFragment ->
                    klib.manifestProperties.getProperty(KLIB_PROPERTY_JS_OUTPUT_NAME)?.let {
                        moduleFragmentToUniqueName[moduleFragment] = it
                    }
                }
            }

            val moduleFragment = deserializedModuleFragments.last()

            irLinker.init(null, emptyList())
            ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
            irLinker.postProcess()

            return IrModuleInfo(moduleFragment, deserializedModuleFragments, irBuiltIns, symbolTable, irLinker, moduleFragmentToUniqueName,
                                depsDescriptors.modulesWithCaches(deserializedModuleFragments))
        }
    }
}

fun prepareAnalyzedSourceModule(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    dependencies: List<String>,
    friendDependencies: List<String>,
    analyzer: AbstractAnalyzerWithCompilerReport,
    icUseGlobalSignatures: Boolean = false,
    icUseStdlibCache: Boolean = false,
    icCache: Map<String, SerializedIcData> = emptyMap(),
    errorPolicy: ErrorTolerancePolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT,
): ModulesStructure {
    val mainModule = MainModule.SourceFiles(files)
    val sourceModule = ModulesStructure(project, mainModule, configuration, dependencies, friendDependencies, icUseGlobalSignatures, icUseStdlibCache, icCache)
    return sourceModule.apply {
        runAnalysis(errorPolicy, analyzer)
    }
}

private fun preparePsi2Ir(
    depsDescriptors: ModulesStructure,
    errorIgnorancePolicy: ErrorTolerancePolicy,
    symbolTable: SymbolTable,
): Pair<GeneratorContext, Boolean> {
    val analysisResult = depsDescriptors.jsFrontEndResult
    val psi2Ir = Psi2IrTranslator(
        depsDescriptors.compilerConfiguration.languageVersionSettings,
        Psi2IrConfiguration(errorIgnorancePolicy.allowErrors)
    )
    return psi2Ir.createGeneratorContext(
        analysisResult.moduleDescriptor,
        analysisResult.bindingContext,
        symbolTable
    ) to analysisResult.hasErrors
}

fun GeneratorContext.generateModuleFragmentWithPlugins(
    project: Project,
    files: List<KtFile>,
    irLinker: IrDeserializer,
    messageLogger: IrMessageLogger,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
): IrModuleFragment {
    val psi2Ir = Psi2IrTranslator(languageVersionSettings, configuration)

    val extensions = IrGenerationExtension.getInstances(project)

    if (extensions.isNotEmpty()) {
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

        for (extension in extensions) {
            psi2Ir.addPostprocessingStep { module ->
                extension.generate(module, pluginContext)
            }
        }
    }

    return psi2Ir.generateModuleFragment(this, files, listOf(irLinker), extensions, expectDescriptorToSymbol)
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
    val icUseGlobalSignatures: Boolean,
    val icUseStdlibCache: Boolean,
    val icCache: Map<String, SerializedIcData>,
) {
    val loweringsCacheProvider: LoweringsCacheProvider = when {
        icUseStdlibCache -> object : LoweringsCacheProvider {
            override fun cacheByPath(path: String): SerializedIcData? {
                return icCache[path]
            }
        }
        icUseGlobalSignatures -> EmptyLoweringsCacheProvider
        else -> EmptyLoweringsCacheProvider
    }

    val allResolvedDependencies = jsResolveLibraries(
        dependencies,
        compilerConfiguration[JSConfigurationKeys.REPOSITORIES] ?: emptyList(),
        compilerConfiguration[IrMessageLogger.IR_MESSAGE_LOGGER].toResolverLogger()
    )

    val allDependencies = allResolvedDependencies.getFullResolvedList()

    val friendDependencies = allDependencies.run {
        val friendAbsolutePaths = friendDependenciesPaths.map { File(it).canonicalPath }
        filter {
            it.library.libraryFile.absolutePath in friendAbsolutePaths
        }
    }

    val moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>> = run {
        val transitives = allDependencies
        transitives.associate { klib ->
            klib.library to klib.resolvedDependencies.map { d -> d.library }
        }.toMap()
    }

    val builtInsDep = allDependencies.find { it.library.isBuiltIns }

    class JsFrontEndResult(val jsAnalysisResult: AnalysisResult, val hasErrors: Boolean) {
        val moduleDescriptor: ModuleDescriptor
            get() = jsAnalysisResult.moduleDescriptor

        val bindingContext: BindingContext
            get() = jsAnalysisResult.bindingContext
    }

    lateinit var jsFrontEndResult: JsFrontEndResult

    fun runAnalysis(errorPolicy: ErrorTolerancePolicy, analyzer: AbstractAnalyzerWithCompilerReport) {
        require(mainModule is MainModule.SourceFiles)
        val files = mainModule.files

        analyzer.analyzeAndReport(files) {
            TopDownAnalyzerFacadeForJSIR.analyzeFiles(
                files,
                project,
                compilerConfiguration,
                allDependencies.map { getModuleDescriptor(it.library) },
                friendDependencies.map { getModuleDescriptor(it.library) },
                analyzer.targetEnvironment,
                thisIsBuiltInsModule = builtInModuleDescriptor == null,
                customBuiltInsModule = builtInModuleDescriptor
            )
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val analysisResult = analyzer.analysisResult
        if (IncrementalCompilation.isEnabledForJs()) {
            /** can throw [IncrementalNextRoundException] */
            compareMetadataAndGoToNextICRoundIfNeeded(analysisResult, compilerConfiguration, project, files, errorPolicy.allowErrors)
        }

        var hasErrors = false
        if (analyzer.hasErrors() || analysisResult !is JsAnalysisResult) {
            if (!errorPolicy.allowErrors)
                throw CompilationErrorException()
            else hasErrors = true
        }

        hasErrors = TopDownAnalyzerFacadeForJSIR.checkForErrors(files, analysisResult.bindingContext, errorPolicy) || hasErrors

        jsFrontEndResult = JsFrontEndResult(analysisResult, hasErrors)
    }

    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    // TODO: these are roughly equivalent to KlibResolvedModuleDescriptorsFactoryImpl. Refactor me.
    val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()

    val loweredIcData = mutableMapOf<ModuleDescriptor, SerializedIcData>()

    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
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

        val dependencies = moduleDependencies.getValue(current).map { getModuleDescriptor(it) }
        md.setDependencies(listOf(md) + dependencies)

        loweringsCacheProvider.cacheByPath(current.libraryFile.absolutePath)?.let { icData ->
            loweredIcData[md] = icData
        }

        md
    }

    fun modulesWithCaches(allModules: Iterable<IrModuleFragment>): Set<IrModuleFragment> {
        return allModules.filter { it.descriptor in loweredIcData }.toSet()
    }

    val builtInModuleDescriptor =
        if (builtInsDep != null)
            getModuleDescriptor(builtInsDep.library)
        else
            null // null in case compiling builtInModule itself
}

private fun getDescriptorForElement(
    context: BindingContext,
    element: PsiElement
): DeclarationDescriptor = BindingContextUtils.getNotNull(context, BindingContext.DECLARATION_TO_DESCRIPTOR, element)

fun serializeModuleIntoKlib(
    moduleName: String,
    project: Project,
    configuration: CompilerConfiguration,
    messageLogger: IrMessageLogger,
    bindingContext: BindingContext,
    files: List<KtFile>,
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
) {
    assert(files.size == moduleFragment.files.size)

    val compatibilityMode = CompatibilityMode(abiVersion)

    val serializedIr =
        JsIrModuleSerializer(
            messageLogger,
            moduleFragment.irBuiltins,
            expectDescriptorToSymbol,
            compatibilityMode,
            skipExpects = !configuration.expectActualLinker
        ).serializedIrModule(moduleFragment)

    val moduleDescriptor = moduleFragment.descriptor
    val metadataSerializer = KlibMetadataIncrementalSerializer(configuration, project, containsErrorCode)

    val incrementalResultsConsumer = configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
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

    for ((ktFile, binaryFile) in files.zip(serializedIr.files)) {
        assert(ktFile.virtualFilePath == binaryFile.path) {
            """The Kt and Ir files are put in different order
                Kt: ${ktFile.virtualFilePath}
                Ir: ${binaryFile.path}
            """.trimMargin()
        }
        val packageFragment = metadataSerializer.serializeScope(ktFile, bindingContext, moduleDescriptor)
        val compiledKotlinFile = KotlinFileSerializedData(packageFragment.toByteArray(), binaryFile)

        additionalFiles += compiledKotlinFile
        processCompiledFileData(VfsUtilCore.virtualToIoFile(ktFile.virtualFile), compiledKotlinFile)
    }

    val compiledKotlinFiles = (cleanFiles + additionalFiles)

    val header = metadataSerializer.serializeHeader(
        moduleDescriptor,
        compiledKotlinFiles.map { it.irData.fqName }.distinct().sorted(),
        emptyList()
    ).toByteArray()
    incrementalResultsConsumer?.run {
        processHeader(header)
    }

    val serializedMetadata =
        metadataSerializer.serializedMetadata(
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
        irVersion = KlibIrVersion.INSTANCE.toString()
    )

    val properties = Properties().also { p ->
        if (jsOutputName != null) {
            p.setProperty(KLIB_PROPERTY_JS_OUTPUT_NAME, jsOutputName)
        }
        if (containsErrorCode) {
            p.setProperty(KLIB_PROPERTY_CONTAINS_ERROR_CODE, "true")
        }
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

private fun KlibMetadataIncrementalSerializer.serializeScope(
    ktFile: KtFile,
    bindingContext: BindingContext,
    moduleDescriptor: ModuleDescriptor
): ProtoBuf.PackageFragment {
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
    val nextRoundChecker = config.get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return
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

private fun KlibMetadataIncrementalSerializer(configuration: CompilerConfiguration, project: Project, allowErrors: Boolean) =
    KlibMetadataIncrementalSerializer(
        languageVersionSettings = configuration.languageVersionSettings,
        metadataVersion = configuration.metadataVersion,
        project = project,
        exportKDoc = false,
        skipExpects = !configuration.expectActualLinker,
        allowErrorTypes = allowErrors
    )

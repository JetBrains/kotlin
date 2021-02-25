/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.compiler.wjs

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.lower.ExpectDeclarationRemover
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.ICData
import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmCompilerResult
import org.jetbrains.kotlin.backend.wasm.generateJs
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
import org.jetbrains.kotlin.backend.wasm.wasmPhases
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.*
import org.jetbrains.kotlin.ir.backend.js.lower.generateTests
import org.jetbrains.kotlin.ir.backend.js.lower.moveBodilessDeclarationsToSeparatePlace
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.StageController
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.persistent.PersistentIrFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.linkage.IrDeserializer
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.DceRuntimeDiagnostic
import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.SerializedIrFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.IncrementalNextRoundException
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.TypeTranslatorImpl
import org.jetbrains.kotlin.psi2ir.generators.generateTypicalIrProviderList
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.Logger
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
import java.io.ByteArrayOutputStream

class Ir2WJCompiler(
    private val project: Project,
    private val configuration: CompilerConfiguration,
    private val analyzer: AbstractAnalyzerWithCompilerReport,
    private val dependencies: Collection<String>,
//    dependencies: KotlinLibraryResolveResult,
    private val friendDependencies: Collection<String>,
    logger: Logger // TODO: is ir loger instead
) {

    class Options {
        // Klib
        var nopack: Boolean = false
        var perFile: Boolean = false

        // Binary JS
        var generateFullJs: Boolean = true
        var generateDceJs: Boolean = false
        var dceDriven: Boolean = false
        var es6mode: Boolean = false
        var multiModule: Boolean = false
        var relativeRequirePath: Boolean = false
        var propertyLazyInitialization: Boolean = false
        var dceRuntimeDiagnostics: DceRuntimeDiagnostic? = null

        // Binary WASM



        // Persistent cache

    }

    val options: Options = Options()

    private fun irFactory(isPersistent: Boolean): IrFactory = if (isPersistent) PersistentIrFactory() else IrFactoryImpl

    private val klibMetadataFactories: KlibMetadataFactories = ModuleLoader.jsMetadataFactories

    private val moduleLoader: ModuleLoader by lazy {
        ModuleLoader(
            dependencies,
            klibMetadataFactories.DefaultDeserializedDescriptorFactory,
            configuration,
            LockBasedStorageManager("WJS IR Compiler"),
            logger
        )
    }

    private val languageVersionSettings: LanguageVersionSettings get() = configuration.languageVersionSettings

    private val expectActualLinker: Boolean
        get() = configuration.get(CommonConfigurationKeys.EXPECT_ACTUAL_LINKER) ?: false

    private val errorTolerancePolicy: ErrorTolerancePolicy =
        configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT

    private val messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None

    private val dependenciesRpo: List<KotlinLibrary> get() = moduleLoader.dependenciesRpo

    private val dependencyDescriptors: Map<String, ModuleDescriptorImpl> get() = moduleLoader.dependencyDescriptors
    private val friendDescriptors: Collection<ModuleDescriptorImpl> by lazy {
        friendDependencies.map {
            val library = moduleLoader.resolveModuleByPath(it) ?: error("Failed to find friend module with path $it")
            dependencyDescriptors[library.libraryName] ?: error("No module loaded for $it")
        }
    }
    private val builtInsModule: ModuleDescriptorImpl? get() = moduleLoader.builtInsModule

    private class FrontEndResult(val moduleDescriptor: ModuleDescriptor, val bindingContext: BindingContext, val hasErrors: Boolean)

    private fun doAnalysis(files: Collection<KtFile>): FrontEndResult {

        val frontend = TopDownAnalyzerFacadeForJSIR(klibMetadataFactories)

        analyzer.analyzeAndReport(files) {
            frontend.analyzeFiles(
                files,
                project,
                configuration,
                dependenciesRpo.map { dependencyDescriptors[it.libraryName]!! },
                friendModuleDescriptors = friendDescriptors,
                analyzer.targetEnvironment,
                thisIsBuiltInsModule = builtInsModule == null,
                customBuiltInsModule = builtInsModule
            )
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        val analysisResult = analyzer.analysisResult
        if (IncrementalCompilation.isEnabledForJs()) {
            /** can throw [IncrementalNextRoundException] */
            compareMetadataAndGoToNextICRoundIfNeeded(analysisResult, files)
        }

        var hasErrors = false
        if (analyzer.hasErrors() || analysisResult !is JsAnalysisResult) {
            if (!errorTolerancePolicy.allowErrors)
                throw CompilationErrorException()
            else hasErrors = true
        }

        hasErrors = frontend.checkForErrors(files, analysisResult.bindingContext, errorTolerancePolicy) || hasErrors

        return FrontEndResult(analysisResult.moduleDescriptor, analysisResult.bindingContext, hasErrors)
    }

    private fun compareMetadataAndGoToNextICRoundIfNeeded(analysisResult: AnalysisResult, files: Collection<KtFile>) {
        val nextRoundChecker = configuration.get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return
        val serializer = KlibMetadataIncrementalSerializer(configuration, project, errorTolerancePolicy.allowErrors)
        for (ktFile in files) {
            val packageFragment = serializer.serializeScope(ktFile, analysisResult.bindingContext, analysisResult.moduleDescriptor)
            // to minimize a number of IC rounds, we should inspect all proto for changes first,
            // then go to a next round if needed, with all new dirty files
            nextRoundChecker.checkProtoChanges(VfsUtilCore.virtualToIoFile(ktFile.virtualFile), packageFragment.toByteArray())
        }

        if (nextRoundChecker.shouldGoToNextRound()) throw IncrementalNextRoundException()
    }

    private fun createPsi2IrContext(frontEndResult: FrontEndResult, isPersistent: Boolean): GeneratorContext {
        val psi2Ir = Psi2IrTranslator(languageVersionSettings, Psi2IrConfiguration(errorTolerancePolicy.allowErrors))
        val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), irFactory(isPersistent))
        return psi2Ir.createGeneratorContext(frontEndResult.moduleDescriptor, frontEndResult.bindingContext, symbolTable).also {
            it.irBuiltIns.functionFactory = IrFunctionFactory(it.irBuiltIns, symbolTable)
        }
    }

    private fun createIrLinker(
        currentModule: ModuleDescriptor?,
        irBuiltIns: IrBuiltIns,
        symbolTable: SymbolTable,
        feContext: JsIrLinker.JsFePluginContext?,
        serializedIrFiles: List<SerializedIrFile>?,
        onlyHeaders: Boolean
    ): JsIrLinker {

        val irLinker = JsIrLinker(
            currentModule,
            messageLogger,
            irBuiltIns,
            symbolTable,
            feContext,
            serializedIrFiles?.let { ICData(it, errorTolerancePolicy.allowErrors) }
        )

        dependenciesRpo.forEach {
            val moduleDescriptor = dependencyDescriptors[it.libraryName]!!
            if (onlyHeaders)
                irLinker.deserializeOnlyHeaderModule(moduleDescriptor, it)
            else
                irLinker.deserializeIrModuleHeader(moduleDescriptor, it)
        }

        return irLinker
    }

    private fun createRawLinker(mainKlib: KotlinLibrary, isPersistent: Boolean): JsIrLinker {
        val mainModule = dependencyDescriptors[mainKlib.libraryName] ?: error("No descriptor for $mainKlib")
        val mangler = JsManglerDesc
        val signaturer = IdSignatureDescriptor(mangler)
        val symbolTable = SymbolTable(signaturer, irFactory(isPersistent))
        val typeTranslator = TypeTranslatorImpl(symbolTable, languageVersionSettings, mainModule)

        val irBuiltIns = IrBuiltIns(mainModule.builtIns, typeTranslator, symbolTable)
        val functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
        irBuiltIns.functionFactory = functionFactory

        return JsIrLinker(null, messageLogger, irBuiltIns, symbolTable, null, null).apply {
            dependenciesRpo.forEach {
                val strategy =
                    if (it.libraryName == mainKlib.libraryName)
                        DeserializationStrategy.ALL
                    else
                        DeserializationStrategy.EXPLICITLY_EXPORTED

                deserializeIrModuleHeader(dependencyDescriptors[it.libraryName]!!, it, strategy)
            }
        }
    }

    private fun GeneratorContext.generateModuleFragmentWithPlugins(
        files: Collection<KtFile>,
        irLinker: IrDeserializer,
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

    fun compileIntoPirCache() {
        TODO("Compile into Cache")
    }

    sealed class MainModule {
        class SourceFiles(val files: List<KtFile>) : MainModule()
        class Klib(val lib: String) : MainModule()
    }

    private fun doJsBackend(
        allModules: List<IrModuleFragment>,
        irLinker: JsIrLinker,
        phaseConfig: PhaseConfig,
        mainArguments: List<String>?,
        exportedDeclarations: Set<FqName>
    ): CompilerResult {
        val mainModule = allModules.last()
        val symbolTable = irLinker.symbolTable
        val factory = symbolTable.irFactory

        val context = JsIrBackendContext(
            mainModule.descriptor,
            irLinker.builtIns,
            symbolTable,
            allModules.first(),
            exportedDeclarations,
            configuration,
            es6mode = options.es6mode,
            propertyLazyInitialization = options.propertyLazyInitialization,
            irFactory = factory,
            dceRuntimeDiagnostic = options.dceRuntimeDiagnostics
        )

        // Load declarations referenced during `context` initialization
        val irProviders = listOf(irLinker)
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

        irLinker.postProcess()
        symbolTable.noUnboundLeft("Unbound symbols at the end of linker")

        allModules.forEach { module ->
            moveBodilessDeclarationsToSeparatePlace(context, module)
        }

        // TODO should be done incrementally
        generateTests(context, mainModule)

        if (options.dceDriven) {
            val controller = MutableController(context, pirLowerings)

            check(factory is PersistentIrFactory)

            factory.stageController = controller

            controller.currentStage = controller.lowerings.size + 1

            eliminateDeadDeclarations(allModules, context)

            factory.stageController = StageController(controller.currentStage)
        } else {
            jsPhases.invokeToplevel(phaseConfig, context, allModules)
        }

        val transformer = IrModuleToJsTransformer(
            context,
            mainArguments,
            fullJs = options.dceDriven || options.generateFullJs,
            dceJs = !options.dceDriven && options.generateDceJs,
            multiModule = options.multiModule,
            relativeRequirePath = options.relativeRequirePath
        )
        return transformer.generateModule(allModules)

    }

    private fun doWasmBackend(
        allModules: List<IrModuleFragment>,
        irLinker: JsIrLinker,
        phaseConfig: PhaseConfig,
        @Suppress("UNUSED_PARAMETER") mainArguments: List<String>?,
        exportedDeclarations: Set<FqName>
    ): WasmCompilerResult {

        val moduleFragment = allModules.last()
        val symbolTable = irLinker.symbolTable
        val moduleDescriptor = moduleFragment.descriptor
        val context =
            WasmBackendContext(moduleDescriptor, irLinker.builtIns, symbolTable, moduleFragment, exportedDeclarations, configuration)

        // Load declarations referenced during `context` initialization
        allModules.forEach {
            val irProviders = generateTypicalIrProviderList(it.descriptor, irLinker.builtIns, symbolTable, irLinker)
            ExternalDependenciesGenerator(symbolTable, irProviders)
                .generateUnboundSymbolsAsDependencies()
        }

        val irFiles = allModules.flatMap { it.files }

        moduleFragment.files.clear()
        moduleFragment.files += irFiles

        // Create stubs
        val irProviders = generateTypicalIrProviderList(moduleDescriptor, irLinker.builtIns, symbolTable, irLinker)
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
        moduleFragment.patchDeclarationParents()

        wasmPhases.invokeToplevel(phaseConfig, context, moduleFragment)

        val compiledWasmModule = WasmCompiledModuleFragment()
        val codeGenerator = WasmModuleFragmentGenerator(context, compiledWasmModule)
        codeGenerator.generateModule(moduleFragment)

        val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
        val watGenerator = WasmIrToText()
        watGenerator.appendWasmModule(linkedModule)
        val wat = watGenerator.toString()

        val js = compiledWasmModule.generateJs()

        val os = ByteArrayOutputStream()
        WasmIrToBinary(os, linkedModule).appendWasmModule()
        val byteArray = os.toByteArray()

        return WasmCompilerResult(
            wat = wat,
            js = js,
            wasm = byteArray
        )
    }

    fun compileBinaryWasm(
        mainModule: MainModule,
        phaseConfig: PhaseConfig,
        mainArguments: List<String>?,
        exportedDeclarations: Set<FqName>
    ): WasmCompilerResult {
        return compileBinary(mainModule) { modules, linker ->
            doWasmBackend(modules, linker, phaseConfig, mainArguments, exportedDeclarations)
        }
    }

    fun compileBinaryJs(
        mainModule: MainModule,
        phaseConfig: PhaseConfig,
        mainArguments: List<String>?,
        exportedDeclarations: Set<FqName> = emptySet()
    ): CompilerResult {
        return compileBinary(mainModule) { modules, linker ->
            doJsBackend(modules, linker, phaseConfig, mainArguments, exportedDeclarations)
        }
    }

    private fun <T> compileBinary(mainModule: MainModule, backend: (List<IrModuleFragment>, JsIrLinker) -> T): T {
        val irLinker: JsIrLinker
        val allModuleFragments = when (mainModule) {
            is MainModule.SourceFiles -> {
                val files = mainModule.files
                val frontEndResult = doAnalysis(mainModule.files)
                val psi2IrContext = createPsi2IrContext(frontEndResult, options.dceDriven)
                irLinker = psi2IrContext.run {
                    val feContext = JsIrLinker.JsFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
                    createIrLinker(moduleDescriptor, irBuiltIns, symbolTable, feContext, null, onlyHeaders = false)
                }
                val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(files, irLinker)
                mutableListOf<IrModuleFragment>().apply {
                    addAll(irLinker.modules)
                    add(moduleFragment)
                }
            }
            is MainModule.Klib -> {
                val mainKlib = moduleLoader.resolveModuleByPath(mainModule.lib) ?: error("No module with path ${mainModule.lib} found")
                irLinker = createRawLinker(mainKlib, options.dceDriven)
                irLinker.run {
                    init(null, emptyList())
                    ExternalDependenciesGenerator(symbolTable, listOf(this)).generateUnboundSymbolsAsDependencies()
                    postProcess()
                    modules
                }
            }
        }

        return backend(allModuleFragments, irLinker)
    }

    // Klibs

    private class ICCacheData(
        val icData: List<KotlinFileSerializedData>,
        val serializedIrFiles: List<SerializedIrFile>?)

    private fun loadICdataIfExists(files: Collection<KtFile>): ICCacheData {
        val incrementalDataProvider = configuration.get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)
        return if (incrementalDataProvider != null) {
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
                    SerializedIrFile(fileData, String(fqn), f.path.replace('\\', '/'), types, signatures, strings, bodies, declarations)
                }
                storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
            }

            ICCacheData(storage, storage.map { it.irData })
        } else {
            ICCacheData(emptyList(), null)
        }
    }

    fun compileKlib(files: Collection<KtFile>, outputKlibPath: String) {

        val icCacheData = loadICdataIfExists(files)

        val frontEndResult = doAnalysis(files)
        val psi2IrContext = createPsi2IrContext(frontEndResult, isPersistent = false)

        val irLinker = psi2IrContext.run {
            val feContext = JsIrLinker.JsFePluginContext(moduleDescriptor, symbolTable, typeTranslator, irBuiltIns)
            createIrLinker(moduleDescriptor, irBuiltIns, symbolTable, feContext, icCacheData.serializedIrFiles, onlyHeaders = true)
        }

        val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()
        val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(files, irLinker, expectDescriptorToSymbol)

        moduleFragment.acceptVoid(ManglerChecker(JsManglerIr, Ir2DescriptorManglerAdapter(JsManglerDesc)))
        if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
            val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
            irLinker.modules.forEach { fakeOverrideChecker.check(it) }
        }

        val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!

        if (!expectActualLinker) {
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
            dependenciesRpo,
            moduleFragment,
            expectDescriptorToSymbol,
            icCacheData.icData,
            configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER),
            options.nopack,
            options.perFile,
            frontEndResult.hasErrors
        )
    }
}
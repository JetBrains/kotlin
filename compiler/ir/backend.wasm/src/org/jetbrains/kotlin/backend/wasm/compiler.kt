/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm


import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
import org.jetbrains.kotlin.backend.wasm.ir2wasm.generateStringLiteralsSupport

//
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.vfs.VfsUtilCore
//import org.jetbrains.kotlin.analyzer.AbstractAnalyzerWithCompilerReport
//import org.jetbrains.kotlin.analyzer.AnalysisResult
//import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
//import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
//import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideChecker
//import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
//import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
//import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
//import org.jetbrains.kotlin.backend.common.serialization.knownBuiltins
//import org.jetbrains.kotlin.backend.common.serialization.mangle.ManglerChecker
//import org.jetbrains.kotlin.backend.common.serialization.mangle.descriptor.Ir2DescriptorManglerAdapter
//import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
//import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
//import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmCompiledModuleFragment
//import org.jetbrains.kotlin.backend.wasm.ir2wasm.WasmModuleFragmentGenerator
//import org.jetbrains.kotlin.backend.wasm.ir2wasm.generateStringLiteralsSupport
//import org.jetbrains.kotlin.builtins.KotlinBuiltIns
//import org.jetbrains.kotlin.config.*
//import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
//import org.jetbrains.kotlin.descriptors.ModuleDescriptor
//import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
//import org.jetbrains.kotlin.incremental.components.LookupTracker
//import org.jetbrains.kotlin.ir.backend.js.*
//import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
//import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerDesc
//import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsManglerIr
//import org.jetbrains.kotlin.ir.declarations.IrFactory
//import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
//import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
//import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
//import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
//import org.jetbrains.kotlin.ir.linkage.IrDeserializer
//import org.jetbrains.kotlin.ir.symbols.IrSymbol
//import org.jetbrains.kotlin.ir.util.*
//import org.jetbrains.kotlin.ir.visitors.acceptVoid
//import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
//import org.jetbrains.kotlin.js.config.ErrorTolerancePolicy
//import org.jetbrains.kotlin.js.config.JSConfigurationKeys
//import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
//import org.jetbrains.kotlin.library.KotlinLibrary
//import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
//import org.jetbrains.kotlin.library.unresolvedDependencies
//import org.jetbrains.kotlin.name.FqName
//import org.jetbrains.kotlin.progress.IncrementalNextRoundException
//import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
//import org.jetbrains.kotlin.psi.KtFile
//import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
//import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
//import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
//import org.jetbrains.kotlin.resolve.BindingContext
//import org.jetbrains.kotlin.storage.LockBasedStorageManager
//import org.jetbrains.kotlin.storage.StorageManager
//import org.jetbrains.kotlin.utils.DFS
//import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToBinary
//import org.jetbrains.kotlin.wasm.ir.convertors.WasmIrToText
//import java.io.ByteArrayOutputStream
//
class WasmCompilerResult(val wat: String, val js: String, val wasm: ByteArray)
//
//private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}
//internal val JsFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)
//
//private class ModulesStructure(
//    private val project: Project,
//    private val mainModule: MainModule,
//    private val analyzer: AbstractAnalyzerWithCompilerReport,
//    val compilerConfiguration: CompilerConfiguration,
//    val allDependencies: KotlinLibraryResolveResult,
//    private val friendDependencies: List<KotlinLibrary>
//) {
//    val moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>> = run {
//        val transitives = allDependencies.getFullResolvedList()
//        transitives.associate { klib ->
//            klib.library to klib.resolvedDependencies.map { d -> d.library }
//        }.toMap()
//    }
//
//    val builtInsDep = allDependencies.getFullList().find { it.isBuiltIns }
//
//    class JsFrontEndResult(val moduleDescriptor: ModuleDescriptor, val bindingContext: BindingContext, val hasErrors: Boolean)
//
//    fun runAnalysis(errorPolicy: ErrorTolerancePolicy): JsFrontEndResult {
//        require(mainModule is MainModule.SourceFiles)
//        val files = mainModule.files
//
//        val frontend = TopDownAnalyzerFacadeForJSIR(JsFactories)
//
//        analyzer.analyzeAndReport(files) {
//            frontend.analyzeFiles(
//                files,
//                project,
//                compilerConfiguration,
//                allDependencies.getFullList().map { getModuleDescriptor(it) },
//                friendModuleDescriptors = friendDependencies.map { getModuleDescriptor(it) },
//                thisIsBuiltInsModule = builtInModuleDescriptor == null,
//                customBuiltInsModule = builtInModuleDescriptor
//            )
//        }
//
//        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
//
//        val analysisResult = analyzer.analysisResult
//        if (IncrementalCompilation.isEnabledForJs()) {
//            /** can throw [IncrementalNextRoundException] */
//            compareMetadataAndGoToNextICRoundIfNeeded(analysisResult, compilerConfiguration, project, files, errorPolicy.allowErrors)
//        }
//
//        var hasErrors = false
//        if (analyzer.hasErrors() || analysisResult !is JsAnalysisResult) {
//            if (!errorPolicy.allowErrors)
//                throw AnalysisResult.CompilationErrorException()
//            else hasErrors = true
//        }
//
//        hasErrors = frontend.checkForErrors(files, analysisResult.bindingContext, errorPolicy) || hasErrors
//
//        return JsFrontEndResult(analysisResult.moduleDescriptor, analysisResult.bindingContext, hasErrors)
//    }
//
//    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings
//
//    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
//    private var runtimeModule: ModuleDescriptorImpl? = null
//
//    // TODO: these are roughly equivalent to KlibResolvedModuleDescriptorsFactoryImpl. Refactor me.
//    val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()
//
//    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
//        val isBuiltIns = current.unresolvedDependencies.isEmpty()
//
//        val lookupTracker = compilerConfiguration[CommonConfigurationKeys.LOOKUP_TRACKER] ?: LookupTracker.DO_NOTHING
//        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
//            current,
//            languageVersionSettings,
//            storageManager,
//            runtimeModule?.builtIns,
//            packageAccessHandler = null, // TODO: This is a speed optimization used by Native. Don't bother for now.
//            lookupTracker = lookupTracker
//        )
//        if (isBuiltIns) runtimeModule = md
//
//        val dependencies = moduleDependencies.getValue(current).map { getModuleDescriptor(it) }
//        md.setDependencies(listOf(md) + dependencies)
//        md
//    }
//
//    val builtInModuleDescriptor =
//        if (builtInsDep != null)
//            getModuleDescriptor(builtInsDep)
//        else
//            null // null in case compiling builtInModule itself
//}
//
//private fun compareMetadataAndGoToNextICRoundIfNeeded(
//    analysisResult: AnalysisResult,
//    config: CompilerConfiguration,
//    project: Project,
//    files: List<KtFile>,
//    allowErrors: Boolean
//) {
//    val nextRoundChecker = config.get(JSConfigurationKeys.INCREMENTAL_NEXT_ROUND_CHECKER) ?: return
//    val bindingContext = analysisResult.bindingContext
//    val serializer = KlibMetadataIncrementalSerializer(config, project, allowErrors)
//    for (ktFile in files) {
//        val packageFragment = serializer.serializeScope(ktFile, bindingContext, analysisResult.moduleDescriptor)
//        // to minimize a number of IC rounds, we should inspect all proto for changes first,
//        // then go to a next round if needed, with all new dirty files
//        nextRoundChecker.checkProtoChanges(VfsUtilCore.virtualToIoFile(ktFile.virtualFile), packageFragment.toByteArray())
//    }
//
//    if (nextRoundChecker.shouldGoToNextRound()) throw IncrementalNextRoundException()
//}
//
//private fun runAnalysisAndPreparePsi2Ir(
//    depsDescriptors: ModulesStructure,
//    irFactory: IrFactory,
//    errorIgnorancePolicy: ErrorTolerancePolicy
//): Pair<GeneratorContext, Boolean> {
//    val analysisResult = depsDescriptors.runAnalysis(errorIgnorancePolicy)
//    val psi2Ir = Psi2IrTranslator(
//        depsDescriptors.compilerConfiguration.languageVersionSettings,
//        Psi2IrConfiguration(errorIgnorancePolicy.allowErrors)
//    )
//    val symbolTable = SymbolTable(IdSignatureDescriptor(JsManglerDesc), irFactory)
//    return psi2Ir.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext, symbolTable) to analysisResult.hasErrors
//}
//
//fun GeneratorContext.generateModuleFragmentWithPlugins(
//    project: Project,
//    files: Collection<KtFile>,
//    irLinker: IrDeserializer,
//    messageLogger: IrMessageLogger,
//    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
//): IrModuleFragment {
//    val psi2Ir = Psi2IrTranslator(languageVersionSettings, configuration)
//
//    val extensions = IrGenerationExtension.getInstances(project)
//
//    if (extensions.isNotEmpty()) {
//        // plugin context should be instantiated before postprocessing steps
//        val pluginContext = IrPluginContextImpl(
//            moduleDescriptor,
//            bindingContext,
//            languageVersionSettings,
//            symbolTable,
//            typeTranslator,
//            irBuiltIns,
//            linker = irLinker,
//            messageLogger
//        )
//
//        for (extension in extensions) {
//            psi2Ir.addPostprocessingStep { module ->
//                extension.generate(module, pluginContext)
//            }
//        }
//    }
//
//    return psi2Ir.generateModuleFragment(this, files, listOf(irLinker), extensions, expectDescriptorToSymbol)
//}
//
//
//private fun sortDependencies(dependencies: List<KotlinLibrary>, mapping: Map<KotlinLibrary, ModuleDescriptor>): Collection<KotlinLibrary> {
//    val m2l = mapping.map { it.value to it.key }.toMap()
//
//    return DFS.topologicalOrder(dependencies) { m ->
//        val descriptor = mapping[m] ?: error("No descriptor found for library ${m.libraryName}")
//        descriptor.allDependencyModules.filter { it != descriptor }.map { m2l[it] }
//    }.reversed()
//}
//
//private fun loadIr(
//    project: Project,
//    mainModule: MainModule,
//    analyzer: AbstractAnalyzerWithCompilerReport,
//    configuration: CompilerConfiguration,
//    allDependencies: KotlinLibraryResolveResult,
//    friendDependencies: List<KotlinLibrary>,
//    irFactory: IrFactory,
//): IrModuleInfo {
//    val depsDescriptors = ModulesStructure(project, mainModule, analyzer, configuration, allDependencies, friendDependencies)
//    val errorPolicy = configuration.get(JSConfigurationKeys.ERROR_TOLERANCE_POLICY) ?: ErrorTolerancePolicy.DEFAULT
//    val messageLogger = configuration.get(IrMessageLogger.IR_MESSAGE_LOGGER) ?: IrMessageLogger.None
//
//    when (mainModule) {
//        is MainModule.SourceFiles -> {
//            val (psi2IrContext, _) = runAnalysisAndPreparePsi2Ir(depsDescriptors, irFactory, errorPolicy)
//            val irBuiltIns = psi2IrContext.irBuiltIns
//            val symbolTable = psi2IrContext.symbolTable
//            val functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
//            irBuiltIns.functionFactory = functionFactory
//            val feContext = psi2IrContext.run {
//                JsIrLinker.JsFePluginContext(moduleDescriptor, bindingContext, symbolTable, typeTranslator, irBuiltIns)
//            }
//            val irLinker = JsIrLinker(psi2IrContext.moduleDescriptor, messageLogger, irBuiltIns, symbolTable, feContext, null)
//            val deserializedModuleFragments = sortDependencies(allDependencies.getFullList(), depsDescriptors.descriptors).map {
//                irLinker.deserializeIrModuleHeader(depsDescriptors.getModuleDescriptor(it), it)
//            }
//
//            val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(project, mainModule.files, irLinker, messageLogger)
//            symbolTable.noUnboundLeft("Unbound symbols left after linker")
//
//            // TODO: not sure whether this check should be enabled by default. Add configuration key for it.
//            val mangleChecker = ManglerChecker(JsManglerIr, Ir2DescriptorManglerAdapter(JsManglerDesc))
//            moduleFragment.acceptVoid(mangleChecker)
//
//            if (configuration.getBoolean(JSConfigurationKeys.FAKE_OVERRIDE_VALIDATOR)) {
//                val fakeOverrideChecker = FakeOverrideChecker(JsManglerIr, JsManglerDesc)
//                irLinker.modules.forEach { fakeOverrideChecker.check(it) }
//            }
//
//            irBuiltIns.knownBuiltins.forEach { it.acceptVoid(mangleChecker) }
//
//            return IrModuleInfo(moduleFragment, deserializedModuleFragments, irBuiltIns, symbolTable, irLinker)
//        }
//        is MainModule.Klib -> {
//            val moduleDescriptor = depsDescriptors.getModuleDescriptor(mainModule.lib)
//            val mangler = JsManglerDesc
//            val signaturer = IdSignatureDescriptor(mangler)
//            val symbolTable = SymbolTable(signaturer, irFactory)
//            val constantValueGenerator = ConstantValueGenerator(moduleDescriptor, symbolTable)
//            val typeTranslator = TypeTranslator(
//                symbolTable,
//                depsDescriptors.compilerConfiguration.languageVersionSettings,
//                builtIns = moduleDescriptor.builtIns
//            )
//            typeTranslator.constantValueGenerator = constantValueGenerator
//            constantValueGenerator.typeTranslator = typeTranslator
//            val irBuiltIns = IrBuiltIns(moduleDescriptor.builtIns, typeTranslator, symbolTable)
//            val functionFactory = IrFunctionFactory(irBuiltIns, symbolTable)
//            irBuiltIns.functionFactory = functionFactory
//            val irLinker =
//                JsIrLinker(null, messageLogger, irBuiltIns, symbolTable, null, null)
//
//            val deserializedModuleFragments = sortDependencies(allDependencies.getFullList(), depsDescriptors.descriptors).map {
//                val strategy =
//                    if (it == mainModule.lib)
//                        DeserializationStrategy.ALL
//                    else
//                        DeserializationStrategy.EXPLICITLY_EXPORTED
//
//                irLinker.deserializeIrModuleHeader(depsDescriptors.getModuleDescriptor(it), it, strategy)
//            }
//
//            val moduleFragment = deserializedModuleFragments.last()
//
//            irLinker.init(null, emptyList())
//            ExternalDependenciesGenerator(symbolTable, listOf(irLinker)).generateUnboundSymbolsAsDependencies()
//            irLinker.postProcess()
//
//            return IrModuleInfo(moduleFragment, deserializedModuleFragments, irBuiltIns, symbolTable, irLinker)
//        }
//    }
//}
//
//
//fun compileWasm(
//    project: Project,
//    mainModule: MainModule,
//    analyzer: AbstractAnalyzerWithCompilerReport,
//    configuration: CompilerConfiguration,
//    phaseConfig: PhaseConfig,
//    allDependencies: KotlinLibraryResolveResult,
//    friendDependencies: List<KotlinLibrary>,
//    exportedDeclarations: Set<FqName> = emptySet()
//): WasmCompilerResult {
//    val (moduleFragment, dependencyModules, irBuiltIns, symbolTable, deserializer) =
//        loadIr(
//            project, mainModule, analyzer, configuration, allDependencies, friendDependencies,
//            IrFactoryImpl
//        )
//
//    val allModules = when (mainModule) {
//        is MainModule.SourceFiles -> dependencyModules + listOf(moduleFragment)
//        is MainModule.Klib -> dependencyModules
//    }
//
//    val moduleDescriptor = moduleFragment.descriptor
//    val context = WasmBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, exportedDeclarations, configuration)
//
//    // Load declarations referenced during `context` initialization
//    allModules.forEach {
//        val irProviders = generateTypicalIrProviderList(it.descriptor, irBuiltIns, symbolTable, deserializer)
//        ExternalDependenciesGenerator(symbolTable, irProviders)
//            .generateUnboundSymbolsAsDependencies()
//    }
//
//    val irFiles = allModules.flatMap { it.files }
//
//    moduleFragment.files.clear()
//    moduleFragment.files += irFiles
//
//    // Create stubs
//    val irProviders = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable, deserializer)
//    ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()
//    moduleFragment.patchDeclarationParents()
//
//    wasmPhases.invokeToplevel(phaseConfig, context, moduleFragment)
//
//    val compiledWasmModule = WasmCompiledModuleFragment()
//    val codeGenerator = WasmModuleFragmentGenerator(context, compiledWasmModule)
//    codeGenerator.generateModule(moduleFragment)
//
//    val linkedModule = compiledWasmModule.linkWasmCompiledFragments()
//    val watGenerator = WasmIrToText()
//    watGenerator.appendWasmModule(linkedModule)
//    val wat = watGenerator.toString()
//
//    val js = compiledWasmModule.generateJs()
//
//    val os = ByteArrayOutputStream()
//    WasmIrToBinary(os, linkedModule).appendWasmModule()
//    val byteArray = os.toByteArray()
//
//    return WasmCompilerResult(
//        wat = wat,
//        js = js,
//        wasm = byteArray
//    )
//}
//
//
fun WasmCompiledModuleFragment.generateJs(): String {
    val runtime = """
    const runtime = {
        String_getChar(str, index) {
            return str.charCodeAt(index);
        },

        String_compareTo(str1, str2) {
            if (str1 > str2) return 1;
            if (str1 < str2) return -1;
            return 0;
        },

        String_equals(str, other) {
            return str === other;
        },

        String_subsequence(str, startIndex, endIndex) {
            return str.substring(startIndex, endIndex);
        },

        String_getLiteral(index) {
            return runtime.stringLiterals[index];
        },

        coerceToString(value) {
            return String(value);
        },

        Char_toString(char) {
            return String.fromCharCode(char)
        },

        identity(x) {
            return x;
        },

        println(value) {
            console.log(">>>  " + value)
        }
    };
    """.trimIndent()

    val jsCode =
        "\nconst js_code = {${jsFuns.joinToString(",\n") { "\"" + it.importName + "\" : " + it.jsCode }}};"

    return runtime + generateStringLiteralsSupport(stringLiterals) + jsCode
}

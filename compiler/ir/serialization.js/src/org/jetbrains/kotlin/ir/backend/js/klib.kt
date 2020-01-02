/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.KlibIrVersion
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsMangler
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.KlibMetadataIncrementalSerializer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.ExpectDeclarationRemover
import org.jetbrains.kotlin.ir.util.IrDeserializer
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.generateTypicalIrProviderList
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.buildKoltinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
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

fun loadKlib(klibPath: String) =
    createKotlinLibrary(KFile(KFile(klibPath).absolutePath))

val emptyLoggingContext = object : LoggingContext {
    override var inVerbosePhase = false

    override fun log(message: () -> String) {}
}

private val CompilerConfiguration.metadataVersion
    get() = get(CommonConfigurationKeys.METADATA_VERSION) as? KlibMetadataVersion ?: KlibMetadataVersion.INSTANCE

private val CompilerConfiguration.klibMpp: Boolean
    get() = get(CommonConfigurationKeys.KLIB_MPP) ?: false

class KotlinFileSerializedData(val metadata: ByteArray, val irData: SerializedIrFile)

fun generateKLib(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>,
    outputKlibPath: String,
    nopack: Boolean
) {
    val incrementalDataProvider = configuration.get(JSConfigurationKeys.INCREMENTAL_DATA_PROVIDER)

    val icData: List<KotlinFileSerializedData>

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
                SerializedIrFile(fileData, String(fqn), f.path.replace('\\', '/'), symbols, types, strings, bodies, declarations)
            }
            storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
        }

        icData = storage
    } else {
        icData = emptyList()
    }

    val depsDescriptors = ModulesStructure(project, files, configuration, allDependencies, friendDependencies)

    val psi2IrContext = runAnalysisAndPreparePsi2Ir(depsDescriptors)

    val expectDescriptorToSymbol = mutableMapOf<DeclarationDescriptor, IrSymbol>()

    val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(project, files,
        deserializer = null, expectDescriptorToSymbol = expectDescriptorToSymbol)

    val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!

    if (!configuration.klibMpp) {
        moduleFragment.acceptVoid(ExpectDeclarationRemover(psi2IrContext.symbolTable, doRemove = false, keepOptionalAnnotations = false))
    }

    serializeModuleIntoKlib(
        moduleName,
        configuration,
        psi2IrContext.bindingContext,
        files,
        outputKlibPath,
        allDependencies.getFullList(),
        moduleFragment,
        expectDescriptorToSymbol,
        icData,
        nopack
    )
}

data class IrModuleInfo(
    val module: IrModuleFragment,
    val allDependencies: List<IrModuleFragment>,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: JsIrLinker
)

private fun sortDependencies(dependencies: List<KotlinLibrary>, mapping: Map<KotlinLibrary, ModuleDescriptor>): Collection<KotlinLibrary> {
    val m2l = mapping.map { it.value to it.key }.toMap()

    return DFS.topologicalOrder(dependencies) { m ->
        val descriptor = mapping[m] ?: error("No descriptor found for library ${m.libraryName}")
        descriptor.allDependencyModules.filter { it != descriptor }.map { m2l[it] }
    }.reversed()
}

fun loadIr(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    allDependencies: KotlinLibraryResolveResult,
    friendDependencies: List<KotlinLibrary>
): IrModuleInfo {
    val depsDescriptors = ModulesStructure(project, files, configuration, allDependencies, friendDependencies)

    val psi2IrContext = runAnalysisAndPreparePsi2Ir(depsDescriptors)

    val irBuiltIns = psi2IrContext.irBuiltIns
    val symbolTable = psi2IrContext.symbolTable
    val moduleDescriptor = psi2IrContext.moduleDescriptor

    val deserializer = JsIrLinker(moduleDescriptor, JsMangler, emptyLoggingContext, irBuiltIns, symbolTable)

    val deserializedModuleFragments = sortDependencies(allDependencies.getFullList(), depsDescriptors.descriptors).map {
        deserializer.deserializeIrModuleHeader(depsDescriptors.getModuleDescriptor(it))!!
    }

    deserializer.initializeExpectActualLinker()

    val moduleFragment = psi2IrContext.generateModuleFragmentWithPlugins(project, files, deserializer)

    return IrModuleInfo(moduleFragment, deserializedModuleFragments, irBuiltIns, symbolTable, deserializer)
}

private fun runAnalysisAndPreparePsi2Ir(depsDescriptors: ModulesStructure): GeneratorContext {
    val analysisResult = depsDescriptors.runAnalysis()

    return GeneratorContext(
        Psi2IrConfiguration(),
        analysisResult.moduleDescriptor,
        analysisResult.bindingContext,
        depsDescriptors.compilerConfiguration.languageVersionSettings,
        SymbolTable(),
        JsGeneratorExtensions()
    )
}

fun GeneratorContext.generateModuleFragmentWithPlugins(
    project: Project,
    files: List<KtFile>,
    deserializer: IrDeserializer? = null,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null
): IrModuleFragment {
    val irProviders = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable, deserializer)
    val psi2Ir = Psi2IrTranslator(languageVersionSettings, configuration, mangler = JsMangler)

    for (extension in IrGenerationExtension.getInstances(project)) {
        psi2Ir.addPostprocessingStep { module ->
            extension.generate(
                module,
                IrPluginContext(
                    moduleDescriptor,
                    bindingContext,
                    languageVersionSettings,
                    symbolTable,
                    typeTranslator,
                    irBuiltIns
                )
            )
        }
    }

    val moduleFragment =
        psi2Ir.generateModuleFragment(
            this,
            files,
            irProviders,
            expectDescriptorToSymbol
        )
    return moduleFragment
}

fun GeneratorContext.generateModuleFragment(files: List<KtFile>, deserializer: IrDeserializer? = null, expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>? = null): IrModuleFragment {
    val irProviders = generateTypicalIrProviderList(moduleDescriptor, irBuiltIns, symbolTable, deserializer)
    return Psi2IrTranslator(
        languageVersionSettings, configuration, mangler = JsMangler
    ).generateModuleFragment(this, files, irProviders, expectDescriptorToSymbol)
}


private fun createBuiltIns(storageManager: StorageManager) = object : KotlinBuiltIns(storageManager) {}
internal val JsFactories = KlibMetadataFactories(::createBuiltIns, DynamicTypeDeserializer)

fun getModuleDescriptorByLibrary(current: KotlinLibrary, mapping: Map<String, ModuleDescriptorImpl>): ModuleDescriptorImpl {
    val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
        current,
        LanguageVersionSettingsImpl.DEFAULT,
        LockBasedStorageManager.NO_LOCKS,
        null,
        packageAccessHandler = null // TODO: This is a speed optimization used by Native. Don't bother for now.
    )
//    if (isBuiltIns) runtimeModule = md

    val dependencies = current.manifestProperties.propertyList(KLIB_PROPERTY_DEPENDS, escapeInQuotes = true).map { mapping.getValue(it) }

    md.setDependencies(listOf(md) + dependencies)
    return md
}

object JsIrCompilationError : Throwable()

private class ModulesStructure(
    private val project: Project,
    private val files: List<KtFile>,
    val compilerConfiguration: CompilerConfiguration,
    val allDependencies: KotlinLibraryResolveResult,
    private val friendDependencies: List<KotlinLibrary>
) {
    val moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>> = run {
        val result = mutableMapOf<KotlinLibrary, List<KotlinLibrary>>()

        allDependencies.forEach { klib, _ ->
            val dependencies = allDependencies.filterRoots {
                it.library == klib
            }.getFullList(TopologicalLibraryOrder)
            result.put(klib, dependencies.minus(klib))
        }
        result
    }

    val builtInsDep = allDependencies.getFullList().find { it.isBuiltIns }

    fun runAnalysis(): JsAnalysisResult {
        // TODO: Should we not provide default message collector?
        val messageCollector: MessageCollector =
            compilerConfiguration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY) ?: MessageCollector.NONE

        val analyzerWithCompilerReport = AnalyzerWithCompilerReport(
            messageCollector,
            compilerConfiguration.languageVersionSettings
        )

        analyzerWithCompilerReport.analyzeAndReport(files) {
            TopDownAnalyzerFacadeForJSIR.analyzeFiles(
                files,
                project,
                compilerConfiguration,
                allDependencies.getFullList().map { getModuleDescriptor(it) },
                friendModuleDescriptors = friendDependencies.map { getModuleDescriptor(it) },
                thisIsBuiltInsModule = builtInModuleDescriptor == null,
                customBuiltInsModule = builtInModuleDescriptor
            )
        }

        val analysisResult = analyzerWithCompilerReport.analysisResult

        if (analyzerWithCompilerReport.hasErrors() || analysisResult !is JsAnalysisResult)
            throw JsIrCompilationError

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        TopDownAnalyzerFacadeForJSIR.checkForErrors(files, analysisResult.bindingContext)

        return analysisResult
    }

    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    // TODO: these are roughly equivalent to KlibResolvedModuleDescriptorsFactoryImpl. Refactor me.
    val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()

    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
        val isBuiltIns = current.unresolvedDependencies.isEmpty()

        val md = JsFactories.DefaultDeserializedDescriptorFactory.createDescriptorOptionalBuiltIns(
            current,
            languageVersionSettings,
            storageManager,
            runtimeModule?.builtIns,
            packageAccessHandler = null // TODO: This is a speed optimization used by Native. Don't bother for now.
        )
        if (isBuiltIns) runtimeModule = md

        val dependencies = moduleDependencies.getValue(current).map { getModuleDescriptor(it) }
        md.setDependencies(listOf(md) + dependencies)
        md
    }

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
    bindingContext: BindingContext,
    files: List<KtFile>,
    klibPath: String,
    dependencies: List<KotlinLibrary>,
    moduleFragment: IrModuleFragment,
    expectDescriptorToSymbol: MutableMap<DeclarationDescriptor, IrSymbol>,
    cleanFiles: List<KotlinFileSerializedData>,
    nopack: Boolean
) {
    assert(files.size == moduleFragment.files.size)

    val descriptorTable = DescriptorTable.createDefault()
    val serializedIr =
        JsIrModuleSerializer(emptyLoggingContext, moduleFragment.irBuiltins, descriptorTable, skipExpects = !configuration.klibMpp, expectDescriptorToSymbol = expectDescriptorToSymbol).serializedIrModule(moduleFragment)

    val moduleDescriptor = moduleFragment.descriptor

    val metadataVersion = configuration.metadataVersion
    val languageVersionSettings = configuration.languageVersionSettings

    val metadataSerializer = KlibMetadataIncrementalSerializer(
        languageVersionSettings,
        metadataVersion,
        descriptorTable,
        skipExpects = !configuration.klibMpp
    )

    fun serializeScope(fqName: FqName, memberScope: Collection<DeclarationDescriptor>): ByteArray {
        return metadataSerializer.serializePackageFragment(
            moduleDescriptor,
            memberScope,
            fqName
        ).toByteArray()
    }

    val incrementalResultsConsumer = configuration.get(JSConfigurationKeys.INCREMENTAL_RESULTS_CONSUMER)
    val empty = ByteArray(0)

    fun processCompiledFileData(ioFile: File, compiledFile: KotlinFileSerializedData) {
        incrementalResultsConsumer?.run {
            processPackagePart(ioFile, compiledFile.metadata, empty, empty)
            with(compiledFile.irData) {
                processIrFile(ioFile, fileData, symbols, types, strings, declarations, bodies, fqName.toByteArray())
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
        val memberScope = ktFile.declarations.map { getDescriptorForElement(bindingContext, it) }
        val packageFragment = serializeScope(ktFile.packageFqName, memberScope)
        val compiledKotlinFile = KotlinFileSerializedData(packageFragment, binaryFile)

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
            compiledKotlinFiles.groupBy { it.irData.fqName }.map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap(),
            header
        )

    val fullSerializedIr = SerializedIrModule(compiledKotlinFiles.map { it.irData })

    val versions = KotlinLibraryVersioning(
        abiVersion = KotlinAbiVersion.CURRENT,
        libraryVersion = null,
        compilerVersion = KotlinCompilerVersion.VERSION,
        metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
        irVersion = KlibIrVersion.INSTANCE.toString()
    )

    buildKoltinLibrary(
        linkDependencies = dependencies,
        ir = fullSerializedIr,
        metadata = serializedMetadata,
        dataFlowGraph = null,
        manifestProperties = null,
        moduleName = moduleName,
        nopack = nopack,
        output = klibPath,
        versions = versions
    )
}

/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsMangler
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.newJsDescriptorUniqId
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.ExpectDeclarationRemover
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JSConfigurationKeys
import org.jetbrains.kotlin.konan.KonanVersionImpl
import org.jetbrains.kotlin.konan.MetaVersion
import org.jetbrains.kotlin.konan.properties.propertyList
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.buildKoltinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
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

internal val JS_KLIBRARY_CAPABILITY = ModuleDescriptor.Capability<KotlinLibrary>("JS KLIBRARY")

private val emptyLoggingContext = object : LoggingContext {
    override var inVerbosePhase = false

    override fun log(message: () -> String) {}
}

private val CompilerConfiguration.metadataVersion
    get() = get(CommonConfigurationKeys.METADATA_VERSION) as? JsKlibMetadataVersion ?: JsKlibMetadataVersion.INSTANCE

class KotlinFileSerializedData(val metadata: ByteArray, val irData: SerializedIrFile)

fun generateKLib(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    allDependencies: List<KotlinLibrary>,
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
                SerializedIrFile(fileData, String(fqn), f.path, symbols, types, strings, bodies, declarations)
            }
            storage.add(KotlinFileSerializedData(metaFile.metadata, irFile))
        }

        icData = storage
    } else {
        icData = emptyList()
    }

    val depsDescriptors = ModulesStructure(project, files, configuration, allDependencies, friendDependencies)

    val psi2IrContext = runAnalysisAndPreparePsi2Ir(depsDescriptors)

    val moduleFragment = psi2IrContext.generateModuleFragment(files)

    val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!

    moduleFragment.acceptVoid(ExpectDeclarationRemover(psi2IrContext.symbolTable, false))

    serializeModuleIntoKlib(
        moduleName,
        configuration,
        psi2IrContext.bindingContext,
        files,
        outputKlibPath,
        allDependencies,
        moduleFragment,
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
    allDependencies: List<KotlinLibrary>,
    friendDependencies: List<KotlinLibrary>
): IrModuleInfo {
    val depsDescriptors = ModulesStructure(project, files, configuration, allDependencies, friendDependencies)

    val psi2IrContext = runAnalysisAndPreparePsi2Ir(depsDescriptors)

    val irBuiltIns = psi2IrContext.irBuiltIns
    val symbolTable = psi2IrContext.symbolTable
    val moduleDescriptor = psi2IrContext.moduleDescriptor

    val deserializer = JsIrLinker(moduleDescriptor, JsMangler, emptyLoggingContext, irBuiltIns, symbolTable)

    val deserializedModuleFragments = sortDependencies(allDependencies, depsDescriptors.descriptors).map {
        deserializer.deserializeIrModuleHeader(depsDescriptors.getModuleDescriptor(it))!!
    }

    val moduleFragment = psi2IrContext.generateModuleFragment(files, deserializer)

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

private fun GeneratorContext.generateModuleFragment(files: List<KtFile>, deserializer: JsIrLinker? = null) =
    Psi2IrTranslator(languageVersionSettings, configuration).generateModuleFragment(this, files, deserializer)


private fun loadKlibMetadataParts(
    moduleId: KotlinLibrary
): JsKlibMetadataParts {
    return JsKlibMetadataSerializationUtil.readModuleAsProto(moduleId.moduleHeaderData)
}

val ModuleDescriptor.kotlinLibrary get() = this.getCapability(JS_KLIBRARY_CAPABILITY)!!

private fun loadKlibMetadata(
    parts: JsKlibMetadataParts,
    moduleId: KotlinLibrary,
    isBuiltIn: Boolean,
    lookupTracker: LookupTracker,
    storageManager: LockBasedStorageManager,
    metadataVersion: JsKlibMetadataVersion,
    languageVersionSettings: LanguageVersionSettings,
    builtinsModule: ModuleDescriptorImpl?,
    dependencies: List<ModuleDescriptorImpl>
): ModuleDescriptorImpl {
    assert(isBuiltIn == (builtinsModule === null))
    val builtIns = builtinsModule?.builtIns ?: object : KotlinBuiltIns(storageManager) {}
    val md = ModuleDescriptorImpl(
        Name.special("<${moduleId.moduleName}>"),
        storageManager,
        builtIns,
        capabilities = mapOf(JS_KLIBRARY_CAPABILITY to moduleId)
    )
    if (isBuiltIn) builtIns.builtInsModule = md
    val currentModuleFragmentProvider = createJsKlibMetadataPackageFragmentProvider(
        storageManager, md, parts.header, parts.body, metadataVersion,
        CompilerDeserializationConfiguration(languageVersionSettings),
        lookupTracker
    )

    val packageFragmentProvider = if (isBuiltIn) {
        val functionFragmentProvider = functionInterfacePackageFragmentProvider(storageManager, md)
        CompositePackageFragmentProvider(listOf(functionFragmentProvider, currentModuleFragmentProvider))
    } else currentModuleFragmentProvider

    md.initialize(packageFragmentProvider)
    md.setDependencies(listOf(md) + dependencies)

    return md
}

private class ModulesStructure(
    private val project: Project,
    private val files: List<KtFile>,
    val compilerConfiguration: CompilerConfiguration,
    private val allDependencies: List<KotlinLibrary>,
    private val friendDependencies: List<KotlinLibrary>
) {
    private val deserializedModuleParts: Map<KotlinLibrary, JsKlibMetadataParts> =
        allDependencies.associateWith { loadKlibMetadataParts(it) }

    fun findModuleByName(name: String): KotlinLibrary =
        allDependencies.find { it.moduleName == name } ?: error("Module is not found: $name")

    val moduleDependencies: Map<KotlinLibrary, List<KotlinLibrary>> =
        deserializedModuleParts.mapValues { (_, parts) ->
            parts.importedModules.map(::findModuleByName)
        }

    val builtInsDep = allDependencies.find { it.isBuiltIns }

    fun runAnalysis(): JsAnalysisResult {
        val analysisResult =
            TopDownAnalyzerFacadeForJS.analyzeFiles(
                files,
                project,
                compilerConfiguration,
                allDependencies.map { getModuleDescriptor(it) },
                friendModuleDescriptors = friendDependencies.map { getModuleDescriptor(it) },
                thisIsBuiltInsModule = builtInModuleDescriptor == null,
                customBuiltInsModule = builtInModuleDescriptor
            )

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

        return analysisResult
    }

    private val lookupTracker: LookupTracker = LookupTracker.DO_NOTHING
    private val metadataVersion: JsKlibMetadataVersion = compilerConfiguration.metadataVersion
    private val languageVersionSettings: LanguageVersionSettings = compilerConfiguration.languageVersionSettings

    private val storageManager: LockBasedStorageManager = LockBasedStorageManager("ModulesStructure")
    private var runtimeModule: ModuleDescriptorImpl? = null

    val descriptors = mutableMapOf<KotlinLibrary, ModuleDescriptorImpl>()

    fun getModuleDescriptor(current: KotlinLibrary): ModuleDescriptorImpl = descriptors.getOrPut(current) {
        val parts = loadKlibMetadataParts(current)
        val isBuiltIns = parts.importedModules.isEmpty()
        loadKlibMetadata(
            parts,
            current,
            isBuiltIns,
            lookupTracker,
            storageManager,
            metadataVersion,
            languageVersionSettings,
            runtimeModule,
            moduleDependencies.getValue(current).map { getModuleDescriptor(it) }
        ).also {
            if (isBuiltIns) runtimeModule = it
        }
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
    cleanFiles: List<KotlinFileSerializedData>,
    nopack: Boolean
) {
    assert(files.size == moduleFragment.files.size)

    val descriptorTable = DescriptorTable()
    val serializedIr =
        JsIrModuleSerializer(emptyLoggingContext, moduleFragment.irBuiltins, descriptorTable).serializedIrModule(moduleFragment)

    val moduleDescriptor = moduleFragment.descriptor

    val descriptorSelector = { declarationDescriptor: DeclarationDescriptor ->
        val index = descriptorTable.get(declarationDescriptor) ?: error("No descriptor ID found for $declarationDescriptor")
        newJsDescriptorUniqId(index)
    }

    val metadataVersion = configuration.metadataVersion
    val languageVersionSettings = configuration.languageVersionSettings

    fun serializeScope(fqName: FqName, memberScope: Collection<DeclarationDescriptor>): ByteArray {
        return JsKlibMetadataSerializationUtil.serializeDescriptors(
            bindingContext,
            moduleDescriptor,
            memberScope,
            fqName,
            languageVersionSettings,
            metadataVersion,
            descriptorSelector
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
        val ioFile = VfsUtilCore.virtualToIoFile(ktFile.virtualFile)
        assert(ioFile.path == binaryFile.path) { "The Kt and Ir files are put in different order" }
        val memberScope = ktFile.declarations.map { getDescriptorForElement(bindingContext, it) }
        val packageFragment = serializeScope(ktFile.packageFqName, memberScope)
        val compiledKotlinFile = KotlinFileSerializedData(packageFragment, binaryFile)

        additionalFiles += compiledKotlinFile
        processCompiledFileData(ioFile, compiledKotlinFile)
    }

    incrementalResultsConsumer?.run {
        processHeader(JsKlibMetadataSerializationUtil.serializeHeader(moduleDescriptor, null, languageVersionSettings).toByteArray())
    }

    val compiledKotlinFiles = cleanFiles + additionalFiles

    val serializedMetadata =
        JsKlibMetadataSerializationUtil.serializedMetadata(
            moduleDescriptor,
            languageVersionSettings,
            dependencies.map { it.moduleName },
            compiledKotlinFiles.groupBy { it.irData.fqName }.map { (fqn, data) -> fqn to data.sortedBy { it.irData.path }.map { it.metadata } }.toMap()
        )

    val fullSerializedIr = SerializedIrModule(compiledKotlinFiles.map { it.irData })

    val abiVersion = KotlinAbiVersion.CURRENT
    val compilerVersion = KonanVersionImpl(MetaVersion.DEV, 1, 3, 0, -1)
    val libraryVersion = "JSIR"

    val versions = KonanLibraryVersioning(abiVersion = abiVersion, libraryVersion = libraryVersion, compilerVersion = compilerVersion)

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

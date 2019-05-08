/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.newJsDescriptorUniqId
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataSerializationUtil
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataVersion
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.createJsKlibMetadataPackageFragmentProvider
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.*
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import java.nio.file.Files.move
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

data class KlibModuleRef(
    val moduleName: String,
    val klibPath: String
)

internal val JS_KLIBRARY_CAPABILITY = ModuleDescriptor.Capability<File>("JS KLIBRARY")
internal val moduleHeaderFileName = "module.kji"
internal val declarationsDirName = "ir/"
private val emptyLoggingContext = object : LoggingContext {
    override var inVerbosePhase = false

    override fun log(message: () -> String) {}
}

private fun metadataFileName(moduleName: String) = "$moduleName.${JsKlibMetadataSerializationUtil.CLASS_METADATA_FILE_EXTENSION}"

private val CompilerConfiguration.metadataVersion
    get() = get(CommonConfigurationKeys.METADATA_VERSION) as? JsKlibMetadataVersion ?: JsKlibMetadataVersion.INSTANCE

fun generateKLib(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    immediateDependencies: List<KlibModuleRef>,
    allDependencies: List<KlibModuleRef>,
    friendDependencies: List<KlibModuleRef>,
    outputKlibPath: String
): KlibModuleRef {
    val depsDescriptors = ModulesStructure(project, files, configuration, immediateDependencies, allDependencies, friendDependencies)

    val psi2IrContext = runAnalysisAndPreparePsi2Ir(depsDescriptors)

    val moduleFragment = psi2IrContext.generateModuleFragment(files)

    val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!
    serializeModuleIntoKlib(
        moduleName,
        configuration.metadataVersion,
        configuration.languageVersionSettings,
        psi2IrContext.bindingContext,
        outputKlibPath,
        immediateDependencies,
        moduleFragment
    )

    return KlibModuleRef(moduleName, outputKlibPath)
}

data class IrModuleInfo(
    val module: IrModuleFragment,
    val allDependencies: List<IrModuleFragment>,
    val bultins: IrBuiltIns,
    val symbolTable: SymbolTable,
    val deserializer: JsIrLinker
)

fun loadIr(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    immediateDependencies: List<KlibModuleRef>,
    allDependencies: List<KlibModuleRef>,
    friendDependencies: List<KlibModuleRef>
): IrModuleInfo {
    val depsDescriptors = ModulesStructure(project, files, configuration, immediateDependencies, allDependencies, friendDependencies)

    val psi2IrContext = runAnalysisAndPreparePsi2Ir(depsDescriptors)

    val irBuiltIns = psi2IrContext.irBuiltIns
    val symbolTable = psi2IrContext.symbolTable
    val moduleDescriptor = psi2IrContext.moduleDescriptor

    val deserializer = JsIrLinker(moduleDescriptor, emptyLoggingContext, irBuiltIns, symbolTable)

    val deserializedModuleFragments = depsDescriptors.sortedImmediateDependencies.map {
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
        GeneratorExtensions()
    )
}

private fun GeneratorContext.generateModuleFragment(files: List<KtFile>, deserializer: JsIrLinker? = null) =
    Psi2IrTranslator(languageVersionSettings, configuration).generateModuleFragment(this, files, deserializer)


private fun loadKlibMetadataParts(
    moduleId: KlibModuleRef
): JsKlibMetadataParts {
    val metadataFile = File(moduleId.klibPath, metadataFileName(moduleId.moduleName))
    val serializer = JsKlibMetadataSerializationUtil
    return serializer.readModuleAsProto(metadataFile.readBytes())
}

private fun loadKlibMetadata(
    parts: JsKlibMetadataParts,
    moduleId: KlibModuleRef,
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
        capabilities = mapOf(JS_KLIBRARY_CAPABILITY to File(moduleId.klibPath))
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
    immediateDependencies: List<KlibModuleRef>,
    private val allDependencies: List<KlibModuleRef>,
    private val friendDependencies: List<KlibModuleRef>
) {
    private val deserializedModuleParts: Map<KlibModuleRef, JsKlibMetadataParts> =
        allDependencies.associateWith { loadKlibMetadataParts(it) }

    fun findModuleByName(name: String): KlibModuleRef =
        allDependencies.find { it.moduleName == name } ?: error("Module is not found: $name")

    val moduleDependencies: Map<KlibModuleRef, List<KlibModuleRef>> =
        deserializedModuleParts.mapValues { (_, parts) ->
            parts.importedModules.map(::findModuleByName)
        }

    val sortedImmediateDependencies: List<KlibModuleRef> =
        DFS.topologicalOrder(immediateDependencies) { moduleDependencies.getValue(it) }
            .reversed()

    val builtInsDep = sortedImmediateDependencies.firstOrNull()

    fun runAnalysis(): JsAnalysisResult {
        val analysisResult =
            TopDownAnalyzerFacadeForJS.analyzeFiles(
                files,
                project,
                compilerConfiguration,
                sortedImmediateDependencies.map { getModuleDescriptor(it) },
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

    private val descriptors = mutableMapOf<KlibModuleRef, ModuleDescriptorImpl>()

    fun getModuleDescriptor(current: KlibModuleRef): ModuleDescriptorImpl = descriptors.getOrPut(current) {
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

fun serializeModuleIntoKlib(
    moduleName: String,
    metadataVersion: JsKlibMetadataVersion,
    languageVersionSettings: LanguageVersionSettings,
    bindingContext: BindingContext,
    klibPath: String,
    dependencies: List<KlibModuleRef>,
    moduleFragment: IrModuleFragment
) {
    val declarationTable = JsDeclarationTable(moduleFragment.irBuiltins, DescriptorTable())

    val serializedIr = JsIrModuleSerializer(emptyLoggingContext, declarationTable).serializedIrModule(moduleFragment)
    val serializer = JsKlibMetadataSerializationUtil

    val moduleDescription =
        JsKlibMetadataModuleDescriptor(moduleName, dependencies.map { it.moduleName }, moduleFragment.descriptor)
    val serializedData = serializer.serializeMetadata(
        bindingContext,
        moduleDescription,
        languageVersionSettings,
        metadataVersion
    ) { declarationDescriptor ->
        val index = declarationTable.descriptorTable.get(declarationDescriptor)
        index?.let { newJsDescriptorUniqId(it) }
    }

    val klibDir = File(klibPath).also {
        it.deleteRecursively()
        it.mkdirs()
    }

    val moduleFile = File(klibDir, moduleHeaderFileName)
    moduleFile.writeBytes(serializedIr.module)

    val irDeclarationDir = File(klibDir, declarationsDirName).also { it.mkdir() }
    val irCombinedFile = File(irDeclarationDir, "irCombined.knd")
    move(Paths.get(serializedIr.combinedDeclarationFilePath), Paths.get(irCombinedFile.path), StandardCopyOption.REPLACE_EXISTING)

    File(klibDir, "${moduleDescription.name}.${JsKlibMetadataSerializationUtil.CLASS_METADATA_FILE_EXTENSION}").also {
        it.writeBytes(serializedData.asByteArray())
    }
}

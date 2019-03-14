/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
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
import org.jetbrains.kotlin.ir.backend.js.lower.inline.replaceUnboundSymbols
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsDeclarationTable
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrLinker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrModuleSerializer
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.newJsDescriptorUniqId
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataModuleDescriptor
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataSerializationUtil
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.JsKlibMetadataVersion
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.createJsKlibMetadataPackageFragmentProvider
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import java.nio.file.Files.move
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class CompiledModule(
    val moduleName: String,
    val generatedCode: String?,
    var moduleDescriptor: ModuleDescriptorImpl?,
    val klibPath: String,
    val dependencies: List<CompiledModule>,
    val isBuiltIn: Boolean
)

enum class CompilationMode(val generateJS: Boolean, val generateKlib: Boolean) {
    KLIB(false, true),
    KLIB_WITH_JS(true, true),
    JS_AGAINST_KLIB(true, false)
}

private val moduleHeaderFileName = "module.kji"
private val declarationsDirName = "ir/"
private val logggg = object : LoggingContext {
    override var inVerbosePhase: Boolean
        get() = TODO("not implemented")
        set(_) {}

    override fun log(message: () -> String) {}
}

private fun metadataFileName(moduleName: String) = "$moduleName.${JsKlibMetadataSerializationUtil.CLASS_METADATA_FILE_EXTENSION}"


data class JsKlib(
    val moduleDescriptor: ModuleDescriptorImpl,
    val moduleIr: IrModuleFragment,
    val symbolTable: SymbolTable,
    val irBuiltIns: IrBuiltIns,
    val deserializer: JsIrLinker
)


private fun deserializeModuleFromKlib(
    locationDir: String,
    moduleName: String,
    lookupTracker: LookupTracker,
    storageManager: LockBasedStorageManager,
    metadataVersion: JsKlibMetadataVersion,
    languageVersionSettings: LanguageVersionSettings,
    dependencies: List<CompiledModule>,
    builtinsModule: ModuleDescriptorImpl?
): JsKlib {
    val klibDirFile = File(locationDir)
    val md = loadKlibMetadata(
        moduleName,
        locationDir,
        builtinsModule == null,
        lookupTracker,
        storageManager,
        metadataVersion,
        languageVersionSettings,
        builtinsModule,
        dependencies
    )

    val st = SymbolTable()
    val typeTranslator = TypeTranslator(st, languageVersionSettings).also {
        it.constantValueGenerator = ConstantValueGenerator(md, st)
    }

    val irBuiltIns = IrBuiltIns(md.builtIns, typeTranslator, st)

    val moduleFile = File(klibDirFile, moduleHeaderFileName)
    val deserializer = JsIrLinker(md, logggg, irBuiltIns, st)

    dependencies.forEach {
        val dependencyKlibDir = File(it.klibPath, moduleHeaderFileName)
        deserializer.deserializeIrModuleHeader(it.moduleDescriptor!!, dependencyKlibDir.readBytes(), File(it.klibPath), DeserializationStrategy.ONLY_REFERENCED)
    }

    val moduleFragment = deserializer.deserializeIrModuleHeader(md, moduleFile.readBytes(), klibDirFile, DeserializationStrategy.ALL)

    return JsKlib(md, moduleFragment, st, irBuiltIns, deserializer)
}

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    export: List<FqName> = emptyList(),
    compileMode: CompilationMode,
    dependencies: List<CompiledModule> = emptyList(),
    klibPath: String
): CompiledModule {
    val metadataVersion = configuration.get(CommonConfigurationKeys.METADATA_VERSION)  as? JsKlibMetadataVersion
        ?: JsKlibMetadataVersion.INSTANCE
    val lookupTracker = LookupTracker.DO_NOTHING
    val languageSettings = configuration.languageVersionSettings
    val storageManager = LockBasedStorageManager("JsDependencies")
    val dfsHandler: MetadataDFSHandler = DependencyMetadataLoader(lookupTracker, metadataVersion, languageSettings, storageManager)
    val sortedDeps = DFS.dfs(dependencies, CompiledModule::dependencies, dfsHandler)
    val builtInModule = sortedDeps.firstOrNull()?.moduleDescriptor // null in case compiling builtInModule itself

    val analysisResult =
        TopDownAnalyzerFacadeForJS.analyzeFiles(
            files,
            project,
            configuration,
            sortedDeps.mapNotNull { it.moduleDescriptor },
            emptyList(),
            thisIsBuiltInsModule = builtInModule == null,
            customBuiltInsModule = builtInModule
        )

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

    val moduleDescriptor = analysisResult.moduleDescriptor as ModuleDescriptorImpl
    val symbolTable = SymbolTable()

    val psi2IrTranslator = Psi2IrTranslator(languageSettings)
    val psi2IrContext = psi2IrTranslator.createGeneratorContext(moduleDescriptor, analysisResult.bindingContext, symbolTable)
    val irBuiltIns = psi2IrContext.irBuiltIns

    var deserializer = JsIrLinker(moduleDescriptor, logggg, irBuiltIns, symbolTable)

    val deserializedModuleFragments = sortedDeps.map {
        val moduleFile = File(it.klibPath, moduleHeaderFileName)
        deserializer.deserializeIrModuleHeader(it.moduleDescriptor!!, moduleFile.readBytes(), File(it.klibPath), DeserializationStrategy.ONLY_REFERENCED)
    }

    var moduleFragment = psi2IrTranslator.generateModuleFragment(psi2IrContext, files, deserializer)
    val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME) as String

    val context = if (compileMode.generateKlib) {
        deserializedModuleFragments.forEach {
            ExternalDependenciesGenerator(it.descriptor, symbolTable, irBuiltIns).generateUnboundSymbolsAsDependencies()
        }
        deserializedModuleFragments.forEach { it.patchDeclarationParents() }
        serializeModuleIntoKlib(
            moduleName,
            metadataVersion,
            languageSettings,
            symbolTable,
            psi2IrContext.bindingContext,
            klibPath,
            dependencies,
            moduleFragment
        )

        if (compileMode.generateJS) {
            deserializeModuleFromKlib(
                klibPath,
                moduleName,
                lookupTracker,
                LockBasedStorageManager("JsDeserialized"),
                metadataVersion,
                languageSettings,
                sortedDeps,
                builtInModule
            ).let {
                deserializer = it.deserializer
                moduleFragment = it.moduleIr

                JsIrBackendContext(it.moduleDescriptor, it.irBuiltIns, it.symbolTable, it.moduleIr, configuration, compileMode).also {
                    moduleFragment.replaceUnboundSymbols(it)
                }
            }
        } else {
            return CompiledModule(moduleName, null, null, klibPath, dependencies, builtInModule == null)
        }
    } else JsIrBackendContext(moduleDescriptor, irBuiltIns, symbolTable, moduleFragment, configuration, compileMode)

    val jsProgram = if (compileMode.generateJS) {
        deserializedModuleFragments.forEach {
            ExternalDependenciesGenerator(
                it.descriptor,
                context.symbolTable,
                context.irBuiltIns,
                deserializer = deserializer
            ).generateUnboundSymbolsAsDependencies()
        }

        // TODO: check the order
        val irFiles = deserializedModuleFragments.flatMap { it.files } + moduleFragment.files

        moduleFragment.files.clear()
        moduleFragment.files += irFiles

        ExternalDependenciesGenerator(
            moduleDescriptor = context.module,
            symbolTable = context.symbolTable,
            irBuiltIns = context.irBuiltIns
        ).generateUnboundSymbolsAsDependencies()
        moduleFragment.patchDeclarationParents()

        jsPhases.invokeToplevel(context.phaseConfig, context, moduleFragment)

        moduleFragment.accept(IrModuleToJsTransformer(context), null)
    } else null

    return CompiledModule(moduleName, jsProgram?.toString(), null, klibPath, dependencies, builtInModule == null)

}

private fun loadKlibMetadata(
    moduleName: String,
    klibPath: String,
    isBuiltIn: Boolean,
    lookupTracker: LookupTracker,
    storageManager: LockBasedStorageManager,
    metadataVersion: JsKlibMetadataVersion,
    languageVersionSettings: LanguageVersionSettings,
    builtinsModule: ModuleDescriptorImpl?,
    dependencies: List<CompiledModule>
): ModuleDescriptorImpl {
    assert(isBuiltIn == (builtinsModule === null))

    val metadataFile = File(klibPath, metadataFileName(moduleName))

    val serializer = JsKlibMetadataSerializationUtil
    val parts = serializer.readModuleAsProto(metadataFile.readBytes())
    val builtIns = builtinsModule?.builtIns ?: object : KotlinBuiltIns(storageManager) {}
    val md = ModuleDescriptorImpl(Name.special("<$moduleName>"), storageManager, builtIns)
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
    md.setDependencies(listOf(md) + dependencies.mapNotNull { it.moduleDescriptor })

    return md
}

typealias MetadataDFSHandler = DFS.NodeHandler<CompiledModule, List<CompiledModule>>

private class DependencyMetadataLoader(
    private val lookupTracker: LookupTracker,
    private val metadataVersion: JsKlibMetadataVersion,
    private val languageVersionSettings: LanguageVersionSettings,
    private val storageManager: LockBasedStorageManager
) : MetadataDFSHandler {
    private val sortedDependencies = mutableListOf<CompiledModule>()

    private var runtimeModule: ModuleDescriptorImpl? = null

    override fun beforeChildren(current: CompiledModule) = true

    override fun afterChildren(current: CompiledModule) {
        val md = current.moduleDescriptor ?: loadKlibMetadata(
            current.moduleName,
            current.klibPath,
            current.isBuiltIn,
            lookupTracker,
            storageManager,
            metadataVersion,
            languageVersionSettings,
            runtimeModule,
            current.dependencies
        ).also { current.moduleDescriptor = it }
        sortedDependencies += current
        if (current.isBuiltIn) runtimeModule = md
    }

    override fun result() = sortedDependencies
}

fun serializeModuleIntoKlib(
    moduleName: String,
    metadataVersion: JsKlibMetadataVersion,
    languageVersionSettings: LanguageVersionSettings,
    symbolTable: SymbolTable,
    bindingContext: BindingContext,
    klibPath: String,
    dependencies: List<CompiledModule>,
    moduleFragment: IrModuleFragment
) {
    val declarationTable = JsDeclarationTable(moduleFragment.irBuiltins, DescriptorTable())

    val serializedIr = JsIrModuleSerializer(logggg, declarationTable).serializedIrModule(moduleFragment)
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
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.backend.common.CompilerPhaseManager
import org.jetbrains.kotlin.backend.common.output.OutputFile
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.backend.common.output.SimpleOutputBinaryFile
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata.*
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.CoroutineIntrinsicLowering
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.IrModuleToJsTransformer
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.JsMetadataVersion
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils
import java.io.File
import org.jetbrains.kotlin.utils.DFS

enum class ModuleType {
    TEST_RUNTIME,
    SECONDARY,
    MAIN
}

class CompiledModule(
    val moduleName: String,
    val generatedCode: String?,
    val moduleFragment: IrModuleFragment?,
    val moduleType: ModuleType,
    val dependencies: List<CompiledModule>
) {
    val descriptor
        get() = moduleFragment!!.descriptor as ModuleDescriptorImpl
}

fun OutputFileCollection.writeAll(outputDir: File) {
    for (file in asList()) {
        val output = File(outputDir, file.relativePath)
        FileUtil.writeToFile(output, file.asByteArray())
    }
}

fun compile(
    project: Project,
    files: List<KtFile>,
    configuration: CompilerConfiguration,
    export: List<FqName> = emptyList(),
    isKlibCompilation: Boolean = false,
    dependencies: List<CompiledModule> = emptyList(),
    builtInsModule: CompiledModule? = null,
    moduleType: ModuleType
): CompiledModule {
    val analysisResult =
        TopDownAnalyzerFacadeForJS.analyzeFiles(
            files,
            project,
            configuration,
            dependencies.map { it.descriptor },
            emptyList(),
            thisIsBuiltInsModule = builtInsModule == null,
            customBuiltInsModule = builtInsModule?.descriptor
        )

    ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

    TopDownAnalyzerFacadeForJS.checkForErrors(files, analysisResult.bindingContext)

    val symbolTable = SymbolTable()
    dependencies.forEach { symbolTable.loadModule(it.moduleFragment!!) }

    val psi2IrTranslator = Psi2IrTranslator(configuration.languageVersionSettings)
    val psi2IrContext = psi2IrTranslator.createGeneratorContext(analysisResult.moduleDescriptor, analysisResult.bindingContext, symbolTable)

    val moduleFragment = psi2IrTranslator.generateModuleFragment(psi2IrContext, files)

    val context = JsIrBackendContext(
        analysisResult.moduleDescriptor,
        psi2IrContext.irBuiltIns,
        psi2IrContext.symbolTable,
        moduleFragment,
        configuration,
        dependencies,
        moduleType
    )


    if (isKlibCompilation) {
//        val declarationTable = DeclarationTable(moduleFragment.irBuiltins, DescriptorTable())
//        val serializedIr = IrModuleSerializer(context, declarationTable/*, onlyForInlines = false*/).serializedIrModule(moduleFragment)
//        val serializer = KonanSerializationUtil(context, configuration.get(CommonConfigurationKeys.METADATA_VERSION)!!, declarationTable)
//        val serializedData = serializer.serializeModule(analysisResult.moduleDescriptor, serializedIr)
//        buildLibrary(serializedData)
//

//        val declarationTable = DeclarationTable(moduleFragment.irBuiltins, DescriptorTable())
//        val serializedIr = IrModuleSerializer(context, declarationTable/*, onlyForInlines = false*/).serializedIrModule(moduleFragment)
        val serializer = JsKlibMetadataSerializationUtil
//        val serializedData = serializer.serializeModule(analysisResult.moduleDescriptor, serializedIr)
        val moduleName = configuration.get(CommonConfigurationKeys.MODULE_NAME) as String
        val metadataVersion = configuration.get(CommonConfigurationKeys.METADATA_VERSION)  as? JsKlibMetadataVersion
            ?: JsKlibMetadataVersion.INSTANCE
        val moduleDescription =
            JsKlibMetadataModuleDescriptor(moduleName, dependencies.map { it.name.asString() }, moduleFragment.descriptor)
        var index = 0L
        val serializedData = serializer.serializeMetadata(
            psi2IrContext.bindingContext,
            moduleDescription,
            configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!,
            metadataVersion
        ) {
//            val index = declarationTable.descriptorTable.get(it)
//            index?.let { newDescriptorUniqId(it) }
            JsKlibMetadataProtoBuf.DescriptorUniqId.newBuilder().setIndex(index++).build()

        }
//        buildLibrary(serializedData)
//

        val stdKlibDir = File("js/js.translator/testData/out/klibs/runtime/").also {
            it.deleteRecursively()
            it.mkdirs()
        }
//
//        val moduleFile = File(stdKlibDir, "module.kji")
//        moduleFile.writeBytes(serializedIr.module)
//
//        for ((id, data) in serializedIr.declarations) {
//            val file = File(stdKlibDir, "${id.index}${if (id.isLocal) "L" else "G"}.kjd")
//            file.writeBytes(data)
//        }
//
//
//        val debugFile = File(stdKlibDir, "debug.txt")
//
//        for ((id, data) in serializedIr.debugIndex) {
//            debugFile.appendText(id.toString())
//            debugFile.appendText(" --- ")
//            debugFile.appendText(data)
//            debugFile.appendText("\n")
//        }

        val metadata = File(stdKlibDir, "${moduleDescription.name}${JsKlibMetadataSerializationUtil.CLASS_METADATA_FILE_EXTENSION}").also {
            it.writeBytes(serializedData.asByteArray())
        }

        val storageManager = LockBasedStorageManager("JsConfig")
//        // CREATE NEW MODULE DESCRIPTOR HERE AND DESERIALIZE IT

        val lookupTracker = configuration.get(CommonConfigurationKeys.LOOKUP_TRACKER, LookupTracker.DO_NOTHING)
        val parts = serializer.readModuleAsProto(metadata.readBytes())
        val md = ModuleDescriptorImpl(
            Name.special("<$moduleName>"), storageManager, JsPlatform.builtIns
        )
        val provider = createJsKlibMetadataPackageFragmentProvider(
            storageManager, md, parts.header, parts.body, metadataVersion,
            CompilerDeserializationConfiguration(configuration.languageVersionSettings),
            lookupTracker
        )

        md.initialize(provider)
        md.setDependencies(listOf(md, md.builtIns.builtInsModule))

        TODO("Implemenet IrSerialization")
    } else {

        // TODO: Split compilation into two steps: kt -> ir, ir -> js
        val moduleName = configuration[CommonConfigurationKeys.MODULE_NAME]!!
        when (moduleType) {
            ModuleType.MAIN -> {
                val moduleDependencies: List<CompiledModule> =
                    DFS.topologicalOrder(dependencies, CompiledModule::dependencies)
                        .filter { it.moduleType == ModuleType.SECONDARY }

                val fileDependencies = moduleDependencies.flatMap { it.moduleFragment!!.files }

                moduleFragment.files.addAll(0, fileDependencies)
            }

            ModuleType.SECONDARY -> {
                return CompiledModule(moduleName, null, moduleFragment, moduleType, dependencies)
            }

            ModuleType.TEST_RUNTIME -> {
            }
        }
    }

    jsPhases.invokeToplevel(context.phaseConfig, context, moduleFragment)

    val jsProgram = moduleFragment.accept(IrModuleToJsTransformer(context), null)

    return CompiledModule(moduleName, jsProgram.toString(), context.moduleFragmentCopy, moduleType, dependencies)
}

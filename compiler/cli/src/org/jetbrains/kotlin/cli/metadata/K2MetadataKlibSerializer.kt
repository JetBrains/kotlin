/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.backend.common.serialization.DescriptorTable
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRoot
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.library.impl.buildKoltinLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibrary
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.utils.keysToMap
import java.io.File

internal class K2MetadataKlibSerializer(private val metadataVersion: BuiltInsBinaryVersion) {
    fun serialize(environment: KotlinCoreEnvironment) {
        val configuration = environment.configuration

        val dependencyContainer = KlibMetadataDependencyContainer(
            configuration,
            LockBasedStorageManager("K2MetadataKlibSerializer")
        )

        val analyzer = runCommonAnalysisForSerialization(environment, false, dependencyContainer)

        if (analyzer == null || analyzer.hasErrors()) return

        val (_, moduleDescriptor) = analyzer.analysisResult

        val destDir = checkNotNull(environment.destDir)
        performSerialization(configuration, moduleDescriptor, destDir)
    }

    private fun performSerialization(
        configuration: CompilerConfiguration,
        module: ModuleDescriptor,
        destDir: File
    ) {
        val serializedMetadata: SerializedMetadata = KlibMetadataMonolithicSerializer(
            configuration.languageVersionSettings,
            metadataVersion,
            DescriptorTable.createDefault(),
            skipExpects = false,
            includeOnlyModuleContent = true
        ).serializeModule(module)

        val versions = KotlinLibraryVersioning(
            abiVersion = KotlinAbiVersion.CURRENT,
            libraryVersion = null,
            compilerVersion = KotlinCompilerVersion.getVersion(),
            metadataVersion = null,
            irVersion = null
        )

        buildKoltinLibrary(
            emptyList(),
            serializedMetadata,
            null,
            versions,
            destDir.absolutePath,
            configuration[CommonConfigurationKeys.MODULE_NAME]!!,
            nopack = true,
            manifestProperties = null,
            dataFlowGraph = null
        )
    }
}

private class KlibMetadataDependencyContainer(
    private val configuration: CompilerConfiguration,
    private val storageManager: StorageManager
) : CommonDependenciesContainer {

    private val kotlinLibraries = run {
        val classpathFiles =
            configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS).filterIsInstance<JvmClasspathRoot>().map(JvmContentRoot::file)

        val klibFiles = classpathFiles
            .filter { it.extension == "klib" || it.isDirectory }

        klibFiles.map { createKotlinLibrary(org.jetbrains.kotlin.konan.file.File(it.absolutePath)) }
    }

    private val builtIns
        get() = DefaultBuiltIns.Instance

    private class KlibModuleInfo(
        override val name: Name,
        val kotlinLibrary: KotlinLibrary,
        private val dependOnKlibModules: List<ModuleInfo>
    ) : ModuleInfo {
        override fun dependencies(): List<ModuleInfo> = dependOnKlibModules

        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.LAST

        override val platform: TargetPlatform
            get() = CommonPlatforms.defaultCommonPlatform

        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = CommonPlatformAnalyzerServices
    }

    private val moduleDescriptorsForKotlinLibraries: Map<KotlinLibrary, ModuleDescriptorImpl> =
        kotlinLibraries.keysToMap { library ->
            val moduleHeader = parseModuleHeader(library.moduleHeaderData)
            val moduleName = Name.special(moduleHeader.moduleName)
            val moduleOrigin = DeserializedKlibModuleOrigin(library)
            MetadataFactories.DefaultDescriptorFactory.createDescriptor(
                moduleName, storageManager, builtIns, moduleOrigin
            )
        }.also { result ->
            val resultValues = result.values.toList()
            val dependenciesForModuleDescriptors = resultValues + builtIns.builtInsModule
            resultValues.forEach { module ->
                module.setDependencies(dependenciesForModuleDescriptors)
            }
        }

    override val moduleInfos: List<ModuleInfo> = mutableListOf<KlibModuleInfo>().apply {
        addAll(
            moduleDescriptorsForKotlinLibraries.map { (kotlinLibrary, moduleDescriptor) ->
                KlibModuleInfo(moduleDescriptor.name, kotlinLibrary, this@apply)
            }
        )
    }

    override fun packageFragmentProviderForModuleInfo(
        moduleInfo: ModuleInfo
    ): PackageFragmentProvider? {
        if (moduleInfo !in moduleInfos)
            return null
        moduleInfo as KlibModuleInfo
        return packageFragmentProviderForKotlinLibrary(moduleInfo.kotlinLibrary)
    }

    private val klibMetadataModuleDescriptorFactory by lazy {
        KlibMetadataModuleDescriptorFactoryImpl(
            MetadataFactories.DefaultDescriptorFactory,
            MetadataFactories.DefaultPackageFragmentsFactory,
            MetadataFactories.flexibleTypeDeserializer
        )
    }

    private fun packageFragmentProviderForKotlinLibrary(
        library: KotlinLibrary
    ): PackageFragmentProvider {
        val languageVersionSettings = configuration.languageVersionSettings

        val libraryModuleDescriptor = moduleDescriptorsForKotlinLibraries.getValue(library)
        val packageFragmentNames = parseModuleHeader(library.moduleHeaderData).packageFragmentNameList

        return klibMetadataModuleDescriptorFactory.createPackageFragmentProvider(
            library,
            packageAccessHandler = null,
            packageFragmentNames = packageFragmentNames,
            storageManager = LockBasedStorageManager("KlibMetadataPackageFragmentProvider"),
            moduleDescriptor = libraryModuleDescriptor,
            configuration = CompilerDeserializationConfiguration(languageVersionSettings),
            compositePackageFragmentAddend = null
        ).also {
            libraryModuleDescriptor.initialize(it)
        }
    }

}

private val MetadataFactories =
    KlibMetadataFactories(
        { DefaultBuiltIns.Instance },
        org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
    )
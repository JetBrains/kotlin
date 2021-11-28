/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.ToolingSingleFileKlibResolveStrategy
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import org.jetbrains.kotlin.library.metadata.NativeTypeTransformer
import org.jetbrains.kotlin.library.metadata.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.library.resolveSingleFileKlib
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile

object KlibTestUtil {
    fun compileCommonSourcesToKlib(sourceFiles: Collection<File>, libraryName: String, klibFile: File) {
        require(!Name.guessByFirstCharacter(libraryName).isSpecial) { "Invalid library name: $libraryName" }

        val configuration = KotlinTestUtils.newConfiguration()
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        configuration.put(CommonConfigurationKeys.MODULE_NAME, libraryName)
        configuration.addKotlinSourceRoots(sourceFiles.map { it.absolutePath })

        val rootDisposable = Disposer.newDisposable()
        val module = try {
            val environment = KotlinCoreEnvironment.createForTests(
                parentDisposable = rootDisposable,
                initialConfiguration = configuration,
                extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
            )

            CommonResolverForModuleFactory.analyzeFiles(
                environment.getSourceFiles(),
                moduleName = Name.special("<$libraryName>"),
                dependOnBuiltIns = true,
                environment.configuration.languageVersionSettings,
                CommonPlatforms.defaultCommonPlatform,
                CompilerEnvironment,
            ) { content ->
                environment.createPackagePartProvider(content.moduleContentScope)
            }.moduleDescriptor
        } finally {
            Disposer.dispose(rootDisposable)
        }

        serializeCommonModuleToKlib(module, libraryName, klibFile)
    }

    fun serializeCommonModuleToKlib(module: ModuleDescriptor, libraryName: String, klibFile: File) {
        require(klibFile.extension == KLIB_FILE_EXTENSION) { "KLIB file must have $KLIB_FILE_EXTENSION extension" }

        val serializer = KlibMetadataMonolithicSerializer(
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            metadataVersion = KlibMetadataVersion.INSTANCE,
            exportKDoc = false,
            skipExpects = false,
            project = null
        )

        val serializedMetadata = serializer.serializeModule(module)

        val unzippedDir = org.jetbrains.kotlin.konan.file.createTempDir(libraryName)
        val layout = KotlinLibraryLayoutForWriter(KFile(klibFile.path), unzippedDir)

        val library = KotlinLibraryWriterImpl(
            moduleName = libraryName,
            versions = KotlinLibraryVersioning(
                libraryVersion = null,
                compilerVersion = null,
                abiVersion = null,
                metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
                irVersion = null
            ),
            builtInsPlatform = BuiltInsPlatform.COMMON,
            nativeTargets = emptyList(),
            nopack = false,
            shortName = libraryName,
            layout = layout
        )

        library.addMetadata(serializedMetadata)
        library.commit()
    }

    fun deserializeKlibToCommonModule(klibFile: File): ModuleDescriptorImpl {
        val library = resolveSingleFileKlib(
            libraryFile = KFile(klibFile.path),
            logger = DummyLogger,
            strategy = ToolingSingleFileKlibResolveStrategy
        )

        val metadataFactories = KlibMetadataFactories({ DefaultBuiltIns.Instance }, NullFlexibleTypeDeserializer, NativeTypeTransformer())

        val module = metadataFactories.DefaultDeserializedDescriptorFactory.createDescriptor(
            library = library,
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            storageManager = LockBasedStorageManager.NO_LOCKS,
            builtIns = DefaultBuiltIns.Instance,
            packageAccessHandler = null
        )
        module.setDependencies(listOf(DefaultBuiltIns.Instance.builtInsModule, module))

        return module
    }
}

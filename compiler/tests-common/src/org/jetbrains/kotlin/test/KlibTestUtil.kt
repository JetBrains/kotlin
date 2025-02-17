/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.parseCommandLineArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.FilteringMessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.common.setupCommonArguments
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.ir.backend.js.moduleName
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.BuiltInsPlatform
import org.jetbrains.kotlin.library.impl.KotlinLibraryLayoutForWriter
import org.jetbrains.kotlin.library.impl.KotlinLibraryWriterImpl
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.util.DummyLogger
import java.io.File
import java.io.IOException
import java.nio.file.Path
import org.jetbrains.kotlin.konan.file.File as KFile

object KlibTestUtil {
    fun compileCommonSourcesToKlib(
        sourceFiles: Collection<File>,
        libraryName: String,
        klibFile: File,
        additionalArguments: List<String> = emptyList(),
    ) {
        require(!Name.guessByFirstCharacter(libraryName).isSpecial) { "Invalid library name: $libraryName" }

        val configuration = KotlinTestUtils.newConfiguration()
        configuration.messageCollector =
            FilteringMessageCollector(
                PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false)
            ) /* decline = */ { !it.isError }
        configuration.put(CommonConfigurationKeys.MODULE_NAME, libraryName)
        configuration.addKotlinSourceRoots(sourceFiles.map { it.absolutePath })
        val stdlibFile = ForTestCompileRuntime.stdlibCommonForTests()
        // support for the legacy version of kotlin-stdlib-common (JAR with .kotlin_metadata)
        configuration.addJvmClasspathRoot(stdlibFile)
        configuration.setupCommonArguments(parseCommandLineArguments<K2MetadataCompilerArguments>(additionalArguments))

        val rootDisposable = Disposer.newDisposable("Disposable for ${KlibTestUtil::class.simpleName}.compileCommonSourcesToKlib")
        val module = try {
            val environment = KotlinCoreEnvironment.createForTests(
                parentDisposable = rootDisposable,
                initialConfiguration = configuration,
                extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
            )

            val projectContext = ProjectContext(environment.project, "Compile common sources to KLIB metadata")

            val analyzer = AnalyzerWithCompilerReport(
                configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY),
                configuration.languageVersionSettings,
                renderDiagnosticName = true,
            )

            analyzer.analyzeAndReport(environment.getSourceFiles()) {
                CommonResolverForModuleFactory.analyzeFiles(
                    environment.getSourceFiles(),
                    moduleName = Name.special("<$libraryName>"),
                    dependOnBuiltIns = true,
                    environment.configuration.languageVersionSettings,
                    CommonPlatforms.defaultCommonPlatform,
                    CompilerEnvironment,
                    explicitProjectContext = projectContext,
                    // support for the KLIB version of kotlin-stdlib-common (KLIB with .knm metadata)
                    dependenciesContainer = createDependencyContainerForStdlibIfKlib(stdlibFile.toPath(), environment, projectContext),
                ) { content ->
                    environment.createPackagePartProvider(content.moduleContentScope)
                }
            }

            val analysisResult = analyzer.analysisResult

            check(!analyzer.hasErrors()) {
                "Compilation finished with errors. See the previous messages."
            }

            analysisResult.moduleDescriptor
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
            project = null,
            includeOnlyModuleContent = true,
        )

        val serializedMetadata = serializer.serializeModule(module)

        val unzippedDir = org.jetbrains.kotlin.konan.file.createTempDir(libraryName)
        val layout = KotlinLibraryLayoutForWriter(KFile(klibFile.path), unzippedDir)

        val library = KotlinLibraryWriterImpl(
            moduleName = libraryName,
            versions = KotlinLibraryVersioning(
                compilerVersion = null,
                abiVersion = null,
                metadataVersion = KlibMetadataVersion.INSTANCE.toString(),
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

        val metadataFactories = KlibMetadataFactories({ DefaultBuiltIns.Instance }, NullFlexibleTypeDeserializer)

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

/**
 * Creates a dependency container that includes common stdlib, if the passed file path points to a metadata KLIB in the supported format.
 * Note that [resolveSingleFileKlib] is sensitive to the library layout and to the file extension.
 * In the case of a custom klib layout or file extension other than .klib, the library won't be resolved even if it contains .knm files.
 * For the current kotlin-stdlib-common.jar it will always return null, the purpose of the function is to simplify future migration.
 * It's been checked that the dependency container for a library with the supported layout is functional.
 * See KTI-1457.
 */
private fun createDependencyContainerForStdlibIfKlib(
    stdlibFilePath: Path,
    environment: KotlinCoreEnvironment,
    projectContext: ProjectContext,
): CommonDependenciesContainerImpl? {
    val stdlibKlib = resolveSingleFileKlib(KFile(stdlibFilePath), strategy = ToolingSingleFileKlibResolveStrategy).also {
        try {
            it.moduleName // trigger an exception in case of unsupported file/layout
        } catch (e: IOException) {
            return null
        }
    }

    val stdlibModuleDescriptor = createAndInitializeKlibBasedStdlibCommonDescriptor(stdlibKlib, environment, projectContext)
    return CommonDependenciesContainerImpl(listOf(stdlibModuleDescriptor))
}

private fun createAndInitializeKlibBasedStdlibCommonDescriptor(
    stdlibKlib: KotlinLibrary,
    environment: KotlinCoreEnvironment,
    projectContext: ProjectContext,
): ModuleDescriptor {
    val stdlibCommonDescriptor = ModuleDescriptorImpl(
        Name.special("<stdlib-common>"),
        projectContext.storageManager, DefaultBuiltIns.Instance, CommonPlatforms.defaultCommonPlatform
    ).also {
        it.setDependencies(it)
    }

    val metadataModuleDescriptorFactory: KlibMetadataModuleDescriptorFactory = KlibMetadataFactories(
        { DefaultBuiltIns.Instance },
        NullFlexibleTypeDeserializer,
    ).DefaultDeserializedDescriptorFactory

    val klibPackageFragmentProvider = metadataModuleDescriptorFactory.createPackageFragmentProvider(
        library = stdlibKlib,
        packageAccessHandler = null,
        packageFragmentNames = parseModuleHeader(stdlibKlib.moduleHeaderData).packageFragmentNameList,
        storageManager = projectContext.storageManager,
        moduleDescriptor = stdlibCommonDescriptor,
        configuration = CompilerDeserializationConfiguration(environment.configuration.languageVersionSettings),
        compositePackageFragmentAddend = null,
        lookupTracker = LookupTracker.DO_NOTHING,
    )

    stdlibCommonDescriptor.initialize(
        CompositePackageFragmentProvider(
            listOf(
                klibPackageFragmentProvider,
                DefaultBuiltIns.Instance.builtInsModule.packageFragmentProvider,
            ),
            "Test provider for .knm metadata and built-in declarations of kotlin-stdlib-common"
        )
    )

    return stdlibCommonDescriptor
}

private class CommonDependenciesContainerImpl(dependencies: Collection<ModuleDescriptor>) : CommonDependenciesContainer {
    private class ModuleInfoImpl(val module: ModuleDescriptor) : ModuleInfo {
        override val name: Name get() = module.name

        override fun dependencies(): List<ModuleInfo> = listOf(this)
        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.LAST

        override val platform: TargetPlatform get() = CommonPlatforms.defaultCommonPlatform
        override val analyzerServices: PlatformDependentAnalyzerServices get() = CommonPlatformAnalyzerServices
    }

    private val moduleDescriptorByModuleInfo: Map<ModuleInfo, ModuleDescriptor> = dependencies.associateBy(::ModuleInfoImpl)

    override val moduleInfos: List<ModuleInfo> get() = moduleDescriptorByModuleInfo.keys.toList()

    override fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor {
        return moduleDescriptorByModuleInfo[moduleInfo]
            ?: error("Unknown module info $moduleInfo")
    }

    override fun registerDependencyForAllModules(moduleInfo: ModuleInfo, descriptorForModule: ModuleDescriptorImpl) = Unit
    override fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider? = null

    override val friendModuleInfos: List<ModuleInfo> get() = emptyList()
    override val refinesModuleInfos: List<ModuleInfo> get() = emptyList()
}

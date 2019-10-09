/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.cli.common.messages.GroupingMessageCollector
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.util.Logger
import kotlin.system.exitProcess
import org.jetbrains.kotlin.library.toUnresolvedLibraries
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.resolver.impl.libraryResolver
import org.jetbrains.kotlin.library.UnresolvedLibrary

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    internal val distribution = Distribution(
        false,
        null,
        configuration.get(KonanConfigKeys.RUNTIME_FILE))

    internal val platformManager = PlatformManager(distribution)
    internal val targetManager = platformManager.targetManager(configuration.get(KonanConfigKeys.TARGET))
    internal val target = targetManager.target
    internal val phaseConfig = configuration.get(CLIConfigurationKeys.PHASE_CONFIG)!!

    val infoArgsOnly = configuration.kotlinSourceRoots.isEmpty()
            && configuration[KonanConfigKeys.INCLUDED_LIBRARIES].isNullOrEmpty()
            && configuration[KonanConfigKeys.LIBRARIES_TO_CACHE].isNullOrEmpty()

    // TODO: debug info generation mode and debug/release variant selection probably requires some refactoring.
    val debug: Boolean get() = configuration.getBoolean(KonanConfigKeys.DEBUG)
    val lightDebug: Boolean get() = configuration.getBoolean(KonanConfigKeys.LIGHT_DEBUG)

    val memoryModel: MemoryModel get() = configuration.get(KonanConfigKeys.MEMORY_MODEL)!!

    val needCompilerVerification: Boolean
        get() = configuration.get(KonanConfigKeys.VERIFY_COMPILER) ?:
            (configuration.getBoolean(KonanConfigKeys.OPTIMIZATION) ||
                KonanVersion.CURRENT.meta != MetaVersion.RELEASE)

    init {
        if (!platformManager.isEnabled(target)) {
            error("Target ${target.visibleName} is not available on the ${HostManager.hostName} host")
        }
    }

    val platform = platformManager.platform(target).apply {
        if (configuration.getBoolean(KonanConfigKeys.CHECK_DEPENDENCIES)) {
            downloadDependencies()
        }
    }

    internal val clang = platform.clang
    val indirectBranchesAreAllowed = target != KonanTarget.WASM32
    val threadsAreAllowed = (target != KonanTarget.WASM32) && (target !is KonanTarget.ZEPHYR)

    internal val produce get() = configuration.get(KonanConfigKeys.PRODUCE)!!

    internal val produceStaticFramework get() = configuration.getBoolean(KonanConfigKeys.STATIC_FRAMEWORK)

    val outputFiles = OutputFiles(configuration.get(KonanConfigKeys.OUTPUT), target, produce)
    val tempFiles = TempFiles(outputFiles.outputName, configuration.get(KonanConfigKeys.TEMPORARY_FILES_DIR))

    val outputFile = outputFiles.mainFile

    val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME) ?: File(outputFiles.outputName).name

    internal val purgeUserLibs: Boolean
        get() = configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)

    private val libraryNames: List<String>
        get() = configuration.getList(KonanConfigKeys.LIBRARY_FILES)

    private val includedLibraryFiles
        get() = configuration.getList(KonanConfigKeys.INCLUDED_LIBRARIES).map { File(it) }

    private val librariesToCacheFiles
        get() = configuration.getList(KonanConfigKeys.LIBRARIES_TO_CACHE).map { File(it) }

    private val unresolvedLibraries = libraryNames.toUnresolvedLibraries

    private val repositories = configuration.getList(KonanConfigKeys.REPOSITORIES)
    private val resolverLogger =
        object : Logger {
            private val collector = configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            override fun warning(message: String)= collector.report(STRONG_WARNING, message)
            override fun error(message: String) = collector.report(ERROR, message)
            override fun log(message: String) = collector.report(LOGGING, message)
            override fun fatal(message: String): Nothing {
                collector.report(ERROR, message)
                (collector as? GroupingMessageCollector)?.flush()
                exitProcess(1)
            }
        }

    private val compatibleCompilerVersions: List<KonanVersion> =
        configuration.getList(KonanConfigKeys.COMPATIBLE_COMPILER_VERSIONS).map { it.parseKonanVersion() }

    private val resolver = defaultResolver(
        repositories,
        libraryNames.filter { it.contains(File.separator) },
        target,
        distribution,
        compatibleCompilerVersions,
        resolverLogger
    ).libraryResolver()

    // We pass included libraries by absolute paths to avoid repository-based resolution for them.
    // Strictly speaking such "direct" libraries should be specially handled by the resolver, not by KonanConfig.
    // But currently the resolver is in the middle of a complex refactoring so it was decided to avoid changes in its logic.
    // TODO: Handle included libraries in KonanLibraryResolver when it's refactored and moved into the big Kotlin repo.
    internal val resolvedLibraries by lazy {
        val additionalLibraryFiles = includedLibraryFiles + librariesToCacheFiles
        resolver.resolveWithDependencies(
                unresolvedLibraries + additionalLibraryFiles.map { UnresolvedLibrary(it.absolutePath, null) },
                noStdLib = configuration.getBoolean(KonanConfigKeys.NOSTDLIB),
                noDefaultLibs = configuration.getBoolean(KonanConfigKeys.NODEFAULTLIBS),
                noEndorsedLibs = configuration.getBoolean(KonanConfigKeys.NOENDORSEDLIBS)
        )
    }

    internal val exportedLibraries by lazy {
        getExportedLibraries(configuration, resolvedLibraries, resolver.searchPathResolver, report = true)
    }

    internal val coveredLibraries by lazy {
        getCoveredLibraries(configuration, resolvedLibraries, resolver.searchPathResolver)
    }

    internal val includedLibraries by lazy {
        getIncludedLibraries(includedLibraryFiles, configuration, resolvedLibraries)
    }

    internal val cacheSupport: CacheSupport by lazy {
        CacheSupport(configuration, resolvedLibraries, target, produce)
    }

    internal val cachedLibraries: CachedLibraries
        get() = cacheSupport.cachedLibraries

    internal val librariesToCache: Set<KotlinLibrary>
        get() = cacheSupport.librariesToCache

    fun librariesWithDependencies(moduleDescriptor: ModuleDescriptor?): List<KonanLibrary> {
        if (moduleDescriptor == null) error("purgeUnneeded() only works correctly after resolve is over, and we have successfully marked package files as needed or not needed.")

        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(TopologicalLibraryOrder) as List<KonanLibrary>
    }

    val shouldCoverSources = configuration.getBoolean(KonanConfigKeys.COVERAGE)
    val shouldCoverLibraries = !configuration.getList(KonanConfigKeys.LIBRARIES_TO_COVER).isNullOrEmpty()

    internal val runtimeNativeLibraries: List<String> = mutableListOf<String>().apply {
        add(if (debug) "debug.bc" else "release.bc")
        add(if (memoryModel == MemoryModel.STRICT) "strict.bc" else "relaxed.bc")
        if (shouldCoverLibraries || shouldCoverSources) add("profileRuntime.bc")
    }.map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val launcherNativeLibraries: List<String> = distribution.launcherFiles.map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val objCNativeLibraries: List<String> = listOf("objc.bc").map {
        File(distribution.defaultNatives(target)).child(it).absolutePath
    }

    internal val nativeLibraries: List<String> = 
        configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    internal val includeBinaries: List<String> = 
        configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    internal val defaultSystemLibraries: List<String> = emptyList()

    internal val languageVersionSettings =
            configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!

    internal val friendModuleFiles: Set<File> =
            configuration.get(KonanConfigKeys.FRIEND_MODULES)?.map { File(it) }?.toSet() ?: emptySet()

    internal val manifestProperties = configuration.get(KonanConfigKeys.MANIFEST_FILE)?.let {
        File(it).loadProperties()
    }

    internal val isInteropStubs: Boolean get() = manifestProperties?.getProperty("interop") == "true"
}

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String) 
    = this.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)


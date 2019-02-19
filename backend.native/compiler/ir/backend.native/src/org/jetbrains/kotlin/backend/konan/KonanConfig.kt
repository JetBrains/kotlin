/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.STRONG_WARNING
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.library.libraryResolver
import org.jetbrains.kotlin.konan.properties.loadProperties
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.KonanAbiVersion
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.konan.library.toUnresolvedLibraries
import org.jetbrains.kotlin.konan.parseKonanVersion

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    internal val distribution = Distribution(
        false,
        null,
        configuration.get(KonanConfigKeys.RUNTIME_FILE))

    internal val platformManager = PlatformManager(distribution)
    internal val targetManager = platformManager.targetManager(configuration.get(KonanConfigKeys.TARGET))
    internal val target = targetManager.target

    val linkOnly: Boolean =
            configuration.kotlinSourceRoots.isEmpty() && libraryNames.isNotEmpty() && produce.isNativeBinary

    val infoArgsOnly = configuration.kotlinSourceRoots.isEmpty() && !linkOnly

    val debug: Boolean get() = configuration.getBoolean(KonanConfigKeys.DEBUG)

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

    val outputFiles = OutputFiles(configuration.get(KonanConfigKeys.OUTPUT), target, produce)
    val tempFiles = TempFiles(outputFiles.outputName, configuration.get(KonanConfigKeys.TEMPORARY_FILES_DIR))

    val outputFile = outputFiles.mainFile

    val moduleId: String
        get() = configuration.get(KonanConfigKeys.MODULE_NAME) ?: File(outputFiles.outputName).name

    internal val purgeUserLibs: Boolean
        get() = configuration.getBoolean(KonanConfigKeys.PURGE_USER_LIBS)

    private val libraryNames: List<String>
        get() = configuration.getList(KonanConfigKeys.LIBRARY_FILES)

    private val unresolvedLibraries = libraryNames.toUnresolvedLibraries

    private val repositories = configuration.getList(KonanConfigKeys.REPOSITORIES)
    private fun resolverLogger(msg: String) = configuration.report(STRONG_WARNING, msg)

    private val compatibleCompilerVersions: List<KonanVersion> =
        configuration.getList(KonanConfigKeys.COMPATIBLE_COMPILER_VERSIONS).map { it.parseKonanVersion() }

    private val resolver = defaultResolver(
        repositories,
        libraryNames.filter { it.contains(File.separator) },
        target,
        distribution,
        ::resolverLogger,
        compatibleCompilerVersions = compatibleCompilerVersions 
    ).libraryResolver()

    internal val resolvedLibraries by lazy {
        resolver.resolveWithDependencies(
                unresolvedLibraries,
                noStdLib = configuration.getBoolean(KonanConfigKeys.NOSTDLIB),
                noDefaultLibs = configuration.getBoolean(KonanConfigKeys.NODEFAULTLIBS)).also {

            validateExportedLibraries(configuration, it)
        }
    }

    fun librariesWithDependencies(moduleDescriptor: ModuleDescriptor?): List<KonanLibrary> {
        if (moduleDescriptor == null) error("purgeUnneeded() only works correctly after resolve is over, and we have successfully marked package files as needed or not needed.")

        return resolvedLibraries.filterRoots { (!it.isDefault && !this.purgeUserLibs) || it.isNeededForLink }.getFullList(TopologicalLibraryOrder)
    }

    internal val defaultNativeLibraries: List<String> = mutableListOf<String>().apply {
        add(if (debug) "debug.bc" else "release.bc")
        if (produce == CompilerOutputKind.PROGRAM) {
            addAll(distribution.launcherFiles)
        }
    }.map {
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


/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.konan.descriptors.createForwardDeclarationsModule
import org.jetbrains.kotlin.backend.konan.library.*
import org.jetbrains.kotlin.backend.konan.library.impl.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.kotlinSourceRoots
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.TempFiles
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.profile
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    val currentAbiVersion: Int = configuration.get(KonanConfigKeys.ABI_VERSION)!!

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

    private val repositories = configuration.getList(KonanConfigKeys.REPOSITORIES)
    private val resolver = defaultResolver(repositories, target, distribution)

    internal val immediateLibraries: List<LibraryReaderImpl> by lazy {
        val result = resolver.resolveImmediateLibraries(libraryNames, target,
                currentAbiVersion,
                configuration.getBoolean(KonanConfigKeys.NOSTDLIB),
                configuration.getBoolean(KonanConfigKeys.NODEFAULTLIBS),
                { msg -> configuration.report(STRONG_WARNING, msg) })
        resolver.resolveLibrariesRecursive(result, target, currentAbiVersion)
        result
    }

    fun librariesWithDependencies(moduleDescriptor: ModuleDescriptor?): List<KonanLibraryReader> {
        if (moduleDescriptor == null) error("purgeUnneeded() only works correctly after resolve is over, and we have successfully marked package files as needed or not needed.")

        return immediateLibraries.purgeUnneeded(this).withResolvedDependencies()
    }

    private val loadedDescriptors = loadLibMetadata()

    internal lateinit var friends:Set<ModuleDescriptorImpl>

    internal val defaultNativeLibraries = 
        if (produce == CompilerOutputKind.PROGRAM) 
            File(distribution.defaultNatives(target)).listFiles.map { it.absolutePath } 
        else emptyList()

    internal val nativeLibraries: List<String> = 
        configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    internal val includeBinaries: List<String> = 
        configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    fun loadLibMetadata(): List<ModuleDescriptorImpl> {

        val allMetadata = mutableListOf<ModuleDescriptorImpl>()
        val specifics = configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!

        val libraries = immediateLibraries.withResolvedDependencies()
        val friendLibsSet = configuration.get(KonanConfigKeys.FRIEND_MODULES)?.map{File(it)}?.toSet()
        val friends = mutableListOf<ModuleDescriptorImpl>()
        for (klib in libraries) {
            profile("Loading ${klib.libraryName}") {
                // MutableModuleContext needs ModuleDescriptorImpl, rather than ModuleDescriptor.
                val moduleDescriptor = klib.moduleDescriptor(specifics)
                allMetadata.add(moduleDescriptor)
                friendLibsSet?.apply {
                    if (contains(klib.libraryFile))
                        friends.add(moduleDescriptor)
                }
            }
        }
        this.friends = friends.toSet()
        return allMetadata
    }

    private var forwardDeclarationsModule: ModuleDescriptorImpl? = null

    internal fun getOrCreateForwardDeclarationsModule(
            builtIns: KotlinBuiltIns, storageManager: StorageManager? = null
    ): ModuleDescriptorImpl {
        forwardDeclarationsModule?.let { return it }
        val result = createForwardDeclarationsModule(
                builtIns,
                storageManager ?: LockBasedStorageManager()
        )

        forwardDeclarationsModule = result
        return result
    }

    internal val moduleDescriptors: List<ModuleDescriptorImpl> by lazy {
        for (module in loadedDescriptors) {
            // Yes, just to all of them.
            module.setDependencies(loadedDescriptors + getOrCreateForwardDeclarationsModule(module.builtIns))
        }

        loadedDescriptors
    }
}

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String) 
    = this.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)

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
import org.jetbrains.kotlin.backend.konan.library.KonanLibraryReader
import org.jetbrains.kotlin.backend.konan.library.KonanLibrarySearchPathResolver
import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.backend.konan.util.profile
import org.jetbrains.kotlin.backend.konan.util.removeSuffixIfPresent
import org.jetbrains.kotlin.backend.konan.util.suffixIfNot
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.target.TargetManager.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind.*
import org.jetbrains.kotlin.konan.util.DependencyProcessor

class KonanConfig(val project: Project, val configuration: CompilerConfiguration) {

    val currentAbiVersion: Int = configuration.get(KonanConfigKeys.ABI_VERSION)!!

    internal val targetManager = TargetManager(
        configuration.get(KonanConfigKeys.TARGET))

    init {
        val target = targetManager.target
        if (!target.enabled) {
            error("Target $target is not available on the ${TargetManager.host} host")
        }
    }

    private fun Distribution.prepareDependencies(checkDependencies: Boolean) {
        if (checkDependencies) {
            DependencyProcessor(java.io.File(dependenciesDir), targetProperties).run()
        }
    }

    internal val distribution = Distribution(targetManager, 
        configuration.get(KonanConfigKeys.PROPERTY_FILE),
        configuration.get(KonanConfigKeys.RUNTIME_FILE)).apply {
        prepareDependencies(configuration.getBoolean(KonanConfigKeys.CHECK_DEPENDENCIES))
    }

    private val produce = configuration.get(KonanConfigKeys.PRODUCE)!!
    private val suffix = produce.suffix(targetManager.target)
    val outputName = configuration.get(KonanConfigKeys.OUTPUT)?.removeSuffixIfPresent(suffix) ?: produce.name.toLowerCase()
    val outputFile = outputName.suffixIfNot(produce.suffix(targetManager.target))

    val moduleId: String
        // This is a decision we could change
        get() = outputName

    private val libraryNames: List<String>
        get() {
            val fromCommandLine = configuration.getList(KonanConfigKeys.LIBRARY_FILES)
            if (configuration.get(KonanConfigKeys.NOSTDLIB) ?: false) {
                return fromCommandLine
            }
            return fromCommandLine + "stdlib"
        }

    private val repositories = configuration.getList(KonanConfigKeys.REPOSITORIES)
    private val resolver = KonanLibrarySearchPathResolver(repositories, distribution.klib, distribution.localKonanDir)
    private val librariesFound: List<File> by lazy {
        val resolvedLibraries = libraryNames.map{it -> resolver.resolve(it)}
        checkLibraryDuplicates(resolvedLibraries)
        resolvedLibraries
    }

    internal val libraries: List<KonanLibraryReader> by lazy {
        val target = targetManager.target
        // Here we have chosen a particular KonanLibraryReader implementation.
        librariesFound.map{it -> LibraryReaderImpl(it, currentAbiVersion, target)}
    }

    private val loadedDescriptors = loadLibMetadata()

    internal val nativeLibraries: List<String> = 
        configuration.getList(KonanConfigKeys.NATIVE_LIBRARY_FILES)

    internal val includeBinaries: List<String> = 
        configuration.getList(KonanConfigKeys.INCLUDED_BINARY_FILES)

    fun loadLibMetadata(): List<ModuleDescriptorImpl> {

        val allMetadata = mutableListOf<ModuleDescriptorImpl>()
        val specifics = configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!

        for (klib in libraries) {
            profile("Loading ${klib.libraryName}") {
                // MutableModuleContext needs ModuleDescriptorImpl, rather than ModuleDescriptor.
                val moduleDescriptor = klib.moduleDescriptor(specifics) as ModuleDescriptorImpl
                allMetadata.add(moduleDescriptor)
            }
        }
        return allMetadata
    }

    internal val moduleDescriptors: List<ModuleDescriptorImpl> by lazy {
        for (module in loadedDescriptors) {
            // Yes, just to all of them.
            module.setDependencies(loadedDescriptors)
        }

        loadedDescriptors
    }

    private fun checkLibraryDuplicates(resolvedLibraries: List<File>) {
        val duplicates = resolvedLibraries.groupBy { it.absolutePath } .values.filter { it.size > 1 }
        duplicates.forEach {
            configuration.report(STRONG_WARNING, "library included more than once: ${it.first().absolutePath}")
        }
    }
}

fun CompilerConfiguration.report(priority: CompilerMessageSeverity, message: String) 
    = this.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(priority, message)

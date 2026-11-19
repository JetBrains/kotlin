/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import org.jetbrains.kotlin.cli.CliDiagnostics.JAVA_MODULE_RESOLUTION_ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.jvm.index.JavaRoot
import org.jetbrains.kotlin.cli.jvm.index.JvmClasspathRootMatcher
import org.jetbrains.kotlin.cli.jvm.index.JvmRootEntryFactory
import org.jetbrains.kotlin.cli.report
import org.jetbrains.kotlin.cli.reportLog
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.jvm.environment.JvmClasspath
import org.jetbrains.kotlin.jvm.environment.JvmClasspathRootId
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartProviderBase
import org.jetbrains.kotlin.load.kotlin.loadModuleMapping
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.ModuleMapping
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.PrintStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads `.kotlin_module` mappings from the binary roots accepted by [acceptsRoot].
 *
 * Compiler code uses the [JvmClasspath] constructors. The root filter is an implementation seam for hosts that select roots in
 * their own terms (e.g. Analysis API adapters over `GlobalSearchScope`); it is not meant to be exposed through compiler APIs.
 */
class JvmPackagePartProvider(
    private val moduleMappings: JvmModuleMappings,
    private val acceptsRoot: (JavaRoot) -> Boolean,
) : JvmPackagePartProviderBase<JvmClasspathRootId>() {
    constructor(languageVersionSettings: LanguageVersionSettings, acceptsRoot: (VirtualFile) -> Boolean) :
            this(JvmModuleMappings(languageVersionSettings), { root: JavaRoot -> acceptsRoot(root.file) })

    constructor(moduleMappings: JvmModuleMappings, classpath: JvmClasspath) :
            this(moduleMappings, classpath.rootFilter())

    constructor(languageVersionSettings: LanguageVersionSettings, classpath: JvmClasspath) :
            this(JvmModuleMappings(languageVersionSettings), classpath)

    override val deserializationConfiguration: CommonCompilerDeserializationConfiguration
        get() = moduleMappings.deserializationConfiguration

    override val loadedModules: MutableList<ModuleMappingInfo<JvmClasspathRootId>> = SmartList()

    private var allPackageNamesCache: Set<String>? = null

    override val allPackageNames: Set<String>
        // assuming that the modifications of loadedModules happen in predictable moments now, so no synchronization is used
        get() = allPackageNamesCache
            ?: loadedModules.flatMapTo(mutableSetOf()) { it.mapping.packageFqName2Parts.keys }
                .also { allPackageNamesCache = it }

    // TODO: redesign to avoid cache-unfriendly usages, see KT-76516
    fun addRoots(roots: List<JavaRoot>, configuration: CompilerConfiguration) {
        for (root in roots) {
            if (root.type != JavaRoot.RootType.BINARY) continue
            if (!acceptsRoot(root)) continue

            for (moduleFile in moduleMappings.moduleFiles(root)) {
                moduleFile.reportErrors(configuration)
                val mapping = moduleFile.mapping ?: continue
                loadedModules.add(ModuleMappingInfo(root.id, mapping, moduleFile.name))
                allPackageNamesCache = null
            }
        }
    }
}

/**
 * The parsed `.kotlin_module` files in `META-INF` of binary roots, read through the entries of [rootEntries] once per root and shared
 * by the [JvmPackagePartProvider]s of an environment.
 */
class JvmModuleMappings(
    languageVersionSettings: LanguageVersionSettings,
    private val rootEntries: JvmRootEntryFactory = JvmRootEntryFactory.VFS,
) {
    val deserializationConfiguration: CommonCompilerDeserializationConfiguration =
        CommonCompilerDeserializationConfiguration(languageVersionSettings)

    private val moduleFilesByRoot = ConcurrentHashMap<JavaRoot, List<ModuleFile>>()

    internal fun moduleFiles(root: JavaRoot): List<ModuleFile> = moduleFilesByRoot.computeIfAbsent(root, ::readModuleFiles)

    private fun readModuleFiles(root: JavaRoot): List<ModuleFile> {
        val metaInf = rootEntries.createRootEntry(root).findChild("META-INF") ?: return emptyList()
        return metaInf.children.filter { it.name.endsWith(ModuleMapping.MAPPING_FILE_EXT) }.map { entry ->
            val handle = entry.toHandle(root)
            var incompatibleVersion: MetadataVersion? = null
            var readError: EOFException? = null
            val mapping = try {
                ModuleMapping.loadModuleMapping(handle.readBytes(), handle.toString(), deserializationConfiguration) {
                    incompatibleVersion = it
                }
            } catch (e: EOFException) {
                readError = e
                null
            }
            ModuleFile(entry.nameWithoutExtension, handle.pathForDiagnostics, mapping, incompatibleVersion, readError)
        }
    }

    internal class ModuleFile(
        val name: String,
        private val path: String,
        val mapping: ModuleMapping?,
        private val incompatibleVersion: MetadataVersion?,
        private val readError: EOFException?,
    ) {
        fun reportErrors(configuration: CompilerConfiguration) {
            incompatibleVersion?.let { reportIncompatibleModuleMapping(it, path, configuration) }
            readError?.let { reportModuleMappingReadError(it, path, configuration) }
        }
    }
}

private fun JvmClasspath.rootFilter(): (JavaRoot) -> Boolean {
    val matcher = JvmClasspathRootMatcher(this)
    return { root -> matcher.accepts(root.file) }
}

fun tryLoadModuleMapping(
    getModuleBytes: () -> ByteArray,
    debugName: String,
    modulePath: String,
    deserializationConfiguration: CommonCompilerDeserializationConfiguration,
    configuration: CompilerConfiguration
): ModuleMapping? = try {
    ModuleMapping.loadModuleMapping(getModuleBytes(), debugName, deserializationConfiguration) { incompatibleVersion ->
        reportIncompatibleModuleMapping(incompatibleVersion, modulePath, configuration)
    }
} catch (e: EOFException) {
    reportModuleMappingReadError(e, modulePath, configuration)
    null
}

private fun reportIncompatibleModuleMapping(
    incompatibleVersion: MetadataVersion,
    modulePath: String,
    configuration: CompilerConfiguration,
) {
    configuration.report(
        JAVA_MODULE_RESOLUTION_ERROR,
        "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is " +
                "$incompatibleVersion, expected version is ${MetadataVersion.INSTANCE}.",
        CompilerMessageLocation.create(modulePath)
    )
}

private fun reportModuleMappingReadError(e: EOFException, modulePath: String, configuration: CompilerConfiguration) {
    configuration.report(
        JAVA_MODULE_RESOLUTION_ERROR,
        "Error occurred when reading the module: ${e.message}", CompilerMessageLocation.create(modulePath)
    )
    configuration.reportLog(
        String(ByteArrayOutputStream().also { e.printStackTrace(PrintStream(it)) }.toByteArray()),
        CompilerMessageLocation.create(modulePath)
    )
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootBase
import org.jetbrains.kotlin.cli.jvm.modules.ALL_UNNAMED
import org.jetbrains.kotlin.cli.jvm.modules.CliJavaModuleFinder
import org.jetbrains.kotlin.cli.jvm.modules.JavaModuleAccess
import org.jetbrains.kotlin.cli.jvm.modules.parseJavaModuleAccessOption
import org.jetbrains.kotlin.cli.jvm.modules.parseJavaModulePatch
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModule
import java.io.File

/**
 * A Kotlin or Java source root, and the extension of the source files compiled from it.
 */
internal data class SourceRootFile(val file: File, val extension: String)

/**
 * Resolves `-Xadd-reads`, `-Xadd-exports`, and `-Xpatch-module` against the module graph, the way javac resolves `--add-reads`,
 * `--add-exports`, and `--patch-module`.
 */
internal class JavaModuleOptionsResolver(
    configuration: CompilerConfiguration,
    private val javaModuleFinder: CliJavaModuleFinder,
    private val contentRootToVirtualFile: (JvmContentRootBase) -> VirtualFile?,
    private val jdkRelease: Int?,
    private val reportError: (String) -> Unit,
    private val reportWarning: (String) -> Unit,
) {
    private val additionalReads = configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULE_READS)
    private val additionalExports = configuration.getList(JVMConfigurationKeys.ADDITIONAL_JAVA_MODULE_EXPORTS)

    // Values of -Xpatch-module, from a module name to its paths. Each module is patched at most once, which is checked in jvmArguments.kt.
    private val patches: Map<String, List<File>> =
        configuration.getList(JVMConfigurationKeys.JAVA_MODULE_PATCHES).mapNotNull(::parseJavaModulePatch)
            .associate { it.first to it.second.map(::File) }

    private val patchRootsByModule = hashMapOf<String, List<JavaModule.Root>>()

    /**
     * The roots given with `-Xpatch-module` for any module.
     */
    val patchRootFiles: Set<VirtualFile> by lazy(LazyThreadSafetyMode.NONE) {
        patches.keys.flatMapTo(hashSetOf()) { moduleName -> patchRoots(moduleName).map(JavaModule.Root::file) }
    }

    /**
     * The roots given with `-Xpatch-module` for the module named [moduleName], leaving out the paths that do not exist.
     */
    fun patchRoots(moduleName: String): List<JavaModule.Root> = patchRootsByModule.getOrPut(moduleName) {
        patches[moduleName].orEmpty().filter(File::exists).mapNotNull { file ->
            contentRootToVirtualFile(JvmClasspathRoot(file, isSdkRoot = false))?.let { JavaModule.Root(it, isBinary = true) }
        }
    }

    /**
     * Like javac, finds the module given with `-Xpatch-module` that the sources are members of: the module with a patch path that contains
     * the real path of a source file being compiled. A file source root is compiled whatever its extension, such as a script, and a
     * directory source root is compiled recursively.
     */
    fun findModuleWithPatchedSources(sourceRootFiles: List<SourceRootFile>): String? {
        if (patches.isEmpty()) return null
        val sourceFiles = sourceRootFiles.flatMap { (val file, val extension) ->
            file.walk().filter { it.isFile && (it == file || it.extension == extension) }.map(File::getCanonicalFile).toList()
        }
        val moduleNames = patches.filterValues { paths ->
            paths.any { path ->
                val patchPath = path.canonicalFile
                sourceFiles.any { FileUtil.isAncestor(patchPath, it, true) }
            }
        }.keys
        if (moduleNames.size > 1) {
            reportError("Too many patched modules: ${moduleNames.joinToString()}")
            return null
        }
        return moduleNames.singleOrNull()
    }

    /**
     * Patches the modules in [modulesInGraph] other than [patchedSourceModuleName], and returns the values of `-Xadd-reads` and
     * `-Xadd-exports` for the module graph. Like javac, which puts the classpath roots in the unnamed module, the [modulesFromClasspath]
     * are not patched, and the options for them are left out.
     */
    fun resolve(modulesInGraph: Set<String>, modulesFromClasspath: Set<String>, patchedSourceModuleName: String?): JavaModuleAccess {
        // Like javac, a patch of a module outside the module graph has no effect.
        for (moduleName in patches.keys) {
            if (moduleName != patchedSourceModuleName && moduleName in modulesInGraph && moduleName !in modulesFromClasspath) {
                javaModuleFinder.patchModule(moduleName, patchRoots(moduleName))
            }
        }
        val namedModulesInGraph = modulesInGraph - modulesFromClasspath
        val systemModuleNames =
            if (jdkRelease == null) emptySet() else javaModuleFinder.systemModules.mapTo(hashSetOf(), JavaModule::name)
        return JavaModuleAccess(
            collectAccessOptions(additionalReads, namedModulesInGraph, systemModuleNames, isExports = false),
            collectAccessOptions(additionalExports, namedModulesInGraph, systemModuleNames, isExports = true),
            modulesFromClasspath,
        )
    }

    // Like javac, report and leave out the modules in -Xadd-reads and -Xadd-exports that are not in the module graph, and reject system
    // modules on the left-hand side with -Xjdk-release.
    private fun collectAccessOptions(
        values: List<String>,
        modulesInGraph: Set<String>,
        systemModuleNames: Set<String>,
        isExports: Boolean,
    ): Map<String, Set<String>> {
        val flag = if (isExports) "-Xadd-exports" else "-Xadd-reads"

        fun isInGraph(name: String): Boolean =
            (name == ALL_UNNAMED || name in modulesInGraph).also { if (!it) reportWarning("Module name in `$flag` not found: $name") }

        val result = HashMap<String, MutableSet<String>>()
        for (value in values) {
            val option = parseJavaModuleAccessOption(value, isExports) ?: continue
            val source = option.source ?: continue
            val moduleName = source.substringBefore('/')
            if (!isInGraph(moduleName)) continue
            if (moduleName in systemModuleNames) {
                reportError(
                    if (isExports) "Exporting a package from system module $moduleName is not allowed with `-Xjdk-release`"
                    else "Adding read edges for system module $moduleName is not allowed with `-Xjdk-release`"
                )
                continue
            }
            result.getOrPut(source, ::HashSet) += option.otherModules.filter(::isInGraph)
        }
        return result
    }
}

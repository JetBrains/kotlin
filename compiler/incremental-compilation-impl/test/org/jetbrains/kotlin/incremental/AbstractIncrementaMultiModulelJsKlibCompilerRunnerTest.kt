/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.utils.*
import org.jetbrains.kotlin.library.KLIB_FILE_EXTENSION
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import java.util.regex.Pattern

abstract class AbstractIncrementalMultiModuleJsKlibCompilerRunnerTest : AbstractIncrementalJsKlibCompilerRunnerTest() {

    private class ModuleBuildConfiguration(val srcDir: File, val dependencies: List<String>)

    private val modulesDir: File by lazy { File(workingDir, "modules") }
    private val modulesInfo: MutableMap<String, ModuleBuildConfiguration> = mutableMapOf()
    private val modulesOrder: MutableList<String> = mutableListOf()

    private val dirToModule = mutableMapOf<File, IncrementalModuleEntry>()
    private val nameToModules = mutableMapOf<String, MutableSet<IncrementalModuleEntry>>()
    private val jarToModule = mutableMapOf<File, IncrementalModuleEntry>()

    private val modulesApiHistory: ModulesApiHistoryJs by lazy {
        ModulesApiHistoryJs(IncrementalModuleInfo(workingDir, dirToModule, nameToModules, emptyMap(), jarToModule))
    }

    override val moduleNames: Collection<String>? get() = modulesOrder

    override fun resetTest(testDir: File, newOutDir: File, newCacheDir: File) {
        modulesDir.deleteRecursively()
        modulesDir.mkdirs()

        dirToModule.clear()
        nameToModules.clear()
        jarToModule.clear()

        modulesOrder.forEach { setupModuleApiHistory(it, newOutDir, newCacheDir) }
    }

    override fun setupTest(testDir: File, srcDir: File, cacheDir: File, outDir: File): List<File> {
        val ktFiles = srcDir.getFiles().filter { it.extension == "kt" }

        val results = mutableMapOf<String, MutableList<Pair<File, String>>>()
        ktFiles.forEach {
            modulePattern.matcher(it.name).let { match ->
                match.find()
                val moduleName = match.group(1)
                val fileName = match.group(2)
                val sources = results.getOrPut(moduleName) { mutableListOf() }
                sources.add(it to fileName)
            }
        }

        val dependencyGraph = parseDependencies(testDir)

        DFS.topologicalOrder(dependencyGraph.keys) { m ->
            dependencyGraph[m] ?: error("Expected dependencies for module $m")
        }.reversed().mapTo(modulesOrder) { it }

        for ((moduleName, fileEntries) in results) {
            val moduleDir = File(workingDir, moduleName).apply { mkdirs() }
            val moduleSrcDir = File(moduleDir, "src")

            val moduleDependencies = dependencyGraph[moduleName] ?: error("Cannot find dependency for module $moduleName")

            for ((oldFile, newName) in fileEntries) {
                val newFile = File(moduleSrcDir, newName)
                oldFile.copyTo(newFile)
            }

            modulesInfo[moduleName] = ModuleBuildConfiguration(moduleSrcDir, moduleDependencies)

            setupModuleApiHistory(moduleName, outDir, cacheDir)
        }

        return listOf(srcDir)
    }

    private fun setupModuleApiHistory(moduleName: String, outDir: File, cacheDir: File) {
        val depKlibFile = File(modulesDir, moduleName.klib)
        val moduleBuildDir = File(outDir, moduleName)
        val moduleCacheDir = File(cacheDir, moduleName)
        val moduleBuildHistoryFile = buildHistoryFile(moduleCacheDir)

        val moduleEntry = IncrementalModuleEntry(workingDir.absolutePath, moduleName, outDir, moduleBuildHistoryFile)

        dirToModule[moduleBuildDir] = moduleEntry
        nameToModules.getOrPut(moduleName) { mutableSetOf() }.add(moduleEntry)
        jarToModule[depKlibFile] = moduleEntry
    }

    companion object {

        private val modulePattern = Pattern.compile("^(module\\d+)_(\\w+\\.kt)$")

        private fun File.getFiles(): List<File> {
            return if (isDirectory) listFiles()?.flatMap { it.getFiles() } ?: emptyList()
            else listOf(this)
        }

        private val String.klib: String get() = "$this.$KLIB_FILE_EXTENSION"

        private fun parseDependencies(testDir: File): Map<String, List<String>> {

            val actualModulesTxtFile = File(testDir, "dependencies.txt")

            if (!actualModulesTxtFile.exists()) {
                error("${actualModulesTxtFile.path} is expected")
            }

            val result = mutableMapOf<String, MutableList<String>>()

            val lines = actualModulesTxtFile.readLines()
            lines.map { it.split("->") }.map {
                assert(it.size == 2)
                val moduleName = it[0]
                val dependencyPart = it[1]

                val idx = dependencyPart.indexOf('[')
                val dependencyName = if (idx >= 0) {
                    // skip annotations
                    dependencyPart.substring(0, idx)
                } else dependencyPart

                val dependencies = result.getOrPut(moduleName) { mutableListOf() }
                if (dependencyName.isNotBlank()) {
                    dependencies.add(dependencyName)
                }
            }

            return result
        }
    }

    private fun K2JSCompilerArguments.updateCompilerArguments(
        moduleDependencies: List<String>,
        initialDeps: String,
        destinationFile: File
    ) {
        val additionalDeps = moduleDependencies.joinToString(File.pathSeparator) {
            File(modulesDir, it.klib).absolutePath
        }

        val sb = StringBuilder(initialDeps)
        if (additionalDeps.isNotBlank()) {
            sb.append(File.pathSeparator)
            sb.append(additionalDeps)
        }

        libraries = sb.toString()
        outputFile = destinationFile.path
    }

    override fun make(
        cacheDir: File,
        outDir: File,
        sourceRoots: Iterable<File>,
        args: K2JSCompilerArguments
    ): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()
        val initialDeps = args.libraries ?: ""

        args.repositries = modulesDir.path

        val modifiedLibraries = mutableListOf<Pair<String, File>>()
        val deletedLibraries = mutableListOf<Pair<String, File>>()

        var compilationIsEnabled = true
        val isInitial = modulesDir.list()?.isEmpty() ?: true

        for (module in modulesOrder) {
            val moduleBuildInfo = modulesInfo[module] ?: error("Cannot find config for $module")

            val moduleDependencies = moduleBuildInfo.dependencies

            val moduleModifiedDependencies = modifiedLibraries.filter { it.first in moduleDependencies }.map { it.second }
            val moduleDeletedDependencies = deletedLibraries.filter { it.first in moduleDependencies }.map { it.second }

            val changedDepsFiles = if (isInitial) null else ChangedFiles.Dependencies(moduleModifiedDependencies, moduleDeletedDependencies)

            val moduleOutDir = File(outDir, module)
            val moduleCacheDir = File(cacheDir, module)
            val moduleBuildHistory = buildHistoryFile(moduleCacheDir)
            val sources = moduleBuildInfo.srcDir.getFiles()

            val outputKlibFile = File(moduleOutDir, module.klib)
            val dependencyFile = File(modulesDir, module.klib)

            args.updateCompilerArguments(moduleDependencies, initialDeps, outputKlibFile)

            if (compilationIsEnabled) {
                makeJsIncrementally(
                    moduleCacheDir,
                    sources,
                    args,
                    moduleBuildHistory,
                    messageCollector,
                    reporter,
                    scopeExpansionMode,
                    modulesApiHistory,
                    changedDepsFiles
                )
            }

            val oldMD5 = if (dependencyFile.exists()) {
                val bytes = dependencyFile.readBytes()
                dependencyFile.delete()
                bytes.md5()
            } else 0

            if (!messageCollector.hasErrors()) {
                val newMD5 = outputKlibFile.readBytes().md5()
                if (oldMD5 != newMD5) {
                    modifiedLibraries.add(module to dependencyFile)
                }
                outputKlibFile.copyTo(dependencyFile)
            } else {
                compilationIsEnabled = false
            }
        }

        return TestCompilationResult(reporter, messageCollector)
    }
}
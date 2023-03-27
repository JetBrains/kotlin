/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.utils.TestCompilationResult
import org.jetbrains.kotlin.incremental.utils.TestICReporter
import org.jetbrains.kotlin.incremental.utils.TestMessageCollector
import org.jetbrains.kotlin.utils.DFS
import java.io.File
import java.util.regex.Pattern

abstract class AbstractIncrementalMultiModuleCompilerRunnerTest<Args : CommonCompilerArguments, ApiHistory : ModulesApiHistory> :
    AbstractIncrementalCompilerRunnerTestBase<Args>() {

    private class ModuleDependency(val moduleName: String, val flags: Set<String>)
    private class ModuleBuildConfiguration(val srcDir: File, val dependencies: List<ModuleDependency>)

    protected val repository: File by lazy { File(workingDir, "repository") }
    private val modulesInfo: MutableMap<String, ModuleBuildConfiguration> = mutableMapOf()
    private val modulesOrder: MutableList<String> = mutableListOf()

    private val dirToModule = mutableMapOf<File, IncrementalModuleEntry>()
    private val nameToModules = mutableMapOf<String, MutableSet<IncrementalModuleEntry>>()
    private val jarToClassListFile = mutableMapOf<File, File>()
    private val jarToModule = mutableMapOf<File, IncrementalModuleEntry>()
    private val jarToAbiSnapshot = mutableMapOf<File, File>()

    protected val incrementalModuleInfo: IncrementalModuleInfo by lazy {
        IncrementalModuleInfo(workingDir, dirToModule, nameToModules, jarToClassListFile, jarToModule, jarToAbiSnapshot)
    }

    protected abstract val modulesApiHistory: ApiHistory

    override val moduleNames: Collection<String>? get() = modulesOrder

    protected abstract val scopeExpansionMode: CompileScopeExpansionMode

    override fun resetTest(testDir: File, newOutDir: File, newCacheDir: File) {
        repository.deleteRecursively()
        repository.mkdirs()

        dirToModule.clear()
        nameToModules.clear()
        jarToModule.clear()

        modulesOrder.forEach { setupModuleApiHistory(it, newOutDir, newCacheDir) }
    }

    override fun setupTest(testDir: File, srcDir: File, cacheDir: File, outDir: File): List<File> {
        repository.mkdirs()
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
            (dependencyGraph[m] ?: error("Expected dependencies for module $m")).map { it.moduleName }
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

    protected open fun setupModuleApiHistory(moduleName: String, outDir: File, cacheDir: File) {
        val depArtifactFile = File(repository, moduleName.asArtifactFileName())
        val moduleBuildDir = File(outDir, moduleName)
        val moduleCacheDir = File(cacheDir, moduleName)
        val moduleBuildHistoryFile = buildHistoryFile(moduleCacheDir)
        val abiSnapshotFile = abiSnapshotFile(moduleCacheDir)

        val moduleEntry = IncrementalModuleEntry(workingDir.absolutePath, moduleName, outDir, moduleBuildHistoryFile, abiSnapshotFile)

        dirToModule[moduleBuildDir] = moduleEntry
        nameToModules.getOrPut(moduleName) { mutableSetOf() }.add(moduleEntry)
        jarToModule[depArtifactFile] = moduleEntry
    }

    companion object {

        private val modulePattern = Pattern.compile("^(module\\d+)_(\\w+\\.kt)$")

        private fun File.getFiles(): List<File> {
            return if (isDirectory) listFiles()?.flatMap { it.getFiles() } ?: emptyList()
            else listOf(this)
        }

        private fun parseDependencies(testDir: File): Map<String, List<ModuleDependency>> {

            val actualModulesTxtFile = File(testDir, "dependencies.txt")

            if (!actualModulesTxtFile.exists()) {
                error("${actualModulesTxtFile.path} is expected")
            }

            val result = mutableMapOf<String, MutableList<ModuleDependency>>()

            val lines = actualModulesTxtFile.readLines()
            lines.map { it.split("->") }.forEach {
                assert(it.size == 2)
                val moduleName = it[0]
                val dependencyPart = it[1]

                val dependencies = result.getOrPut(moduleName) { mutableListOf() }

                if (dependencyPart.isNotBlank()) {
                    val idx = dependencyPart.indexOf('[')
                    val dependency = if (idx >= 0) {
                        // skip annotations
                        val depModuleName = dependencyPart.substring(0, idx)
                        val flagsString = dependencyPart.substring(idx + 1, dependencyPart.length - 1)
                        val flags = flagsString.split(",").map { s -> s.trim() }.filter { s -> s.isNotEmpty() }.toSet()
                        ModuleDependency(depModuleName, flags)
                    } else ModuleDependency(dependencyPart, emptySet())
                    dependencies.add(dependency)
                }
            }

            return result
        }

        private const val EXPORTED = "exported"
    }

    protected abstract fun makeForSingleModule(
        moduleCacheDir: File,
        sourceRoots: Iterable<File>,
        args: Args,
        moduleBuildHistoryFile: File,
        messageCollector: MessageCollector,
        reporter: ICReporter,
        scopeExpansion: CompileScopeExpansionMode,
        modulesApiHistory: ApiHistory,
        providedChangedFiles: ChangedFiles?
    )

    private fun collectEffectiveDependencies(moduleName: String): List<String> {
        val result = mutableSetOf<String>()

        val moduleInfo = modulesInfo[moduleName] ?: error("Cannot find module info for $moduleName")

        for (dep in moduleInfo.dependencies) {
            val depName = dep.moduleName
            result.add(depName)

            val depInfo = modulesInfo[depName] ?: error("Cannot find module info for dependency $moduleName -> $depName")
            for (depdep in depInfo.dependencies) {
                if (EXPORTED in depdep.flags) {
                    result.add(depdep.moduleName)
                }
            }
        }

        return result.toList()
    }

    protected abstract fun Args.updateForSingleModule(moduleDependencies: List<String>, outFile: File)

    protected abstract fun String.asOutputFileName(): String
    protected abstract fun String.asArtifactFileName(): String

    protected abstract fun transformToDependency(moduleName: String, rawArtifact: File): File

    override fun make(
        cacheDir: File,
        outDir: File,
        sourceRoots: Iterable<File>,
        args: Args
    ): TestCompilationResult {
        val reporter = TestICReporter()
        val messageCollector = TestMessageCollector()

        val modifiedLibraries = mutableListOf<Pair<String, File>>()
        val deletedLibraries = mutableListOf<Pair<String, File>>()

        var compilationIsEnabled = true
        val isInitial = repository.list()?.isEmpty() ?: true

        for (module in modulesOrder) {
            val moduleDependencies = collectEffectiveDependencies(module)

            val moduleModifiedDependencies = modifiedLibraries.filter { it.first in moduleDependencies }.map { it.second }
            val moduleDeletedDependencies = deletedLibraries.filter { it.first in moduleDependencies }.map { it.second }

            val changedDepsFiles =
                if (isInitial) null else ChangedFiles.Known(moduleModifiedDependencies, moduleDeletedDependencies, forDependencies = true)

            val moduleOutDir = File(outDir, module)
            val moduleCacheDir = File(cacheDir, module)
            val moduleBuildHistory = buildHistoryFile(moduleCacheDir)

            val moduleBuildInfo = modulesInfo[module] ?: error("Cannot find config for $module")
            val sources = moduleBuildInfo.srcDir.getFiles()

            val outputFile = File(moduleOutDir, module.asOutputFileName())

            if (compilationIsEnabled) {
                args.updateForSingleModule(moduleDependencies, outputFile)
                makeForSingleModule(
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

            val dependencyFile = File(repository, module.asArtifactFileName())
            val oldMD5 = if (dependencyFile.exists()) {
                val bytes = dependencyFile.readBytes()
                dependencyFile.delete()
                bytes.md5()
            } else 0

            if (!messageCollector.hasErrors()) {
                transformToDependency(module, outputFile)
                val newMD5 = dependencyFile.readBytes().md5()
                if (oldMD5 != newMD5) {
                    modifiedLibraries.add(module to dependencyFile)
                }
            } else {
                compilationIsEnabled = false
            }
        }

        return TestCompilationResult(reporter, messageCollector)
    }
}
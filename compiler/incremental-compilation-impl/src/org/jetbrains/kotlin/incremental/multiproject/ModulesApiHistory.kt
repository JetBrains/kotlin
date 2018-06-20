/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.daemon.common.IncrementalModuleEntry
import org.jetbrains.kotlin.daemon.common.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.util.Either
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipFile

interface ModulesApiHistory {
    fun historyFilesForChangedFiles(changedFiles: Set<File>): Either<Set<File>>
}

object EmptyModulesApiHistory : ModulesApiHistory {
    override fun historyFilesForChangedFiles(changedFiles: Set<File>): Either<Set<File>> =
        Either.Error("Multi-module IC is not configured")
}

open class ModulesApiHistoryJvm(protected val modulesInfo: IncrementalModuleInfo) : ModulesApiHistory {
    protected val projectRootPath: Path = Paths.get(modulesInfo.projectRoot.absolutePath)
    private val dirToHistoryFileCache = HashMap<File, Set<File>>()

    override fun historyFilesForChangedFiles(changedFiles: Set<File>): Either<Set<File>> {
        val result = HashSet<File>()
        val jarFiles = ArrayList<File>()
        val classFiles = ArrayList<File>()

        for (file in changedFiles) {
            val extension = file.extension

            when {
                extension.equals("class", ignoreCase = true) -> {
                    classFiles.add(file)
                }
                extension.equals("jar", ignoreCase = true) -> {
                    jarFiles.add(file)
                }
            }
        }

        for (jar in jarFiles) {
            val historyEither = getBuildHistoryFilesForJar(jar)
            when (historyEither) {
                is Either.Success<Set<File>> -> result.addAll(historyEither.value)
                is Either.Error -> return historyEither
            }
        }

        val classFileDirs = classFiles.groupBy { it.parentFile }
        for ((dir, files) in classFileDirs) {
            val historyEither = getBuildHistoryForDir(dir)
            when (historyEither) {
                is Either.Success<Set<File>> -> result.addAll(historyEither.value)
                is Either.Error -> return historyEither
            }
        }

        return Either.Success(result)
    }

    protected open fun getBuildHistoryForDir(file: File): Either<Set<File>> {
        val history = dirToHistoryFileCache.getOrPut(file) {
            val module = modulesInfo.dirToModule[file]
            val parent = file.parentFile

            when {
                module != null ->
                    setOf(module.buildHistoryFile)
                parent != null && projectRootPath.isParentOf(parent) -> {
                    val parentHistory = getBuildHistoryForDir(parent)
                    when (parentHistory) {
                        is Either.Success<Set<File>> -> parentHistory.value
                        is Either.Error -> return parentHistory
                    }
                }
                else ->
                    return Either.Error("Unable to get build history for $file")
            }
        }
        return Either.Success(history)
    }

    protected open fun getBuildHistoryFilesForJar(jar: File): Either<Set<File>> {
        val classListFile = modulesInfo.jarToClassListFile[jar] ?: return Either.Error("Unknown jar: $jar")
        if (!classListFile.isFile) return Either.Error("Class list file does not exist $classListFile")

        val classFiles = try {
            classListFile.readText().split(File.pathSeparator).map(::File)
        } catch (t: Throwable) {
            return Either.Error("Could not read class list for $jar from $classListFile: $t")
        }

        val classFileDirs = classFiles.groupBy { it.parentFile }
        val result = HashSet<File>()
        for ((dir, files) in classFileDirs) {
            val historyEither = getBuildHistoryForDir(dir)
            when (historyEither) {
                is Either.Success<Set<File>> -> result.addAll(historyEither.value)
                is Either.Error -> return historyEither
            }
        }
        return Either.Success(result)
    }
}

class ModulesApiHistoryAndroid(modulesInfo: IncrementalModuleInfo) : ModulesApiHistoryJvm(modulesInfo) {
    override fun getBuildHistoryFilesForJar(jar: File): Either<Set<File>> {
        // Module detection is expensive, so we don't don it for jars outside of project dir
        if (!projectRootPath.isParentOf(jar)) return Either.Error("Non-project jar is modified $jar")

        val jarPath = Paths.get(jar.absolutePath)
        return getHistoryForModuleNames(jarPath, getPossibleModuleNamesFromJar(jarPath))
    }

    override fun getBuildHistoryForDir(file: File): Either<Set<File>> {
        if (!projectRootPath.isParentOf(file)) return Either.Error("Non-project file while looking for history $file")

        // check both meta-inf and META-INF directories
        val moduleNames =
            getPossibleModuleNamesForDir(file.resolve("meta-inf")) + getPossibleModuleNamesForDir(file.resolve("META-INF"))
        if (moduleNames.isEmpty()) {
            return if (file.parentFile == null) {
                Either.Error("Unable to find history for $file")
            } else {
                getBuildHistoryForDir(file.parentFile)
            }
        }

        return getHistoryForModuleNames(file.toPath(), moduleNames)
    }

    private fun getPossibleModuleNamesFromJar(path: Path): Collection<String> {
        val result = HashSet<String>()

        try {
            ZipFile(path.toFile()).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (name.endsWith(".kotlin_module", ignoreCase = true)) {
                        result.add(File(name).nameWithoutExtension)
                    }
                }
            }
        } catch (t: Throwable) {
            return emptyList()
        }

        return result
    }

    private fun getPossibleModuleNamesForDir(path: File): List<String> {
        if (!path.isDirectory) return listOf()

        return path.listFiles().filter { it.name.endsWith(".kotlin_module", ignoreCase = true) }.map { it.nameWithoutExtension }
    }

    private fun getHistoryForModuleNames(path: Path, moduleNames: Iterable<String>): Either<Set<File>> {
        val possibleModules =
            moduleNames.flatMapTo(HashSet<IncrementalModuleEntry>()) { modulesInfo.nameToModules[it] ?: emptySet() }
        val modules = possibleModules.filter { Paths.get(it.buildDir.absolutePath).isParentOf(path) }
        if (modules.isEmpty()) return Either.Error("Unknown module for $path (candidates: ${possibleModules.joinToString()})")

        val result = modules.mapTo(HashSet()) { it.buildHistoryFile }
        return Either.Success(result)
    }
}

private fun Path.isParentOf(path: Path) = path.startsWith(this)
private fun Path.isParentOf(file: File) = this.isParentOf(Paths.get(file.absolutePath))
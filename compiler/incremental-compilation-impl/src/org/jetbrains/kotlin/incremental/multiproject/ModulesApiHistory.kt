/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

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
    private val dirToHistoryFileCache = HashMap<File, File?>()

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
            val buildHistory = getBuildHistoryForDir(dir)
                    ?: return Either.Error("Could not get build history for class files: ${files.joinToString()}")
            result.add(buildHistory)
        }

        return Either.Success(result)
    }

    private fun getBuildHistoryForDir(file: File): File? =
        dirToHistoryFileCache.getOrPut(file) {
            val module = modulesInfo.dirToModule[file]
            val parent = file.parentFile

            when {
                module != null ->
                    module.buildHistoryFile
                parent != null && projectRootPath.isParentOf(parent) ->
                    getBuildHistoryForDir(parent)
                else ->
                    null
            }
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
            val buildHistory = getBuildHistoryForDir(dir)
                    ?: return Either.Error("Could not get build history for class files: ${files.joinToString()}")
            result.add(buildHistory)
        }
        return Either.Success(result)
    }
}

class ModulesApiHistoryAndroid(modulesInfo: IncrementalModuleInfo) : ModulesApiHistoryJvm(modulesInfo) {
    override fun getBuildHistoryFilesForJar(jar: File): Either<Set<File>> {
        // Module detection is expensive, so we don't don it for jars outside of project dir
        if (!projectRootPath.isParentOf(jar)) return Either.Error("Non-project jar is modified $jar")

        val jarPath = Paths.get(jar.absolutePath)
        val possibleModules = getPossibleModuleNamesFromJar(jarPath)
            .flatMapTo(HashSet()) { modulesInfo.nameToModules[it] ?: emptySet() }

        val modules = possibleModules.filter { Paths.get(it.buildDir.absolutePath).isParentOf(jarPath) }
        if (modules.isEmpty()) return Either.Error("Unknown module for $jar (candidates: ${possibleModules.joinToString()})")

        val result = modules.mapTo(HashSet()) { it.buildHistoryFile }
        return Either.Success(result)
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
}

private fun Path.isParentOf(path: Path) = path.startsWith(this)
private fun Path.isParentOf(file: File) = this.isParentOf(Paths.get(file.absolutePath))
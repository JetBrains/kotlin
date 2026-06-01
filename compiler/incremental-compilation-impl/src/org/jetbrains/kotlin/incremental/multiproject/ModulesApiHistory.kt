/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.multiproject

import org.jetbrains.kotlin.incremental.IncrementalModuleInfo
import org.jetbrains.kotlin.incremental.util.Either
import org.jetbrains.kotlin.library.KlibConstants.KLIB_MANIFEST_FILE_NAME
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

interface ModulesApiHistory {
    fun historyFilesForChangedFiles(changedFiles: Set<File>): Either<Set<File>>
}

object EmptyModulesApiHistory : ModulesApiHistory {
    override fun historyFilesForChangedFiles(changedFiles: Set<File>): Either<Set<File>> =
        Either.Error("Multi-module IC is not configured")
}

abstract class ModulesApiHistoryBase(rootProjectDir: File, protected val modulesInfo: IncrementalModuleInfo) : ModulesApiHistory {
    // All project build dirs should have this dir as their parent. For a default project setup, this will
    // be the same as root project path. Some projects map output outside of the root project dir, typically
    // with <some_dir>/<project_path>/build, and in that case, this path will be <some_dir>.
    // This is using set in order to de-dup paths, and avoid duplicate checks when possible.
    protected val possibleParentsToBuildDirs: Set<Path> = buildSet {
        modulesInfo.rootProjectBuildDir?.let { add(Paths.get(it.parentFile.absolutePath)) }
        add(Paths.get(rootProjectDir.absolutePath))
    }
    private val dirToHistoryFileCache = HashMap<File, Set<File>>()

    override fun historyFilesForChangedFiles(changedFiles: Set<File>): Either<Set<File>> {
        val result = HashSet<File>()
        val jarFiles = ArrayList<File>()
        val classFiles = ArrayList<File>()

        val manifestFiles = ArrayList<File>()

        for (file in changedFiles) {
            val extension = file.extension

            when {
                extension.equals("class", ignoreCase = true) -> {
                    classFiles.add(file)
                }
                extension.equals("jar", ignoreCase = true) -> {
                    jarFiles.add(file)
                }
                extension.equals("klib", ignoreCase = true) -> {
                    // TODO: shouldn't jars and klibs be tracked separately?
                    jarFiles.add(file)
                }
                file.name == KLIB_MANIFEST_FILE_NAME -> {
                    manifestFiles.add(file)
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
        for (dir in classFileDirs.keys) {
            when (val historyEither = getBuildHistoryForDir(dir)) {
                is Either.Success<Set<File>> -> result.addAll(historyEither.value)
                is Either.Error -> return historyEither
            }
        }

        for (manifest in manifestFiles) {
            when (val historyEither = getBuildHistoryForDir(manifest.parentFile)) {
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
                parent != null && isInProjectBuildDir(parent) -> {
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

    protected fun isInProjectBuildDir(file: File): Boolean {
        return possibleParentsToBuildDirs.any { it.isParentOf(file) }
    }

    protected abstract fun getBuildHistoryFilesForJar(jar: File): Either<Set<File>>
}

class ModulesApiHistoryJs(rootProjectDir: File, modulesInfo: IncrementalModuleInfo) : ModulesApiHistoryBase(rootProjectDir, modulesInfo) {
    override fun getBuildHistoryFilesForJar(jar: File): Either<Set<File>> {
        val moduleEntry = modulesInfo.jarToModule[jar]

        return when {
            moduleEntry != null -> Either.Success(setOf(moduleEntry.buildHistoryFile))
            else -> Either.Error("No module is found for jar $jar")
        }
    }
}

private fun Path.isParentOf(path: Path) = path.startsWith(this)
private fun Path.isParentOf(file: File) = this.isParentOf(Paths.get(file.absolutePath))

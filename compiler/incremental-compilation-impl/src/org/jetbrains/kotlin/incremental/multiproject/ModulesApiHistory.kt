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

private fun Path.isParentOf(path: Path) = path.startsWith(this)
private fun Path.isParentOf(file: File) = this.isParentOf(Paths.get(file.absolutePath))
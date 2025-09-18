/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.SERVER_CACHE_DIR
import common.SERVER_ARTIFACTS_CACHE_DIR
import common.SERVER_TMP_CACHE_DIR
import common.calculateCompilationInputHash
import common.computeSha256
import common.copyDirectoryRecursively
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import model.ArtifactType
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class CacheHandler {

    private val artifacts = mutableMapOf<String, String>()

    init {
        Files.createDirectories(SERVER_ARTIFACTS_CACHE_DIR)
        Files.createDirectories(SERVER_TMP_CACHE_DIR)
        loadCache()
    }

    fun cleanup() {
        artifacts.clear()
        SERVER_CACHE_DIR.toFile().deleteRecursively()
        Files.createDirectories(SERVER_ARTIFACTS_CACHE_DIR)
        Files.createDirectories(SERVER_TMP_CACHE_DIR)
    }

    fun loadCache() {
        artifacts.clear()

        Files.list(SERVER_ARTIFACTS_CACHE_DIR).forEach {
            val fingerprint = it.fileName.toString()
            val actualFilePath = it.toAbsolutePath().toString()
            artifacts[fingerprint] = actualFilePath
        }
    }

    fun isFileCached(fingerprint: String): Boolean {
        return artifacts.containsKey(fingerprint)
    }

    fun getFile(fingerprint: String): File {
        return File(artifacts[fingerprint])
    }

    suspend fun cacheFile(
        tmpFile: File,
        artifactTypes: Set<ArtifactType>,
        deleteOriginalFile: Boolean,
        fileLockMap: MutableMap<Path, Mutex>,
        remoteCompilerArguments: K2JVMCompilerArguments? = null,
    ): File {
        val fingerprint = computeSha256(tmpFile)
        val targetPath = SERVER_ARTIFACTS_CACHE_DIR.resolve(fingerprint)
        val mutex = fileLockMap.computeIfAbsent(targetPath) { Mutex() }

        mutex.withLock {
            when {
                tmpFile.isDirectory -> {
                    copyDirectoryRecursively(
                        source = tmpFile.toPath(),
                        target = targetPath,
                        overwrite = true
                    )
                }
                tmpFile.isFile -> {
                    Files.copy(
                        tmpFile.toPath(),
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        }

        artifacts[fingerprint] = targetPath.toAbsolutePath().toString()

        if (tmpFile.isDirectory && artifactTypes.any { it == ArtifactType.RESULT } && remoteCompilerArguments != null) {
            // the compilation result is saved in the cache two ways
            // 1. as a hash of directory content, that is because the compilation result may be used as
            // a dependency for other compilation tasks
            // 2. as a hash of compiler arguments and input files, that is because the compilation result may be used
            // as a dependency for compilation of the same source files with different compiler arguments
            val compilationInputHash = calculateCompilationInputHash(remoteCompilerArguments)
            try {
                val symlink = Files.createSymbolicLink(SERVER_ARTIFACTS_CACHE_DIR.resolve(compilationInputHash), targetPath.fileName)
                artifacts[compilationInputHash] = symlink.toAbsolutePath().toString()
            } catch (e: FileAlreadyExistsException) {
                println("Warning: the compilation result is already cached as a symbolic link to $targetPath")
            }
        }

        // TODO we want to delete temp files, but this tmpFile does not have to be necessarily in tmp directory
//        if (deleteOriginalFile) {
//            tmpFile.deleteRecursively()
//        }
        return targetPath.toFile()
    }

    fun isCompilationResultCached(
        compilerArguments: K2JVMCompilerArguments,
    ): Pair<Boolean, String> {
        val inputFingerprint = calculateCompilationInputHash(compilerArguments)
        return artifacts.containsKey(inputFingerprint) to inputFingerprint
    }

    fun getCompilationResultDirectory(inputFingerprint: String): File {
        return SERVER_ARTIFACTS_CACHE_DIR.resolve(inputFingerprint).toFile()
    }

}
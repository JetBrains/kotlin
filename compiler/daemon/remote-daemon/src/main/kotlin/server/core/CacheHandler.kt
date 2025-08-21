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
import model.ArtifactType
import model.CacheItem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class CacheHandler {

    private val artifacts = mutableMapOf<String, String>()

    init {
        Files.createDirectories(Paths.get(SERVER_ARTIFACTS_CACHE_DIR))
        Files.createDirectories(Paths.get(SERVER_ARTIFACTS_CACHE_DIR))
        loadCache()
    }

    fun cleanup() {
        artifacts.clear()
        File(SERVER_CACHE_DIR).deleteRecursively()
        Files.createDirectories(Paths.get(SERVER_ARTIFACTS_CACHE_DIR))
        Files.createDirectories(Paths.get(SERVER_TMP_CACHE_DIR))
    }

    fun loadCache() {
        artifacts.clear()

        Files.list(Paths.get(SERVER_ARTIFACTS_CACHE_DIR)).forEach {
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

    fun cacheFile(tmpFile: File, artifactType: ArtifactType, remoteCompilerArguments: Map<String, String>? = null): File {
        val fingerprint = computeSha256(tmpFile)
        println("SERVER file path CACEH is ${tmpFile} and hash is $fingerprint")

        val targetPath = Paths.get(SERVER_ARTIFACTS_CACHE_DIR, fingerprint)
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
        artifacts[fingerprint] = targetPath.toAbsolutePath().toString()

        if (tmpFile.isDirectory && artifactType == ArtifactType.RESULT && remoteCompilerArguments != null) {
            // the compilation result is saved in the cache two ways
            // 1. as a hash of directory content, that is because the compilation result may be used as
            // a dependency for other compilation tasks
            // 2. as a hash of compiler arguments and input files, that is because the compilation result may be used
            // as a dependency for compilation of the same source files with different compiler arguments
            val compilationInputHash = calculateCompilationInputHash(remoteCompilerArguments)
            val symlink = Files.createSymbolicLink(Paths.get(SERVER_ARTIFACTS_CACHE_DIR, compilationInputHash), targetPath)
            artifacts[compilationInputHash] = symlink.toAbsolutePath().toString()
        }

        tmpFile.deleteRecursively()
        return targetPath.toFile()
    }

    private fun cacheCompilationResult(
        compilerArguments: Map<String, String>
    ): CacheItem {
        val inputFingerprint = calculateCompilationInputHash(compilerArguments)
        val resultOutputDir = compilerArguments["-d"]
        val cachedCompilationResultPath = Paths.get(SERVER_ARTIFACTS_CACHE_DIR, inputFingerprint)
        copyDirectoryRecursively(
            Paths.get(resultOutputDir!!),
            cachedCompilationResultPath,
            overwrite = true
        ) // TODO double exclamation mark
        artifacts[inputFingerprint] = cachedCompilationResultPath.toAbsolutePath().toString()
        return CacheItem(inputFingerprint, cachedCompilationResultPath.toFile())
    }

    fun isCompilationResultCached(
        compilerArguments: Map<String, String>,
    ): Pair<Boolean, String> {
        val inputFingerprint = calculateCompilationInputHash(compilerArguments)
        return artifacts.containsKey(inputFingerprint) to inputFingerprint
    }

    fun getCompilationResultDirectory(inputFingerprint: String): File {
        return File("${SERVER_ARTIFACTS_CACHE_DIR}/$inputFingerprint")
    }

}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.CompilerUtils
import common.FileChunkingStrategy
import common.SERVER_CACHE_DIR
import common.SERVER_COMPILATION_RESULT_CACHE_DIR
import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.calculateCompilationInputHash
import common.computeSha256
import common.copyDirectoryRecursively
import model.CacheItem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class CacheHandler(
    private val fileChunkingStrategy: FileChunkingStrategy
) {

    private val sourceFiles = mutableMapOf<String, String>()
    private val compilationResults = mutableMapOf<String, String>()

    init {
        Files.createDirectories(Paths.get(SERVER_SOURCE_FILES_CACHE_DIR))
        Files.createDirectories(Paths.get(SERVER_COMPILATION_RESULT_CACHE_DIR))
        loadCache()
    }

    fun cleanup() {
        sourceFiles.clear()
        compilationResults.clear()
        File(SERVER_CACHE_DIR).deleteRecursively()
        Files.createDirectories(Paths.get(SERVER_SOURCE_FILES_CACHE_DIR))
        Files.createDirectories(Paths.get(SERVER_COMPILATION_RESULT_CACHE_DIR))
    }

    fun loadCache() {
        sourceFiles.clear()
        compilationResults.clear()

        Files.list(Paths.get(SERVER_SOURCE_FILES_CACHE_DIR)).forEach {
            val fingerprint = it.fileName.toString()
            val actualFilePath = it.toAbsolutePath().toString()
            sourceFiles[fingerprint] = actualFilePath
        }

        Files.list(Paths.get(SERVER_COMPILATION_RESULT_CACHE_DIR)).forEach {
            val fingerprint = it.fileName.toString()
            val actualFilePath = it.toAbsolutePath().toString()
            compilationResults[fingerprint] = actualFilePath
        }
    }

    fun isFileCached(clientFilePath: String, fingerprint: String): Boolean {
        return sourceFiles.containsKey(fingerprint)
    }

    fun getFile(clientFilePath: String, fingerprint: String): File {
        return File(sourceFiles[fingerprint])
    }

    fun cacheSourceFile(fileChunks: List<ByteArray>): CacheItem {
        // TODO here we computing hash again, consider using that one that we received from client
        val fingerprint = computeSha256(fileChunks)
        Files.createDirectories(Paths.get(SERVER_SOURCE_FILES_CACHE_DIR))
        val file = fileChunkingStrategy.reconstruct(fileChunks, "${SERVER_SOURCE_FILES_CACHE_DIR}/$fingerprint")
        sourceFiles[fingerprint] = file.absolutePath
        return CacheItem(fingerprint, file)
    }

    fun cacheCompilationResult(
        compilerArguments: Map<String, String>
    ): CacheItem {
        val inputFingerprint = calculateCompilationInputHash(compilerArguments)

        val resultOutputDir = compilerArguments["-d"]
        val cachedCompilationResultPath = Paths.get(SERVER_COMPILATION_RESULT_CACHE_DIR, inputFingerprint)
        copyDirectoryRecursively(
            Paths.get(resultOutputDir!!),
            cachedCompilationResultPath,
            overwrite = true
        ) // TODO double exclamation mark
        compilationResults[inputFingerprint] = cachedCompilationResultPath.toAbsolutePath().toString()
        return CacheItem(inputFingerprint, cachedCompilationResultPath.toFile())
    }

    fun cacheCompilationResult(
        inputFingerprint: String,
        compilerArguments: Map<String, String>,
    ): CacheItem {
        val cachedCompilationResultPath = Paths.get(SERVER_COMPILATION_RESULT_CACHE_DIR, inputFingerprint)
        copyDirectoryRecursively(CompilerUtils.getOutputDir(compilerArguments).toPath(), cachedCompilationResultPath, overwrite = true)
        compilationResults[inputFingerprint] = cachedCompilationResultPath.toAbsolutePath().toString()
        return CacheItem(inputFingerprint, cachedCompilationResultPath.toFile())
    }

    fun isCompilationResultCached(
        compilerArguments: Map<String, String>,
    ): Pair<Boolean, String> {
        val inputFingerprint = calculateCompilationInputHash(compilerArguments)
        return compilationResults.containsKey(inputFingerprint) to inputFingerprint
    }

    fun getCompilationResultDirectory(inputFingerprint: String): File {
        return File("${SERVER_COMPILATION_RESULT_CACHE_DIR}/$inputFingerprint")
    }

}
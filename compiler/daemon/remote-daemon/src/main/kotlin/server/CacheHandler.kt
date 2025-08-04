/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import common.SERVER_SOURCE_FILES_CACHE_DIR
import common.FileChunkingStrategy
import common.SERVER_COMPILATION_RESULT_DIR
import common.computeSha256
import common.copyDirectoryRecursively
import model.CacheItem
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

class CacheHandler(
    private val fileChunkingStrategy: FileChunkingStrategy
) {

    private val sourceFilePaths = mutableMapOf<String, String>()
    private val compilationResults = mutableMapOf<String, String>()

    init {
        Files.createDirectories(Paths.get(SERVER_SOURCE_FILES_CACHE_DIR))
        Files.createDirectories(Paths.get(SERVER_COMPILATION_RESULT_DIR))
    }

    fun loadSourceFilesCache() {
        sourceFilePaths.clear()
        Files.list(Paths.get(SERVER_SOURCE_FILES_CACHE_DIR)).forEach {
            val fingerprint = it.fileName.toString()
            val actualFilePath = it.toAbsolutePath().toString()
            sourceFilePaths[fingerprint] = actualFilePath
        }
    }

    fun addSourceFile(fileChunks: List<ByteArray>): CacheItem {
        // TODO here we computing hash again, consider using that one that we received from client
        val fingerprint = computeSha256(fileChunks)
        val file = fileChunkingStrategy.reconstruct(fileChunks, "$SERVER_SOURCE_FILES_CACHE_DIR/$fingerprint")
        sourceFilePaths[fingerprint] = file.absolutePath
        return CacheItem(fingerprint, file)
    }

    fun getSourceFile(fingerprint: String): File? {
        return sourceFilePaths[fingerprint]?.let {
            File(it)
        }
    }

    fun addCompilationResultIfAbsent(
        compilationResultDirectory: File,
        compilerArguments: List<String>,
        compilerVersion: String
    ): CacheItem {
        val inputFingerprint = calculateCompilationInputHash(compilationResultDirectory.listFiles()?.toList() ?: emptyList(), compilerArguments, compilerVersion)
        val cachedCompilationResultPath = Paths.get(SERVER_COMPILATION_RESULT_DIR, inputFingerprint)
        if (!compilationResults.containsKey(inputFingerprint)) {
            copyDirectoryRecursively(compilationResultDirectory.toPath(), cachedCompilationResultPath)
            compilationResults[inputFingerprint] = cachedCompilationResultPath.toAbsolutePath().toString()
        }
        return CacheItem(inputFingerprint, cachedCompilationResultPath.toFile())
    }

    fun isCompilationResultCached(fingerprint: String): Boolean {
        return compilationResults.containsKey(fingerprint)
    }

    fun getCompilationResultDirectory(inputFingerprint: String): File {
        return File("$SERVER_COMPILATION_RESULT_DIR/$inputFingerprint")
    }

    private fun isSourceFileCached(fingerprint: String): Boolean = sourceFilePaths.containsKey(fingerprint)


    private fun calculateCompilationInputHash(sourceFiles: List<File>, compilerArguments: List<String>, compilerVersion: String = "2.0"): String{
        val digest = MessageDigest.getInstance("SHA-256")
        sourceFiles.sortedBy { it.path }.forEach { file->
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
        }
        compilerArguments.sorted().forEach { arg->
            digest.update(arg.toByteArray())
        }
        digest.update(compilerVersion.toByteArray())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
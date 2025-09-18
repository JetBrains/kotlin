/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.ARTIFACTS_FOLDER_NAME
import common.SERVER_CACHE_DIR
import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.copyDirectoryRecursively
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.absolute
import kotlin.io.path.name

class WorkspaceManager(
    val userId: String,
    val projectName: String,
    val clientOutputDir: Path,
) {

    val userProjectWorkspaceDir: Path =
        SERVER_COMPILATION_WORKSPACE_DIR
            .resolve(userId)
            .resolve(projectName)

    init {
        Files.createDirectories(SERVER_COMPILATION_WORKSPACE_DIR)
    }

    companion object {
        fun cleanup() {
            SERVER_COMPILATION_WORKSPACE_DIR.toFile().deleteRecursively()
            Files.createDirectories(SERVER_COMPILATION_WORKSPACE_DIR)
        }
    }

    fun prependWorkspaceProjectDirectory(path: Path): Path {
        val relativePath = if (path.isAbsolute) {
            Paths.get(path.toString().removePrefix("/"))
        } else {
            path
        }
        return userProjectWorkspaceDir.resolve(relativePath).absolute()
    }

    private fun removeWorkspaceProjectPrefix(path: Path): Path {
        val base = userProjectWorkspaceDir.toAbsolutePath().normalize()
        val normalizedInput = path.normalize()
        val relativePath =
            if (normalizedInput.isAbsolute) {
                if (normalizedInput.startsWith(base)) base.relativize(normalizedInput)
                else normalizedInput.fileName
            } else {
                normalizedInput
            }
        return Paths.get("/").resolve(relativePath)
    }

    private fun removeCachePrefix(remotePath: Path): Path {
        val fingerprintFilenameIndex = remotePath.indexOfFirst { it.name == ARTIFACTS_FOLDER_NAME } + 1
        val cleanedPath = remotePath.subpath(fingerprintFilenameIndex + 1, remotePath.nameCount)
        val resolved = clientOutputDir.resolve(cleanedPath)
        return resolved
    }

    fun getClientPathFromRemote(remotePath: Path): Path {
        return if (remotePath.toString().contains(SERVER_CACHE_DIR.toString())) {
            removeCachePrefix(remotePath)
        } else {
            removeWorkspaceProjectPrefix(remotePath)
        }
    }

    fun getOutputDir(clientOutputPath: Path): Path {
        return prependWorkspaceProjectDirectory(clientOutputPath)
    }

    // in scenario where .jar file is part of classpath and also part compiler plugins
    // we can end up with FileAlreadyExist exception because multiple coroutines attempt to copy the
    // same file and copying file is not atomic operation
    suspend fun copyFileToProject(
        cachedFilePath: String,
        clientFilePath: String,
        fileLockMap: MutableMap<Path, Mutex>
    ): File {
        val targetPath = prependWorkspaceProjectDirectory(Paths.get(clientFilePath))
        val mutex = fileLockMap.computeIfAbsent(targetPath) { Mutex() }

        return mutex.withLock {
            Files.createDirectories(targetPath.parent)

            if (File(cachedFilePath).isDirectory) {
                copyDirectoryRecursively(Paths.get(cachedFilePath), targetPath, overwrite = true)
            } else {
                Files.copy(Paths.get(cachedFilePath), targetPath, StandardCopyOption.REPLACE_EXISTING).toFile()
            }
        }
    }
}
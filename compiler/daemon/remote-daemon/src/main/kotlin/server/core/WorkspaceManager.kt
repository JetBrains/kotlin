/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.SERVER_COMPILATION_WORKSPACE_DIR
import common.copyDirectoryRecursively
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class WorkspaceManager {


    init {
        Files.createDirectories(Paths.get(SERVER_COMPILATION_WORKSPACE_DIR))
    }

    fun getOutputDir(userId: String, projectName: String): Path {
        return Paths.get(SERVER_COMPILATION_WORKSPACE_DIR, userId, projectName, "output")
    }

    fun copyFileToProject(cachedFilePath: String, clientFilePath: String, userId: String, projectName: String): File {
        val projectDir = getUserProjectFolder(userId, projectName)
        val targetPath = Paths.get(projectDir, clientFilePath)
        Files.createDirectories(targetPath.parent)

        println("Copying $cachedFilePath to $targetPath")
        return if (File(cachedFilePath).isDirectory) {
            copyDirectoryRecursively(Paths.get(cachedFilePath), targetPath)
        } else {
            // TODO: .copy is not atomic operation, in case the same user will try to compile this file multiple times
            Files.copy(Paths.get(cachedFilePath), targetPath, StandardCopyOption.REPLACE_EXISTING).toFile()
        }
    }

    fun getUserProjectFolder(userId: String, projectName: String) =
        "$SERVER_COMPILATION_WORKSPACE_DIR/$userId/$projectName"

    fun getUserProjectSourceFolder(userId: String, projectName: String) =
        "$SERVER_COMPILATION_WORKSPACE_DIR/$userId/$projectName/output"

    fun cleanup() {
        File(SERVER_COMPILATION_WORKSPACE_DIR).deleteRecursively()
    }
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import common.SERVER_COMPILATION_WORKSPACE_DIR
import java.nio.file.CopyOption
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class WorkspaceManager {

    fun copyFileToProject(cachedFilePath: String, clientFilePath: String, userId: String, projectName: String): Path?{
        try {
            val projectDir = Paths.get(SERVER_COMPILATION_WORKSPACE_DIR, userId, projectName)
            val cp = Paths.get(cachedFilePath)
            val tp = Paths.get(projectDir.toString(), clientFilePath).toAbsolutePath()
            Files.createDirectories(tp.parent)
            return Files.copy(cp, tp, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            // TODO error handling
            println("Error while copying file to project: ${e.message}")
            e.printStackTrace()
        }
        return null
    }

}
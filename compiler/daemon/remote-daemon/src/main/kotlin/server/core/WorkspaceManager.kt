/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server.core

import common.SERVER_COMPILATION_WORKSPACE_DIR
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class WorkspaceManager {

    fun getOutputDir(userId: String, projectName: String): Path {
        return Paths.get(SERVER_COMPILATION_WORKSPACE_DIR, userId, projectName, "output")
    }

    fun copyFileToProject(cachedFilePath: String, clientFilePath: String, userId: String, projectName: String): Path {
        val projectDir = Paths.get(SERVER_COMPILATION_WORKSPACE_DIR, userId, projectName)
        println("Copying file to project: $projectDir")
        val cp = Paths.get(cachedFilePath)
        println("cp is $cp")
        val tp = Paths.get(projectDir.toString(), clientFilePath)
        println("Copying file from $cp to $tp")
        Files.createDirectories(tp.parent)
        return Files.copy(cp, tp, StandardCopyOption.REPLACE_EXISTING)
    }

}
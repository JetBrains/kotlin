/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import org.jetbrains.kotlin.incremental.IncrementalModuleEntry
import org.jetbrains.kotlin.server.IncrementalModuleEntryProto
import java.io.File

// TODO fix the project path
fun IncrementalModuleEntry.toProto(): IncrementalModuleEntryProto {
    return IncrementalModuleEntryProto.newBuilder()
        .setProjectPath("todo_project_path")
        .setName(name)
        .setBuildDir(buildDir.absolutePath)
        .setBuildHistoryFile(buildHistoryFile.absolutePath)
        .setAbiSnapshot(abiSnapshot.absolutePath)
        .build()
}

fun IncrementalModuleEntryProto.toDomain(): IncrementalModuleEntry {
    return IncrementalModuleEntry(
        projectPath,
        name,
        File(buildDir),
        File(buildHistoryFile),
        File(abiSnapshot)
    )
}
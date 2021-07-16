/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import java.io.File
import java.io.Serializable

data class IncrementalModuleEntry(
    private val projectPath: String,
    val name: String,
    val buildDir: File,
    val buildHistoryFile: File,
    val abiSnapshot: File
) : Serializable {
    companion object {
        private const val serialVersionUID = 0L
    }
}

class IncrementalModuleInfo(
    val projectRoot: File,
    val rootProjectBuildDir: File,
    val dirToModule: Map<File, IncrementalModuleEntry>,
    val nameToModules: Map<String, Set<IncrementalModuleEntry>>,
    val jarToClassListFile: Map<File, File>,
    // only for js and mpp
    val jarToModule: Map<File, IncrementalModuleEntry>,
    //for JVM only
    val jarToAbiSnapshot: Map<File, File>
) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

private fun mergeNameToModules(
    main: Map<String, Set<IncrementalModuleEntry>>,
    child: Map<String, Set<IncrementalModuleEntry>>
): Map<String, Set<IncrementalModuleEntry>> {
    val merged = mutableMapOf<String, Set<IncrementalModuleEntry>>()
    merged.putAll(main)

    child.forEach { key, value ->
        merged.put(key, mutableSetOf<IncrementalModuleEntry>().apply {
            addAll(merged.get(key) ?: mutableSetOf())
            addAll(value)
        })
    }

    return merged
}

fun mergeIncrementalModuleInfo(
    mainModuleInfo: IncrementalModuleInfo,
    includedBuildModuleInfo: IncrementalModuleInfo
): IncrementalModuleInfo {
    return IncrementalModuleInfo(
        projectRoot = mainModuleInfo.projectRoot,
        rootProjectBuildDir = mainModuleInfo.rootProjectBuildDir,
        dirToModule = mainModuleInfo.dirToModule.toMutableMap().apply { putAll(includedBuildModuleInfo.dirToModule) },
        nameToModules = mergeNameToModules(mainModuleInfo.nameToModules, includedBuildModuleInfo.nameToModules),
        jarToClassListFile = mainModuleInfo.jarToClassListFile.toMutableMap().apply { putAll(includedBuildModuleInfo.jarToClassListFile) },
        jarToModule = mainModuleInfo.jarToModule.toMutableMap().apply { putAll(includedBuildModuleInfo.jarToModule) },
        jarToAbiSnapshot = mainModuleInfo.jarToAbiSnapshot.toMutableMap().apply { putAll(includedBuildModuleInfo.jarToAbiSnapshot) }
    )
}
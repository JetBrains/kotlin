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
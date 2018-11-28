/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.io.Serializable

data class IncrementalModuleEntry(
    private val projectPath: String,
    val name: String,
    val buildDir: File,
    val buildHistoryFile: File
) : Serializable {
    companion object {
        private const val serialVersionUID = 0L
    }
}

class IncrementalModuleInfo(
    val projectRoot: File,
    val dirToModule: Map<File, IncrementalModuleEntry>,
    val nameToModules: Map<String, Set<IncrementalModuleEntry>>,
    val jarToClassListFile: Map<File, File>,
    // only for js
    val jarToModule: Map<File, IncrementalModuleEntry>
) : Serializable {
    companion object {
        private const val serialVersionUID = 0L
    }
}
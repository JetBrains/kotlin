/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.common

import java.io.File
import java.io.Serializable

open class CompilationOptions(
        val compilerMode: CompilerMode,
        val targetPlatform: CompileService.TargetPlatform,
        val reportCategories: Array<Int>,
        val reportSeverity: Int,
        val requestedCompilationResults: Array<Int>
) : Serializable {
    companion object {
        const val serialVersionUID: Long = 0
    }
}

class IncrementalCompilationOptions(
        val areFileChangesKnown: Boolean,
        val modifiedFiles: List<File>?,
        val deletedFiles: List<File>?,
        val workingDir: File,
        val customCacheVersionFileName: String,
        val customCacheVersion: Int,
        compilerMode: CompilerMode,
        targetPlatform: CompileService.TargetPlatform,
        reportCategories: Array<Int>,
        reportSeverity: Int,
        requestedCompilationResults: Array<Int>
) : CompilationOptions(compilerMode, targetPlatform, reportCategories, reportSeverity, requestedCompilationResults) {
    companion object {
        const val serialVersionUID: Long = 0
    }
}

enum class CompilerMode : Serializable {
    NON_INCREMENTAL_COMPILER,
    INCREMENTAL_COMPILER,
    JPS_COMPILER
}
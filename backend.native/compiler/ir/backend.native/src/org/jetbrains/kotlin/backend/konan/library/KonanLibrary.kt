/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan.library

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.file.File

// This scheme describes the Konan Library (klib) layout.
interface KonanLibraryLayout {
    val libDir: File
    val target: KonanTarget?
        // This is a default implementation. Can't make it an assignment.
        get() = null 
    val manifestFile 
        get() = File(libDir, "manifest")
    val resourcesDir 
        get() = File(libDir, "resources")
    val targetsDir
        get() = File(libDir, "targets")
    val targetDir 
        get() = File(targetsDir, target!!.name.toLowerCase())
    val kotlinDir 
        get() = File(targetDir, "kotlin")
    val nativeDir 
        get() = File(targetDir, "native")
    val linkdataDir 
        get() = File(libDir, "linkdata")
    val moduleHeaderFile 
        get() = File(linkdataDir, "module")
    val escapeAnalysisFile
        get() = File(linkdataDir, "module_escape_analysis")
    fun packageFile(packageName: String)
        = File(linkdataDir, if (packageName == "") "root_package" else "package_$packageName")
}

interface KonanLibrary: KonanLibraryLayout {
    val libraryName: String
}


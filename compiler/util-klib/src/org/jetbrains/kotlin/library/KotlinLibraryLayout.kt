/**
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.kotlin.library

import org.jetbrains.kotlin.konan.file.File

const val KLIB_MANIFEST_FILE_NAME = "manifest"
const val KLIB_RESOURCES_FOLDER_NAME = "resources"

/**
 * This scheme describes the Kotlin/Native Library (KLIB) layout.
 */
interface KotlinLibraryLayout {
    val libFile: File
    val libraryName: String
        get() = libFile.path
    val component: String?
    val componentDir: File
        get() = File(libFile, component!!)
    val manifestFile
        get() = File(componentDir, KLIB_MANIFEST_FILE_NAME)
    val resourcesDir
        get() = File(componentDir, KLIB_RESOURCES_FOLDER_NAME)
    val pre_1_4_manifest: File
        get() = File(libFile, KLIB_MANIFEST_FILE_NAME)
}

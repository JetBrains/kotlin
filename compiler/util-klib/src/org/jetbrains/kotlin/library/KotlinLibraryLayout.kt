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
const val KLIB_MODULE_METADATA_FILE_NAME = "module"
const val KLIB_METADATA_FOLDER_NAME = "linkdata"
const val KLIB_IR_FOLDER_NAME = "ir"

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
        get() = File(componentDir, "resources")
    val pre_1_4_manifest: File
        get() = File(libFile, KLIB_MANIFEST_FILE_NAME)
}

interface MetadataKotlinLibraryLayout : KotlinLibraryLayout {
    val metadataDir
        get() = File(componentDir, KLIB_METADATA_FOLDER_NAME)
    val moduleHeaderFile
        get() = File(metadataDir, KLIB_MODULE_METADATA_FILE_NAME)

    fun packageFragmentsDir(packageName: String) =
        File(metadataDir, if (packageName == "") "root_package" else "package_$packageName")

    fun packageFragmentFile(packageFqName: String, partName: String) =
        File(packageFragmentsDir(packageFqName), "$partName$KLIB_METADATA_FILE_EXTENSION_WITH_DOT")
}

interface IrKotlinLibraryLayout : KotlinLibraryLayout {
    val irDir
        get() = File(componentDir, KLIB_IR_FOLDER_NAME)
    val irDeclarations
        get() = File(irDir, IR_DECLARATIONS_FILE_NAME)
    val irTypes
        get() = File(irDir, IR_TYPES_FILE_NAME)
    val irSignatures
        get() = File(irDir, IR_SIGNATURES_FILE_NAME)
    val irStrings
        get() = File(irDir, IR_STRINGS_FILE_NAME)
    val irBodies
        get() = File(irDir, IR_BODIES_FILE_NAME)
    val irFiles
        get() = File(irDir, IR_FILES_FILE_NAME)
    val irDebugInfo
        get() = File(irDir, IR_DEBUG_INFO_FILE_NAME)

    fun irDeclarations(file: File): File = File(file, IR_DECLARATIONS_FILE_NAME)
    fun irTypes(file: File): File = File(file, IR_TYPES_FILE_NAME)
    fun irSignatures(file: File): File = File(file, IR_SIGNATURES_FILE_NAME)
    fun irStrings(file: File): File = File(file, IR_STRINGS_FILE_NAME)
    fun irBodies(file: File): File = File(file, IR_BODIES_FILE_NAME)
    fun irFile(file: File): File = File(file, IR_FILES_FILE_NAME)
    fun irDebugInfo(file: File): File = File(file, IR_DEBUG_INFO_FILE_NAME)

    companion object {
        const val IR_DECLARATIONS_FILE_NAME = "irDeclarations.knd"
        const val IR_TYPES_FILE_NAME = "types.knt"
        const val IR_SIGNATURES_FILE_NAME = "signatures.knt"
        const val IR_STRINGS_FILE_NAME = "strings.knt"
        const val IR_BODIES_FILE_NAME = "bodies.knb"
        const val IR_FILES_FILE_NAME = "files.knf"
        const val IR_DEBUG_INFO_FILE_NAME = "debugInfo.knd"
    }
}

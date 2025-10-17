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
const val KLIB_IR_INLINABLE_FUNCTIONS_DIR_NAME = "ir_inlinable_functions"
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
    val mainIr: IrDirectory
        get() = IrDirectory(File(componentDir, KLIB_IR_FOLDER_NAME))

    // This directory is similar to the main "ir" directory but contains only specially prepared copies of public inline functions,
    // instead of the entire IR.
    // Those may be read and inlined on the first stage of compilation, without the need to read the main, much bigger,
    // "ir" directory (see KT-75794).
    val inlineableFunsIr: IrDirectory
        get() = IrDirectory(File(componentDir, KLIB_IR_INLINABLE_FUNCTIONS_DIR_NAME))

    class IrDirectory(val dir: File) {
        val irDeclarations
            get() = File(dir, IR_DECLARATIONS_FILE_NAME)
        val irTypes
            get() = File(dir, IR_TYPES_FILE_NAME)
        val irSignatures
            get() = File(dir, IR_SIGNATURES_FILE_NAME)
        val irStrings
            get() = File(dir, IR_STRINGS_FILE_NAME)
        val irBodies
            get() = File(dir, IR_BODIES_FILE_NAME)
        val irFiles
            get() = File(dir, IR_FILES_FILE_NAME)
        val irDebugInfo
            get() = File(dir, IR_DEBUG_INFO_FILE_NAME)
        // Please check `hasFileEntriesTable` before getter invocation, otherwise it may crash in override getter
        val irFileEntries
            get() = File(dir, IR_FILE_ENTRIES_FILE_NAME)
    }

    companion object {
        const val IR_DECLARATIONS_FILE_NAME = "irDeclarations.knd"
        const val IR_TYPES_FILE_NAME = "types.knt"
        const val IR_SIGNATURES_FILE_NAME = "signatures.knt"
        const val IR_STRINGS_FILE_NAME = "strings.knt"
        const val IR_BODIES_FILE_NAME = "bodies.knb"
        const val IR_FILES_FILE_NAME = "files.knf"
        const val IR_DEBUG_INFO_FILE_NAME = "debugInfo.knd"
        const val IR_FILE_ENTRIES_FILE_NAME = "fileEntries.knf"
    }
}

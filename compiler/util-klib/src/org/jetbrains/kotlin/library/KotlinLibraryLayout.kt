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
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_BODIES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_DEBUG_INFO_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_DECLARATIONS_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FILES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FILE_ENTRIES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_SIGNATURES_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_STRINGS_FILE_NAME
import org.jetbrains.kotlin.library.components.KlibIrConstants.KLIB_IR_TYPES_FILE_NAME

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

interface IrKotlinLibraryLayout : KotlinLibraryLayout {
    val mainIr: IrDirectory
        get() = IrDirectory(File(componentDir, KLIB_IR_FOLDER_NAME))

    // This directory is similar to the main "ir" directory but contains only specially prepared copies of public inline functions,
    // instead of the entire IR.
    // Those may be read and inlined on the first stage of compilation, without the need to read the main, much bigger,
    // "ir" directory (see KT-75794).
    val inlineableFunsIr: IrDirectory
        get() = IrDirectory(File(componentDir, KLIB_IR_INLINABLE_FUNCTIONS_FOLDER_NAME))

    class IrDirectory(val dir: File) {
        val irDeclarations
            get() = File(dir, KLIB_IR_DECLARATIONS_FILE_NAME)
        val irTypes
            get() = File(dir, KLIB_IR_TYPES_FILE_NAME)
        val irSignatures
            get() = File(dir, KLIB_IR_SIGNATURES_FILE_NAME)
        val irStrings
            get() = File(dir, KLIB_IR_STRINGS_FILE_NAME)
        val irBodies
            get() = File(dir, KLIB_IR_BODIES_FILE_NAME)
        val irFiles
            get() = File(dir, KLIB_IR_FILES_FILE_NAME)
        val irDebugInfo
            get() = File(dir, KLIB_IR_DEBUG_INFO_FILE_NAME)
        // Please check `hasFileEntriesTable` before getter invocation, otherwise it may crash in override getter
        val irFileEntries
            get() = File(dir, KLIB_IR_FILE_ENTRIES_FILE_NAME)
    }
}

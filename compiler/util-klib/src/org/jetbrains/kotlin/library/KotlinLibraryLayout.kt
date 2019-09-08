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
import org.jetbrains.kotlin.library.KLIB_METADATA_FILE_EXTENSION_WITH_DOT

/**
 * This scheme describes the Kotlin/Native Library (KLIB) layout.
 */
interface KotlinLibraryLayout {
    val libDir: File
    val libraryName: String
        get() = libDir.path
    val manifestFile
        get() = File(libDir, "manifest")
    val resourcesDir
        get() = File(libDir, "resources")
}

interface MetadataKotlinLibraryLayout : KotlinLibraryLayout {
    val metadataDir
        get() = File(libDir, "linkdata")
    val moduleHeaderFile
        get() = File(metadataDir, "module")

    fun packageFragmentsDir(packageName: String) =
        File(metadataDir, if (packageName == "") "root_package" else "package_$packageName")

    fun packageFragmentFile(packageFqName: String, partName: String) =
        File(packageFragmentsDir(packageFqName), "$partName$KLIB_METADATA_FILE_EXTENSION_WITH_DOT")
}

interface IrKotlinLibraryLayout : KotlinLibraryLayout {
    val irDir
        get() = File(libDir, "ir")
    val irDeclarations
        get() = File(irDir, "irDeclarations.knd")
    val irSymbols
        get() = File(irDir, "symbols.knt")
    val irTypes
        get() = File(irDir, "types.knt")
    val irStrings
        get() = File(irDir, "strings.knt")
    val irBodies
        get() = File(irDir, "bodies.knb")
    val irFiles
        get() = File(irDir, "files.knf")
    val dataFlowGraphFile
        get() = File(irDir, "module_data_flow_graph")

    fun irDeclarations(file: File): File = File(file, "irCombined.knd")
    fun irSymbols(file: File): File = File(file, "symbols.knt")
    fun irTypes(file: File): File = File(file, "types.knt")
    fun irStrings(file: File): File = File(file, "strings.knt")
    fun irBodies(file: File): File = File(file, "body.knb")
    fun irFile(file: File): File = File(file, "file.knf")
}

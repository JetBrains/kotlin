/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.wasm

import org.jetbrains.kotlin.ir.backend.js.fileMetadata
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.library.SerializedIrFile
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

abstract class WasmKlibExport(val containingFile: String) {
    abstract val fqName: String
    abstract fun render(): String
}

class WasmKlibExportingDeclaration(
    val exportingName: String,
    actualName: String,
    containingFile: String,
    packageFqName: String,
    val declaration: IrDeclaration?,
    val exportKind: ExportKind,
) : WasmKlibExport(containingFile) {
    constructor(exportingName: String, actualName: String, file: SerializedIrFile, exportKind: ExportKind) :
            this(exportingName, actualName, file.path, file.fqName, null, exportKind)

    constructor(exportingName: String, file: IrFile, decl: IrDeclarationWithName, exportKind: ExportKind) :
            this(exportingName, decl.name.identifier, file.fileEntry.name, file.packageFqName.toString(), decl, exportKind)

    val containingPackageFqName = packageFqName.takeIf { it != "<root>" } ?: ""
    override val fqName =
        "$containingPackageFqName${".".takeIf { containingPackageFqName.isNotEmpty() } ?: ""}$actualName"

    override fun render() = "'$fqName' from file '$containingFile'"

    companion object {
        fun collectDeclarations(
            cleanFiles: List<SerializedIrFile>,
            exportedNames: ExportNamesMap,
        ) = buildList {
            for (serializedFile in cleanFiles) {
                val fileMetadata = WasmIrFileMetadata.fromByteArray(serializedFile.fileMetadata)
                for ((exportKind, exportedNames) in fileMetadata.exportNames) {
                    for ((exportedName, actualName) in exportedNames) {
                        add(WasmKlibExportingDeclaration(exportedName, actualName, serializedFile, exportKind))
                    }
                }
            }

            for ((exportKind, exportNamesFileMap) in exportedNames) {
                for ((dirtyFile, exportedDeclarations) in exportNamesFileMap) {
                    for ((declaration, exportedName) in exportedDeclarations) {
                        add(WasmKlibExportingDeclaration(exportedName, dirtyFile, declaration, exportKind))
                    }
                }
            }
        }
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers

import org.jetbrains.kotlin.ir.backend.js.fileMetadata
import org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir.JsIrFileMetadata
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.library.SerializedIrFile

abstract class JsKlibExport(val containingFile: String) {
    abstract val fqName: String
    abstract fun render(): String
}

class JsKlibExportingPackage(containingFile: String, override val fqName: String) : JsKlibExport(containingFile) {
    override fun render() = "package '$fqName' from file '$containingFile'"
}

class JsKlibExportingDeclaration(
    val exportingName: String,
    containingFile: String,
    packageFqName: String,
    val declaration: IrDeclaration?,
) : JsKlibExport(containingFile) {
    constructor(name: String, file: SerializedIrFile) : this(name, file.path, file.fqName, null)
    constructor(name: String, file: IrFile, decl: IrDeclaration) : this(name, file.fileEntry.name, file.packageFqName.toString(), decl)

    val containingPackageFqName = packageFqName.takeIf { it != "<root>" } ?: ""
    override val fqName = "$containingPackageFqName${".".takeIf { containingPackageFqName.isNotEmpty() } ?: ""}$exportingName"
    override fun render() = "exporting name '$exportingName' from file '$containingFile'"

    companion object {
        fun collectDeclarations(
            cleanFiles: List<SerializedIrFile>,
            dirtyFiles: List<IrFile>,
            exportedNames: Map<IrFile, Map<IrDeclarationWithName, String>>,
        ) = buildList {
            for (serializedFile in cleanFiles) {
                val fileMetadata = JsIrFileMetadata.fromByteArray(serializedFile.fileMetadata)
                for (exportedName in fileMetadata.exportedNames) {
                    add(JsKlibExportingDeclaration(exportedName, serializedFile))
                }
            }
            for (dirtyFile in dirtyFiles) {
                val exportedDeclarations = exportedNames[dirtyFile] ?: continue
                for ((declaration, exportedName) in exportedDeclarations) {
                    add(JsKlibExportingDeclaration(exportedName, dirtyFile, declaration))
                }
            }
        }
    }
}

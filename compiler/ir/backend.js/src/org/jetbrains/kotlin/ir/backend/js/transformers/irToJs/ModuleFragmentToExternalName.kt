/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.declarations.path

private const val EXPORTER_FILE_POSTFIX = ".export"

class ModuleFragmentToExternalName(private val jsOutputNamesMapping: Map<IrModuleFragment, String>) {
    private val externalNameToItsFile = hashMapOf<String, IrFile>()

    fun getExternalNameFor(file: IrFile, granularity: JsGenerationGranularity): String {
        assert(granularity == JsGenerationGranularity.PER_FILE) { "This method should be used only for PER_FILE granularity" }
        return file.module.getJsOutputName().getExternalModuleNameForPerFile(file).also {
            val alreadyReservedBy = externalNameToItsFile.putIfAbsent(it.lowercase(), file)

            if (alreadyReservedBy != null && alreadyReservedBy != file) {
                error(
                    """
                      |There are two files in module '${file.module.name}' that have the similar package and file names.
                      |  - Package "${file.packageFqName.asString()}" and path "${file.path}"
                      |  - Package "${alreadyReservedBy.packageFqName.asString()}" and path "${alreadyReservedBy.path}"
                      |Note, that if the difference is only in letter cases, it also could lead to a clash of the compiled artifacts
                   """.trimMargin()
                )
            }
        }
    }

    fun getExternalNameForExporterFile(file: IrFile, granularity: JsGenerationGranularity): String {
        return "${getExternalNameFor(file, granularity)}$EXPORTER_FILE_POSTFIX"
    }

    fun getSafeNameFor(file: IrFile): String {
        return "${file.module.safeName}/${file.stableFileName}"
    }

    fun getSafeNameExporterFor(file: IrFile): String {
        return "${getSafeNameFor(file)}$EXPORTER_FILE_POSTFIX"
    }

    fun getExternalNameFor(module: IrModuleFragment): String {
        return module.getJsOutputName()
    }

    private fun IrModuleFragment.getJsOutputName(): String {
        return jsOutputNamesMapping[this] ?: sanitizeName(safeName)
    }

    private fun String.getExternalModuleNameForPerFile(file: IrFile) = "$this/${file.stableFileName}"

    private val IrFile.stableFileName: String
        get() {
            val prefix = packageFqName.asString().replace('.', '/')
            val fileName = getJsName() ?: name.substringBefore(".kt")
            return "$prefix${if (prefix.isNotEmpty()) "/" else ""}$fileName"
        }
}
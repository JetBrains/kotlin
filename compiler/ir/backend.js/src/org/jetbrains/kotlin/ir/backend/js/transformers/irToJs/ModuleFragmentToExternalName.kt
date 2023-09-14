/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.utils.getJsFileName
import org.jetbrains.kotlin.ir.backend.js.utils.nameWithoutExtension
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

private const val EXPORTER_FILE_POSTFIX = ".export"

class ModuleFragmentToExternalName(private val jsOutputNamesMapping: Map<IrModuleFragment, String>) {
    fun getExternalNameFor(file: IrFile): String {
        return getExternalNameFor(file.outputName, file.packageFqName.asString(), file.module.getJsOutputName())
    }

    fun getExternalNameFor(fileName: String, packageFqn: String, moduleName: String): String {
        return "$moduleName/${getFileStableName(fileName, packageFqn)}"
    }

    fun getExternalNameForExporterFile(file: IrFile): String {
        return getExternalNameForExporterFile(file.outputName, file.packageFqName.asString(), file.module.getJsOutputName())
    }

    fun getExternalNameForExporterFile(fileName: String, packageFqn: String, moduleName: String): String {
        return "${getExternalNameFor(fileName, packageFqn, moduleName)}$EXPORTER_FILE_POSTFIX"
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

    private fun getFileStableName(fileName: String, packageFqn: String): String {
        val prefix = packageFqn.replace('.', '/')
        return "$prefix${if (prefix.isNotEmpty()) "/" else ""}$fileName"
    }

    private val IrFile.outputName: String get() = getJsFileName() ?: nameWithoutExtension
    private val IrFile.stableFileName: String get() = getFileStableName(outputName, packageFqName.asString())
}
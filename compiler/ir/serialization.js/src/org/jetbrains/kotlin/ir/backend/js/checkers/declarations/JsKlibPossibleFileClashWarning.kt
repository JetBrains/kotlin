/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.checkers.declarations

import com.intellij.util.containers.without
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.backend.js.checkers.*
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.FqName

object JsKlibFileClashChecker : JsKlibModuleChecker<IrModuleFragment> {
    private val jsFileNameFqn = FqName("kotlin.js.JsFileName")

    override fun check(
        module: IrModuleFragment,
        context: JsKlibDiagnosticContext,
        reporter: IrDiagnosticReporter
    ) {
        val possibleFinalArtifactToIrFile = hashMapOf<FinalArtifactParameters, MutableList<FinalArtifactValuableParameters>>()

        for (file in module.files) {
            val jsFileNameAnnotation = file.getAnnotation(jsFileNameFqn)
            val jsFileNameAnnotationValue = jsFileNameAnnotation
                ?.arguments
                ?.singleOrNull()
                ?.let { (it as? IrConst)?.value as? String }

            val finalArtifactValuableParameters = FinalArtifactValuableParameters(
                file.packageFqName.asString(),
                jsFileNameAnnotationValue ?: file.name.substringBeforeLast(".kt"),
                file
            )

            val clashedFiles = possibleFinalArtifactToIrFile
                .getOrPut(finalArtifactValuableParameters.asFinalArtifactParameters()) { mutableListOf() }

            clashedFiles.add(finalArtifactValuableParameters)
        }

        for ((_, clashedFiles) in possibleFinalArtifactToIrFile) {
            if (clashedFiles.size == 1) continue

            val clashedFilesByCaseSensitiveData = buildMap {
                for (clashedFile in clashedFiles) {
                    getOrPut(clashedFile.computedFileName to clashedFile.packageFqn, { mutableListOf() })
                        .add(clashedFile)
                }
            }

            if (clashedFilesByCaseSensitiveData.size == 1) continue

            clashedFilesByCaseSensitiveData.forEach { (key, clashedFiles) ->
                val firstFileWithThisSensitivePath = clashedFiles.first().file

                reporter.at(firstFileWithThisSensitivePath, firstFileWithThisSensitivePath).report(
                    JsKlibErrors.CLASHED_FILES_IN_CASE_INSENSITIVE_FS,
                    clashedFilesByCaseSensitiveData
                        .without(key)
                        .values
                        .flatten()
                        .map { it.file.fileEntry }
                )
            }
        }
    }

    private class FinalArtifactValuableParameters(
        val packageFqn: String,
        val computedFileName: String,
        val file: IrFile,
    ) {
        fun asFinalArtifactParameters() = FinalArtifactParameters(packageFqn.lowercase(), computedFileName.lowercase())
    }

    private data class FinalArtifactParameters(val packageFqn: String, val fileName: String)
}

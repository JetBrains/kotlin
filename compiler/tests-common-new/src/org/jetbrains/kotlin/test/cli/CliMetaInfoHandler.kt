/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.cli

import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.globalMetadataInfoHandler
import org.jetbrains.kotlin.test.services.sourceFileProvider

class CliMetaInfoHandler(testServices: TestServices) : CliArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: CliArtifact) {
        addMetaInfos(module, info.kotlinDiagnostics)
    }

    private fun addMetaInfos(module: TestModule, diagnostics: List<CliDiagnostic>) {
        val files = module.files.sortedBy { it.startLineNumberInOriginalFile }
        val startLineNumberToFile = files.map { it.startLineNumberInOriginalFile to it }
        val fileToContent = files.associateWith { testServices.sourceFileProvider.getContentOfSourceFile(it).split("\n") }
        for (diagnostic in diagnostics) {
            // "+1" to always find the next file, even if the diagnostic is on the first line in the file.
            val position = startLineNumberToFile.binarySearchBy(diagnostic.lineBegin + 1) { it.first }
            // binarySearchBy returns `(-insertion_point - 1)`, where insertion_point is the next file after the one we're looking for
            // (because upon inserting, the list would need to be sorted). So we need to take `(-position - 2)`.
            val file = files[-position - 2]
            val content = fileToContent.getValue(file)
            // "-1" everywhere because line/column numbers are 1-based in the compiler output.
            val metaInfo = ParsedCodeMetaInfo(
                convertLineColumnToOffset(content, diagnostic.lineBegin - 1, diagnostic.columnBegin - 1),
                convertLineColumnToOffset(content, diagnostic.lineEnd - 1, diagnostic.columnEnd - 1),
                mutableListOf(),
                diagnostic.name,
                description = null,
            )
            testServices.globalMetadataInfoHandler.addMetadataInfosForFile(file, listOf(metaInfo))
        }
    }

    private fun convertLineColumnToOffset(content: List<String>, line: Int, column: Int): Int =
        content.subList(0, line).sumOf { it.length + 1 } + column

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
    }
}

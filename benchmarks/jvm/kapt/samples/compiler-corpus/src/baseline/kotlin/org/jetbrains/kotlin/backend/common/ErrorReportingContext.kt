/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.ir.IrDiagnosticReporter
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile

interface ErrorReportingContext {
    val diagnosticReporter: IrDiagnosticReporter

    /**
     * Writes the message to the output with `LOG` severity.
     */
    fun log(message: String)
}

fun IrElement.getCompilerMessageLocation(containingFile: IrFile): CompilerMessageLocation? =
    createCompilerMessageLocation(containingFile, this.startOffset, this.endOffset)

fun IrFile.getCompilerMessageLocation(element: IrElement): CompilerMessageLocation? =
    createCompilerMessageLocation(this, element.startOffset, element.endOffset)

private fun createCompilerMessageLocation(containingFile: IrFile, startOffset: Int, endOffset: Int): CompilerMessageLocation? {
    val sourceRangeInfo = containingFile.fileEntry.getSourceRangeInfo(startOffset, endOffset)
    return CompilerMessageLocation.create(
        path = sourceRangeInfo.filePath,
        line = sourceRangeInfo.startLineNumber + 1,
        column = sourceRangeInfo.startColumnNumber + 1,
        lineContent = null // TODO: retrieve the line content.
    )
}

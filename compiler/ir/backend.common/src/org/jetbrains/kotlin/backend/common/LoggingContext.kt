/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.getPackageFragment

interface LoggingContext {

    /**
     * Where to output the logging messages, warnings and errors.
     */
    val messageCollector: MessageCollector

    /**
     * Whether logging is enabled for the current [CompilerPhase] by the `-Xverbose-phases` CLI option.
     */
    var inVerbosePhase: Boolean

    /**
     * If [inVerbosePhase] is `true`, prints [message] to the [messageCollector].
     */
    fun log(message: () -> String) {
        if (inVerbosePhase) {
            // Not CompilerMessageSeverity.LOGGING, because that's only printed if the `-verbose` flag is passed.
            messageCollector.report(CompilerMessageSeverity.INFO, message())
        }
    }
}

fun LoggingContext.report(severity: CompilerMessageSeverity, element: IrElement?, irFile: IrFile?, message: String) {
    val location = if (element != null && irFile != null) element.getCompilerMessageLocation(irFile) else null
    messageCollector.report(severity, message, location)
}

fun LoggingContext.reportWarning(message: String, irFile: IrFile?, irElement: IrElement) {
    report(CompilerMessageSeverity.WARNING, irElement, irFile, message)
}

fun LoggingContext.reportCompilationWarning(message: String) {
    report(CompilerMessageSeverity.WARNING, null, null, message)
}

fun IrElement.getCompilerMessageLocation(containingFile: IrFile): CompilerMessageLocation? =
    createCompilerMessageLocation(containingFile, this.startOffset, this.endOffset)

fun IrBuilderWithScope.getCompilerMessageLocation(): CompilerMessageLocation? {
    val declaration = this.scope.scopeOwnerSymbol.owner as? IrDeclaration ?: return null
    val file = declaration.getPackageFragment() as? IrFile ?: return null
    return createCompilerMessageLocation(file, startOffset, endOffset)
}

private fun createCompilerMessageLocation(containingFile: IrFile, startOffset: Int, endOffset: Int): CompilerMessageLocation? {
    val sourceRangeInfo = containingFile.fileEntry.getSourceRangeInfo(startOffset, endOffset)
    return CompilerMessageLocation.create(
        path = sourceRangeInfo.filePath,
        line = sourceRangeInfo.startLineNumber + 1,
        column = sourceRangeInfo.startColumnNumber + 1,
        lineContent = null // TODO: retrieve the line content.
    )
}


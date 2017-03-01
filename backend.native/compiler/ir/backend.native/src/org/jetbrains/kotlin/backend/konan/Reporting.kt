package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile

internal val Context.messageCollector: MessageCollector
    get() = this.config.configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

internal fun getCompilerMessageLocation(irFile: IrFile, irElement: IrElement): CompilerMessageLocation {
    val sourceRangeInfo = irFile.fileEntry.getSourceRangeInfo(irElement.startOffset, irElement.endOffset)
    return CompilerMessageLocation.create(
            path = sourceRangeInfo.filePath,
            line = sourceRangeInfo.startLineNumber,
            column = sourceRangeInfo.startColumnNumber,
            lineContent = null // TODO: retrieve the line content.
    )
}

internal fun Context.reportCompilationError(message: String, irFile: IrFile, irElement: IrElement): Nothing {
    val location = getCompilerMessageLocation(irFile, irElement)
    this.messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    throw KonanCompilationException()
}
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.messageCollector
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.getCompilerMessageLocation

internal fun BackendContext.reportCompilationError(message: String, irFile: IrFile, irElement: IrElement): Nothing {
    val location = irElement.getCompilerMessageLocation(irFile)
    this.messageCollector.report(CompilerMessageSeverity.ERROR, message, location)
    throw KonanCompilationException()
}

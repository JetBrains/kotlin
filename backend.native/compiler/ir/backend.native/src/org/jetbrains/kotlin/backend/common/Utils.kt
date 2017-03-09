package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.util.getCompilerMessageLocation

// TODO: declare as a member of BackendContext
val BackendContext.compilerConfiguration: CompilerConfiguration
    get() = when (this) {
        is KonanBackendContext -> this.config.configuration
        is JvmBackendContext -> this.state.configuration
        else -> TODO(this.toString())
    }

val BackendContext.messageCollector: MessageCollector
    get() = this.compilerConfiguration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY)

fun BackendContext.reportWarning(message: String, irFile: IrFile, irElement: IrElement) {
    val location = irElement.getCompilerMessageLocation(irFile)
    this.messageCollector.report(CompilerMessageSeverity.WARNING, message, location)
}
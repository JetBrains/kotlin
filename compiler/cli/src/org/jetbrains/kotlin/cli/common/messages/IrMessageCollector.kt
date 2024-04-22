/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

import org.jetbrains.kotlin.ir.util.IrMessageLogger

class IrMessageCollector(private val messageCollector: MessageCollector) : IrMessageLogger {
    override fun report(severity: IrMessageLogger.Severity, message: String, location: IrMessageLogger.Location?) {
        messageCollector.report(severityToCLISeverity(severity), message, locationToCLILocation(location))
    }

    companion object {
        private fun severityToCLISeverity(severity: IrMessageLogger.Severity): CompilerMessageSeverity {
            return when (severity) {
                IrMessageLogger.Severity.INFO -> CompilerMessageSeverity.INFO
                IrMessageLogger.Severity.WARNING -> CompilerMessageSeverity.WARNING
                IrMessageLogger.Severity.STRONG_WARNING -> CompilerMessageSeverity.STRONG_WARNING
                IrMessageLogger.Severity.ERROR -> CompilerMessageSeverity.ERROR
            }
        }

        private fun locationToCLILocation(location: IrMessageLogger.Location?): CompilerMessageLocation? {
            return location?.run {
                CompilerMessageLocation.Companion.create(filePath, line, column, null)
            }
        }
    }
}
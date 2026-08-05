/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId

internal class BufferingMessageCollector : MessageCollectorWithDiagnosticId {
    val messages: List<Message>
        field = mutableListOf<Message>()

    fun forward(other: MessageCollector) {
        for ((severity, message, location, diagnosticId) in messages) {
            if (other is MessageCollectorWithDiagnosticId) {
                other.report(severity, message, location, diagnosticId)
            } else {
                other.report(severity, message, location)
            }
        }
    }

    override fun clear() {
        messages.clear()
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        messages.add(Message(severity, message, location, null))
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
        diagnosticId: String?,
    ) {
        messages.add(Message(severity, message, location, diagnosticId))
    }

    override fun hasErrors(): Boolean =
        messages.any { it.severity.isError }

    override fun toString(): String = messages.joinToString("\n")

    data class Message(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?,
        val diagnosticId: String?,
    ) {
        override fun toString(): String = buildString {
            if (location != null) {
                append(location)
                append(": ")
            }
            append(severity.presentableName)
            if (diagnosticId != null) append(" $diagnosticId")
            append(": ")
            append(message)
        }
    }
}

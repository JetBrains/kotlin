/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.serializability

import org.jetbrains.kotlin.buildtools.internal.serializability.Message
import org.jetbrains.kotlin.buildtools.internal.serializability.beforeSerialization
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorWithDiagnosticId

internal class MessageReportingMessageCollectorWithDiagnosticId(private val messageReporter: (Message) -> Unit) :
    MessageCollectorWithDiagnosticId {
    override fun clear() {
        messageReporter(Message.CompilerMessageClear)
    }

    override fun hasErrors(): Boolean {
        return false
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        messageReporter(Message.CompilerMessageWithDiagnosticId(severity, message, location?.beforeSerialization(), diagnosticId = null))
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
        diagnosticId: String?,
    ) {
        messageReporter(Message.CompilerMessageWithDiagnosticId(severity, message, location?.beforeSerialization(), diagnosticId))
    }
}

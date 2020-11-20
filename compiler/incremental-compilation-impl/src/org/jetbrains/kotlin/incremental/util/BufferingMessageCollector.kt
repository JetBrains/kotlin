/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.util

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.util.ArrayList

internal class BufferingMessageCollector : MessageCollector {
    private class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageSourceLocation?)

    private val messages = ArrayList<Message>()

    override fun clear() {
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        messages.add(
            Message(
                severity,
                message,
                location
            )
        )
    }

    override fun hasErrors(): Boolean =
        messages.any { it.severity.isError }

    fun flush(delegate: MessageCollector) {
        messages.forEach { delegate.report(it.severity, it.message, it.location) }
    }
}
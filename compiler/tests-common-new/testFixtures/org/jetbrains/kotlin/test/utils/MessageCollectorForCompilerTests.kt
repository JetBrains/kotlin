/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import java.io.PrintStream

class MessageCollectorForCompilerTests(
    errStream: PrintStream,
    private val messageRenderer: MessageRenderer
) : MessageCollector {
    private val printingCollector = PrintingMessageCollector(errStream, messageRenderer, /*verbose=*/ false)
    private var hasErrors = false

    val nonSourceMessages: List<String>
        field = mutableListOf<String>()

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        hasErrors = hasErrors || severity.isError
        if (location == null && severity <= CompilerMessageSeverity.WARNING) {
            nonSourceMessages.add(messageRenderer.render(severity, message, location))
        }
        printingCollector.report(severity, message, location)
    }

    override fun hasErrors(): Boolean = hasErrors

    override fun clear() {
        nonSourceMessages.clear()
        printingCollector.clear()
    }
}

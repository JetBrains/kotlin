/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package server

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl.Message

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


interface OnReport{
    fun onReport(msg: Message)
}

class RemoteMessageCollector(
    private val onReport: OnReport
) : MessageCollector {


    private val messages: MutableList<Message> = mutableListOf<Message>()

    val errors: List<Message>
        get() = messages.filter { it.severity.isError }

    override fun clear() {
        messages.clear()
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        val msg = Message(severity, message, location)
        messages.add(msg)
        onReport.onReport(msg)
    }

    override fun hasErrors(): Boolean =
        messages.any { it.severity.isError }

}
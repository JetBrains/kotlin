import kotlinx.coroutines.channels.Channel
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl.Message
import org.jetbrains.kotlin.server.CompileResponseGrpc

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


interface MessageSender{
    fun send(msg: Message)
}

class RemoteMessageCollector(
    private val messageSender: MessageSender
): MessageCollector {


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
        messages.add(Message(severity, message, location))
        println("message is $message severity is $severity location is $location")
        try {
            messageSender.send(Message(severity, message, location))
        }catch (e: Exception){
            println("exception is $e")
            e.printStackTrace()
        }
    }

    override fun hasErrors(): Boolean =
        messages.any { it.severity.isError }

}
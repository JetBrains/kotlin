package org.jetbrains.kotlin.util

import kotlin.system.exitProcess

interface Logger {
    fun log(message: String)
    fun error(message: String, throwable: Throwable? = null)
    fun warning(message: String)
    fun fatal(message: String): Nothing
    fun lifecycle(message: String)
}

interface WithLogger {
    val logger: Logger
}

object DummyLogger : Logger {
    override fun log(message: String) = println(message)
    override fun error(message: String, throwable: Throwable?) {
        println("e: $message")
        throwable?.also {
            println("${it.message}:\n${it.stackTraceToString()} ")
        }
    }

    override fun warning(message: String) = println("w: $message")
    override fun fatal(message: String): Nothing {
        println("e: $message")
        exitProcess(1)
    }

    override fun lifecycle(message: String) = println("i: $message")
}

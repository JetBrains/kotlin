package org.jetbrains.kotlin.util

import kotlin.system.exitProcess

interface Logger {
    fun log(message: String)
    fun error(message: String)
    fun warning(message: String)
    fun fatal(message: String): Nothing
}

interface WithLogger {
    val logger: Logger
}

/**
 * The default logger that just prints messages to console.
 *
 * Caution: Whenever possible please try to use more appropriate alternatives to [DummyLogger] that
 * would direct the output to the proper logs. Here are some of them:
 * - [org.jetbrains.kotlin.gradle.utils.GradleLoggerAdapter] - to be used in Gradle plugins.
 * - [org.jetbrains.kotlin.cli.common.messages.CompilerLoggerAdapter] - to be used in all flavors of the Kotlin compiler. Can be accessed
 *   via [org.jetbrains.kotlin.cli.common.messages.toLogger] and [org.jetbrains.kotlin.cli.common.messages.getLogger].
 * - [org.jetbrains.kotlin.backend.common.IrMessageLoggerAdapter] - to be used in those parts of the compiler where
 *   [org.jetbrains.kotlin.cli.common.messages.CompilerLoggerAdapter] is not available. Can be accessed via
 *   [org.jetbrains.kotlin.backend.common.toLogger].
 */
object DummyLogger : Logger {
    override fun log(message: String) = println(message)
    override fun error(message: String) = println("e: $message")
    override fun warning(message: String) = println("w: $message")
    override fun fatal(message: String): Nothing {
        println("e: $message")
        exitProcess(1)
    }
}

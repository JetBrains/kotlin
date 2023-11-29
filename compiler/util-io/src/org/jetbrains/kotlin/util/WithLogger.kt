package org.jetbrains.kotlin.util

import kotlin.system.exitProcess

interface Logger {
    fun log(message: String)
    fun warning(message: String)
    fun error(message: String)

    @Deprecated(FATAL_DEPRECATION_MESSAGE, ReplaceWith(FATAL_REPLACEMENT))
    fun fatal(message: String): Nothing

    companion object {
        const val FATAL_DEPRECATION_MESSAGE = "Invocation of fatal() may cause severe side effects such as throwing an exception or " +
                "even terminating the current JVM process (check various implementations of this function for details). " +
                "The code that uses Logger.fatal() sometimes expects a particular kind of side effect. " +
                "This is an undesirable design. And it's definitely not a responsibility of Logger to influence " +
                "the execution flow of the program."
        const val FATAL_REPLACEMENT = "error(message)"
    }
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
    override fun warning(message: String) = println("w: $message")
    override fun error(message: String) = println("e: $message")

    @Deprecated(Logger.FATAL_DEPRECATION_MESSAGE, ReplaceWith(Logger.FATAL_REPLACEMENT))
    override fun fatal(message: String): Nothing {
        error(message)
        exitProcess(1) // WARNING: This would stop the JVM process!
    }
}

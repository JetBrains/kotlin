/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.model

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.util.EnumMap
import java.util.concurrent.ConcurrentLinkedQueue

class LogEntry(
    val logLevel: LogLevel,
    val message: String,
    val exception: Throwable? = null,
) {
    override fun toString() = "$logLevel: $message ${exception?.stackTraceToString() ?: ""}"
}

class TestKotlinLogger : KotlinLogger {
    override val isDebugEnabled = true

    private val logMessages = ConcurrentLinkedQueue<LogEntry>()
    private val logMessagesByLevelImpl = EnumMap<LogLevel, ConcurrentLinkedQueue<String>>(LogLevel::class.java).apply {
        for (logLevel in LogLevel.entries) {
            put(logLevel, ConcurrentLinkedQueue())
        }
    }

    val logMessagesByLevel: Map<LogLevel, Collection<String>> = logMessagesByLevelImpl

    override fun debug(msg: String) {
        saveLogEntry(LogLevel.DEBUG, msg)
    }

    override fun error(msg: String, throwable: Throwable?) {
        saveLogEntry(LogLevel.ERROR, msg, throwable)
    }

    override fun info(msg: String) {
        saveLogEntry(LogLevel.INFO, msg)
    }

    override fun lifecycle(msg: String) {
        saveLogEntry(LogLevel.LIFECYCLE, msg)
    }

    override fun warn(msg: String) {
        saveLogEntry(LogLevel.WARN, msg)
    }

    private fun saveLogEntry(logLevel: LogLevel, msg: String, throwable: Throwable? = null) {
        logMessagesByLevelImpl.getValue(logLevel).add(msg)
        logMessages.add(LogEntry(logLevel, msg, throwable))
    }

    fun printBuildOutput(maxLogLevel: LogLevel) {
        println("Build output (with messages up to $maxLogLevel level): ")
        for (logMessage in logMessages.filter { it.logLevel <= maxLogLevel }) {
            println(logMessage)
        }
    }
}
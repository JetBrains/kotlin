/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.runner

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashMap

enum class LogLevel {
    ERROR,
    WARN,
    LIFECYCLE,
    INFO,
    DEBUG,
}

class TestKotlinLogger(private val defaultLogger: KotlinLogger) : KotlinLogger {
    override val isDebugEnabled = true

    private val loggedMessagesImpl = HashMap<LogLevel, Queue<String>>().apply {
        for (level in LogLevel.entries) {
            put(level, ConcurrentLinkedQueue())
        }
    }

    val loggedMessages: Map<LogLevel, Collection<String>>
        get() = loggedMessagesImpl

    fun clear() {
        for (logs in loggedMessagesImpl.values) {
            logs.clear()
        }
    }

    override fun error(msg: String, throwable: Throwable?) {
        loggedMessagesImpl.getValue(LogLevel.ERROR).add(msg)
        defaultLogger.error(msg, throwable)
    }

    override fun warn(msg: String) {
        loggedMessagesImpl.getValue(LogLevel.WARN).add(msg)
        defaultLogger.warn(msg)
    }

    override fun info(msg: String) {
        loggedMessagesImpl.getValue(LogLevel.INFO).add(msg)
        defaultLogger.info(msg)
    }

    override fun debug(msg: String) {
        loggedMessagesImpl.getValue(LogLevel.DEBUG).add(msg)
        defaultLogger.debug(msg)
    }

    override fun lifecycle(msg: String) {
        loggedMessagesImpl.getValue(LogLevel.LIFECYCLE).add(msg)
        defaultLogger.lifecycle(msg)
    }
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger

private enum class LogLevel {
    ERROR,
    WARN,
    LIFECYCLE,
    INFO,
    DEBUG,
    ;

    companion object {
        fun fromString(rawValue: String) = entries.firstOrNull { it.name.equals(rawValue, ignoreCase = true) }
            ?: error("Unknown log level for the DefaultKotlinLogger: $rawValue")
    }
}

internal object DefaultKotlinLogger : KotlinLogger {
    private val logLevel: LogLevel = System.getProperty("kotlin.build-tools-api.log.level")?.let {
        LogLevel.fromString(it)
    } ?: LogLevel.WARN

    private val LogLevel.isEnabled: Boolean
        get() = logLevel >= this

    override val isDebugEnabled: Boolean
        get() = LogLevel.DEBUG.isEnabled

    override fun error(msg: String, throwable: Throwable?) {
        if (!LogLevel.ERROR.isEnabled) return
        System.err.println("e: $msg")
        throwable?.printStackTrace()
    }

    override fun warn(msg: String) {
        if (!LogLevel.WARN.isEnabled) return
        System.err.println("w: $msg")
    }

    override fun info(msg: String) {
        if (!LogLevel.INFO.isEnabled) return
        println("i: $msg")
    }

    override fun debug(msg: String) {
        if (!LogLevel.DEBUG.isEnabled) return
        println("d: $msg")
    }

    override fun lifecycle(msg: String) {
        if (!LogLevel.LIFECYCLE.isEnabled) return
        println("l: $msg")
    }
}
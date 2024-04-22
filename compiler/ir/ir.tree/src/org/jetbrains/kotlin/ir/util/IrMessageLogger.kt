/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

interface IrMessageLogger {

    enum class Severity {
        INFO, WARNING, STRONG_WARNING, ERROR
    }

    data class Location(val filePath: String, val line: Int, val column: Int)

    fun report(severity: Severity, message: String, location: Location?)

    object None : IrMessageLogger {
        override fun report(severity: Severity, message: String, location: Location?) {}
    }

    companion object {
        @JvmStatic
        val IR_MESSAGE_LOGGER = CompilerConfigurationKey<IrMessageLogger>("ir message logger")
    }
}

val CompilerConfiguration.irMessageLogger: IrMessageLogger
    get() = this[IrMessageLogger.IR_MESSAGE_LOGGER] ?: IrMessageLogger.None

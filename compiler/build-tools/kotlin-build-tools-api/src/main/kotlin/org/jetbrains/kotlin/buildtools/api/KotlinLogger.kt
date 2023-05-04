/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * An interface for logging messages in Kotlin build tools API.
 * This interface should be used as a primary logging interface.
 */
interface KotlinLogger {
    /**
     * Returns true if debug-level logging is enabled, false otherwise.
     */
    val isDebugEnabled: Boolean

    /**
     * Logs an error message with an optional throwable.
     *
     * @param msg The error message to log.
     * @param throwable The throwable associated with the error, if any.
     */
    fun error(msg: String, throwable: Throwable? = null)

    /**
     * Logs a warning message.
     *
     * @param msg The warning message to log.
     */
    fun warn(msg: String)

    /**
     * Logs an info message.
     *
     * @param msg The info message to log.
     */
    fun info(msg: String)

    /**
     * Logs a debug message.
     *
     * @param msg The debug message to log.
     */
    fun debug(msg: String)

    /**
     * Logs a message related to the build lifecycle.
     *
     * @param msg The lifecycle message to log.
     */
    fun lifecycle(msg: String)
}
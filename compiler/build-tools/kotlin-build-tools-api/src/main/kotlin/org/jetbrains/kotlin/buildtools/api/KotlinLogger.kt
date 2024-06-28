/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * A logger object used to log messages from the Build Tools API.
 */
public interface KotlinLogger {
    public val isDebugEnabled: Boolean
    public fun error(msg: String, throwable: Throwable? = null)
    public fun warn(msg: String)
    public fun info(msg: String)
    public fun debug(msg: String)
    public fun lifecycle(msg: String)
}
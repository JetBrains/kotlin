/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

interface KotlinLogger {
    val isDebugEnabled: Boolean
    fun error(msg: String, throwable: Throwable? = null)
    fun warn(msg: String)
    fun info(msg: String)
    fun debug(msg: String)
    fun lifecycle(msg: String)
}
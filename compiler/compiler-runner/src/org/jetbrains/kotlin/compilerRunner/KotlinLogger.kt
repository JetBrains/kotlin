/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

interface KotlinLogger {
    fun error(msg: String)
    fun warn(msg: String)
    fun info(msg: String)
    fun debug(msg: String)

    val isDebugEnabled: Boolean
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.api

data class Callbacks(
    val messageLogger: MessageLogger?,
)

interface MessageLogger {
    fun report(level: FacadeLogLevel, message: String)
}

enum class FacadeLogLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG,
}
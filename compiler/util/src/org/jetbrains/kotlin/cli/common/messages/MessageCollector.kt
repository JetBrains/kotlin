/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.common.messages

@JvmDefaultWithCompatibility
interface MessageCollector {
    fun clear()

    fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation? = null)

    fun hasErrors(): Boolean

    companion object {
        val NONE: MessageCollector = object : MessageCollector {
            override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
                // Do nothing
            }

            override fun clear() {
                // Do nothing
            }

            override fun hasErrors(): Boolean = false
        }
    }
}

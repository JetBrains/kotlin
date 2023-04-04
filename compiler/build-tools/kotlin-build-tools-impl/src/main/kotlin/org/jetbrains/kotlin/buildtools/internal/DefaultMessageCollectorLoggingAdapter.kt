/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.cli.common.messages.*

internal object DefaultMessageCollectorLoggingAdapter : MessageCollector {
    private val messageRenderer = MessageRenderer.PLAIN_FULL_PATHS
    override fun clear() {}

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        val renderedMessage = messageRenderer.render(severity, message, location)
        if (severity.isError) {
            System.err.println(renderedMessage)
        } else {
            println(renderedMessage)
        }
    }

    override fun hasErrors() = false
}
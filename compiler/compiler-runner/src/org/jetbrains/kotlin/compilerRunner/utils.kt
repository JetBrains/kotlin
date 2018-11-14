/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.StringReader

fun reportInternalCompilerError(messageCollector: MessageCollector) {
    messageCollector.report(CompilerMessageSeverity.ERROR, "Compiler terminated with internal error")
}

fun processCompilerOutput(
    environment: CompilerEnvironment,
    stream: ByteArrayOutputStream,
    exitCode: ExitCode?
) {
    processCompilerOutput(environment.messageCollector, environment.outputItemsCollector, stream, exitCode)
}

fun processCompilerOutput(
    messageCollector: MessageCollector,
    outputItemsCollector: OutputItemsCollector,
    stream: ByteArrayOutputStream,
    exitCode: ExitCode?
) {
    val reader = BufferedReader(StringReader(stream.toString()))
    CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, reader, outputItemsCollector)

    if (ExitCode.INTERNAL_ERROR == exitCode) {
        reportInternalCompilerError(messageCollector)
    }
}

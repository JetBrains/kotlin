/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package model

import common.toDomain
import common.toGrpc
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.server.CompileResponseGrpc
import org.jetbrains.kotlin.server.CompilerMessageGrpc

// TODO: this class is basically copy of MessageCollectorImpl.Message
data class CompilerMessage(
    val severity: CompilerMessageSeverity,
    val message: String,
    val location: CompilerMessageSourceLocation?
) : CompileResponse {

    override fun toString(): String = buildString {
        if (location != null) {
            append(location)
            append(": ")
        }
        append(severity.presentableName)
        append(": ")
        append(message)
    }
}

fun CompilerMessage.toGrpc(): CompilerMessageGrpc {
    val compilerMessage = CompilerMessageGrpc.newBuilder()
    compilerMessage.setMessage(message)
    compilerMessage.setCompilerMessageSeverity(severity.toGrpc())
    location?.let { location ->
        compilerMessage.setCompilerMessageSourceLocation(location.toGrpc())
    }
    return compilerMessage.build()
}

fun CompilerMessageGrpc.toCompileResponse(): CompileResponseGrpc {
    return CompileResponseGrpc.newBuilder().setCompilerMessage(this).build()
}

fun CompilerMessageGrpc.toDomain(): CompilerMessage {
    return CompilerMessage(
        compilerMessageSeverity.toDomain(),
        message,
        compilerMessageSourceLocation?.toDomain()
    )
}

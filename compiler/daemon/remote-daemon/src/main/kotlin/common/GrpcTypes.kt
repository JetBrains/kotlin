/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import com.google.protobuf.ByteString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.CompileRequest
import model.toDomain
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.server.CompileRequestGrpc
import org.jetbrains.kotlin.server.CompilerMessageSeverityGrpc
import org.jetbrains.kotlin.server.CompilerMessageSourceLocationGrpc
import org.jetbrains.kotlin.server.CompilerModeGrpc
import org.jetbrains.kotlin.server.TargetPlatformGrpc

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


fun CompileRequestGrpc.toDomain(): CompileRequest = when {
    hasMetadata() -> metadata.toDomain()
    hasSourceFileChunk() -> sourceFileChunk.toDomain()
    hasFileTransferRequest() -> fileTransferRequest.toDomain()
    else -> error("Unknown CompileRequestGrpc type") // TODO fix
}


fun CompilerMode.toGrpc(): CompilerModeGrpc {
    return when (this) {
        CompilerMode.INCREMENTAL_COMPILER -> CompilerModeGrpc.INCREMENTAL_COMPILER
        CompilerMode.NON_INCREMENTAL_COMPILER -> CompilerModeGrpc.NON_INCREMENTAL_COMPILER
        CompilerMode.JPS_COMPILER -> CompilerModeGrpc.JPS_COMPILER
    }
}

fun CompilerModeGrpc.toDomain(): CompilerMode {
    return when (this) {
        CompilerModeGrpc.INCREMENTAL_COMPILER -> CompilerMode.INCREMENTAL_COMPILER
        CompilerModeGrpc.NON_INCREMENTAL_COMPILER -> CompilerMode.NON_INCREMENTAL_COMPILER
        CompilerModeGrpc.JPS_COMPILER -> CompilerMode.JPS_COMPILER
        CompilerModeGrpc.UNRECOGNIZED -> CompilerMode.NON_INCREMENTAL_COMPILER //TODO check
    }
}

fun CompileService.TargetPlatform.toGrpc(): TargetPlatformGrpc {
    return when (this) {
        CompileService.TargetPlatform.JVM -> TargetPlatformGrpc.JVM
        CompileService.TargetPlatform.JS -> TargetPlatformGrpc.JS
        CompileService.TargetPlatform.METADATA -> TargetPlatformGrpc.METADATA
    }
}

fun TargetPlatformGrpc.toDomain(): CompileService.TargetPlatform {
    return when (this) {
        TargetPlatformGrpc.JVM -> CompileService.TargetPlatform.JVM
        TargetPlatformGrpc.JS -> CompileService.TargetPlatform.JS
        TargetPlatformGrpc.METADATA -> CompileService.TargetPlatform.METADATA
        TargetPlatformGrpc.UNRECOGNIZED -> CompileService.TargetPlatform.JVM // TODO check
    }
}


fun CompilerMessageSourceLocation.toGrpc(): CompilerMessageSourceLocationGrpc {
    return CompilerMessageSourceLocationGrpc.newBuilder()
        .setPath(this.path)
        .setLine(this.line)
        .setColumn(this.column)
        .setLineContent(this.lineContent)
        .build()
}

fun CompilerMessageSeverity.toGrpc(): CompilerMessageSeverityGrpc{
    return when(this){
        CompilerMessageSeverity.INFO -> CompilerMessageSeverityGrpc.INFO
        CompilerMessageSeverity.ERROR -> CompilerMessageSeverityGrpc.ERROR
        CompilerMessageSeverity.WARNING -> CompilerMessageSeverityGrpc.WARNING
        CompilerMessageSeverity.LOGGING -> CompilerMessageSeverityGrpc.LOGGING
        CompilerMessageSeverity.OUTPUT -> CompilerMessageSeverityGrpc.OUTPUT
        CompilerMessageSeverity.EXCEPTION -> CompilerMessageSeverityGrpc.EXCEPTION
        CompilerMessageSeverity.STRONG_WARNING -> CompilerMessageSeverityGrpc.STRONG_WARNING
        CompilerMessageSeverity.FIXED_WARNING -> CompilerMessageSeverityGrpc.FIXED_WARNING
    }
}

fun CompilerMessageSeverityGrpc.toDomain(): CompilerMessageSeverity {
    return when (this) {
        CompilerMessageSeverityGrpc.INFO -> CompilerMessageSeverity.INFO
        CompilerMessageSeverityGrpc.ERROR -> CompilerMessageSeverity.ERROR
        CompilerMessageSeverityGrpc.WARNING -> CompilerMessageSeverity.WARNING
        CompilerMessageSeverityGrpc.LOGGING -> CompilerMessageSeverity.LOGGING
        CompilerMessageSeverityGrpc.OUTPUT -> CompilerMessageSeverity.OUTPUT
        CompilerMessageSeverityGrpc.EXCEPTION -> CompilerMessageSeverity.EXCEPTION
        CompilerMessageSeverityGrpc.STRONG_WARNING -> CompilerMessageSeverity.STRONG_WARNING
        CompilerMessageSeverityGrpc.FIXED_WARNING -> CompilerMessageSeverity.FIXED_WARNING
        CompilerMessageSeverityGrpc.UNRECOGNIZED -> CompilerMessageSeverity.INFO // TODO double check
    }
}

fun CompilerMessageSourceLocationGrpc.toDomain(): CompilerMessageSourceLocation? {
    // TODO: there also exist class CompilerMessageLocationWithRange, investigate the difference
    return CompilerMessageLocation.create(path, line, column, lineContent)
}
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
import org.jetbrains.kotlin.server.CompileRequestProto
import org.jetbrains.kotlin.server.CompilerMessageSeverityProto
import org.jetbrains.kotlin.server.CompilerMessageSourceLocationProto
import org.jetbrains.kotlin.server.CompilerModeProto
import org.jetbrains.kotlin.server.TargetPlatformProto

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


fun CompileRequestProto.toDomain(): CompileRequest = when {
    hasMetadata() -> metadata.toDomain()
    hasSourceFileChunk() -> sourceFileChunk.toDomain()
    hasFileTransferRequest() -> fileTransferRequest.toDomain()
    else -> error("Unknown CompileRequestProto type") // TODO fix
}


fun CompilerMode.toProto(): CompilerModeProto {
    return when (this) {
        CompilerMode.INCREMENTAL_COMPILER -> CompilerModeProto.INCREMENTAL_COMPILER
        CompilerMode.NON_INCREMENTAL_COMPILER -> CompilerModeProto.NON_INCREMENTAL_COMPILER
        CompilerMode.JPS_COMPILER -> CompilerModeProto.JPS_COMPILER
    }
}

fun CompilerModeProto.toDomain(): CompilerMode {
    return when (this) {
        CompilerModeProto.INCREMENTAL_COMPILER -> CompilerMode.INCREMENTAL_COMPILER
        CompilerModeProto.NON_INCREMENTAL_COMPILER -> CompilerMode.NON_INCREMENTAL_COMPILER
        CompilerModeProto.JPS_COMPILER -> CompilerMode.JPS_COMPILER
        CompilerModeProto.UNRECOGNIZED -> CompilerMode.NON_INCREMENTAL_COMPILER //TODO check
    }
}

fun CompileService.TargetPlatform.toProto(): TargetPlatformProto {
    return when (this) {
        CompileService.TargetPlatform.JVM -> TargetPlatformProto.JVM
        CompileService.TargetPlatform.JS -> TargetPlatformProto.JS
        CompileService.TargetPlatform.METADATA -> TargetPlatformProto.METADATA
    }
}

fun TargetPlatformProto.toDomain(): CompileService.TargetPlatform {
    return when (this) {
        TargetPlatformProto.JVM -> CompileService.TargetPlatform.JVM
        TargetPlatformProto.JS -> CompileService.TargetPlatform.JS
        TargetPlatformProto.METADATA -> CompileService.TargetPlatform.METADATA
        TargetPlatformProto.UNRECOGNIZED -> CompileService.TargetPlatform.JVM // TODO check
    }
}


fun CompilerMessageSourceLocation.toProto(): CompilerMessageSourceLocationProto {
    return CompilerMessageSourceLocationProto.newBuilder()
        .setPath(this.path)
        .setLine(this.line)
        .setColumn(this.column)
        .setLineContent(this.lineContent)
        .build()
}

fun CompilerMessageSeverity.toProto(): CompilerMessageSeverityProto{
    return when(this){
        CompilerMessageSeverity.INFO -> CompilerMessageSeverityProto.INFO
        CompilerMessageSeverity.ERROR -> CompilerMessageSeverityProto.ERROR
        CompilerMessageSeverity.WARNING -> CompilerMessageSeverityProto.WARNING
        CompilerMessageSeverity.LOGGING -> CompilerMessageSeverityProto.LOGGING
        CompilerMessageSeverity.OUTPUT -> CompilerMessageSeverityProto.OUTPUT
        CompilerMessageSeverity.EXCEPTION -> CompilerMessageSeverityProto.EXCEPTION
        CompilerMessageSeverity.STRONG_WARNING -> CompilerMessageSeverityProto.STRONG_WARNING
        CompilerMessageSeverity.FIXED_WARNING -> CompilerMessageSeverityProto.FIXED_WARNING
    }
}

fun CompilerMessageSeverityProto.toDomain(): CompilerMessageSeverity {
    return when (this) {
        CompilerMessageSeverityProto.INFO -> CompilerMessageSeverity.INFO
        CompilerMessageSeverityProto.ERROR -> CompilerMessageSeverity.ERROR
        CompilerMessageSeverityProto.WARNING -> CompilerMessageSeverity.WARNING
        CompilerMessageSeverityProto.LOGGING -> CompilerMessageSeverity.LOGGING
        CompilerMessageSeverityProto.OUTPUT -> CompilerMessageSeverity.OUTPUT
        CompilerMessageSeverityProto.EXCEPTION -> CompilerMessageSeverity.EXCEPTION
        CompilerMessageSeverityProto.STRONG_WARNING -> CompilerMessageSeverity.STRONG_WARNING
        CompilerMessageSeverityProto.FIXED_WARNING -> CompilerMessageSeverity.FIXED_WARNING
        CompilerMessageSeverityProto.UNRECOGNIZED -> CompilerMessageSeverity.INFO // TODO double check
    }
}

fun CompilerMessageSourceLocationProto.toDomain(): CompilerMessageSourceLocation? {
    // TODO: there also exist class CompilerMessageLocationWithRange, investigate the difference
    return CompilerMessageLocation.create(path, line, column, lineContent)
}
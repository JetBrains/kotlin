/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package common

import com.google.protobuf.ByteString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import model.DaemonJVMOptionsConfigurator
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerMode
import org.jetbrains.kotlin.server.CompilationOptionsGrpc
import org.jetbrains.kotlin.server.CompileResponseGrpc
import org.jetbrains.kotlin.server.CompilerMessageSeverityGrpc
import org.jetbrains.kotlin.server.CompilerMessageSourceLocationGrpc
import org.jetbrains.kotlin.server.CompilerModeGrpc
import org.jetbrains.kotlin.server.ConnectResponseGrpc
import org.jetbrains.kotlin.server.DaemonJVMOptionsConfiguratorGrpc
import org.jetbrains.kotlin.server.DaemonMessageGrpc
import org.jetbrains.kotlin.server.FileChunkGrpc
import org.jetbrains.kotlin.server.TargetPlatformGrpc

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun CompilationOptions.toCompilationOptionsGrpc(): CompilationOptionsGrpc {
    return CompilationOptionsGrpc.newBuilder()
        .setCompilerMode(this.compilerMode.toGrpc())
        .setTargetPlatform(this.targetPlatform.toGrpc())
        .setReportSeverity(this.reportSeverity)
        .build()
}

fun CompilerMode.toGrpc(): CompilerModeGrpc {
    return when (this) {
        CompilerMode.INCREMENTAL_COMPILER -> CompilerModeGrpc.INCREMENTAL_COMPILER
        CompilerMode.NON_INCREMENTAL_COMPILER -> CompilerModeGrpc.NON_INCREMENTAL_COMPILER
        CompilerMode.JPS_COMPILER -> CompilerModeGrpc.JPS_COMPILER
    }
}

fun CompilerModeGrpc.toDomain(): CompilerMode {
    return when (this){
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

fun MessageCollectorImpl.Message.toGrpc(): DaemonMessageGrpc {
    val daemonMessage = DaemonMessageGrpc.newBuilder()
    daemonMessage.setMessage(message)
    daemonMessage.setCompilerMessageSeverity(severity.toGrpc())
    location?.let { location ->
        daemonMessage.setCompilerMessageSourceLocation(location.toGrpc())
    }
    return daemonMessage.build()
}

fun DaemonMessageGrpc.toConnectResponseGrpc(): ConnectResponseGrpc {
    return ConnectResponseGrpc.newBuilder().setDaemonMessage(this).build()
}

fun DaemonMessageGrpc.toCompileResponseGrpc(): CompileResponseGrpc {
    return CompileResponseGrpc.newBuilder().setDaemonMessage(this).build()
}

fun CompilationOptionsGrpc.toDomain(): CompilationOptions {
    return CompilationOptions(
        compilerMode = compilerMode.toDomain(),
        targetPlatform = targetPlatform.toDomain(),
        reportSeverity = reportSeverity,
        requestedCompilationResults = requestedCompilationResultsList.toTypedArray(),
        reportCategories = reportCategoriesList.toTypedArray(),
        kotlinScriptExtensions = kotlinScriptExtensionsList.toTypedArray()
    )
}

fun FileChunkGrpc.fromBytes(fileName: String, bytes: List<Byte>): FileChunkGrpc {
    return FileChunkGrpc.newBuilder()
        .setFilePath(fileName)
        .setContent(ByteString.copyFrom(bytes.toByteArray()))
        .build()
}

fun DaemonJVMOptionsConfigurator.toGrpc(): DaemonJVMOptionsConfiguratorGrpc {

    val builder = DaemonJVMOptionsConfiguratorGrpc.newBuilder()

    additionalParams.forEachIndexed { index, param ->
        builder.setAdditionalParams(index, param)
    }

    return builder
        .setInheritMemoryLimits(inheritMemoryLimits)
        .setInheritAdditionalProperties(inheritAdditionalProperties)
        .setInheritOtherJvmOptions(inheritOtherJvmOptions)
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



fun generateDummyCompilerMessages(): Flow<CompileResponseGrpc> = flow {

    for (i in 1..100) {
        val location = CompilerMessageSourceLocationGrpc.newBuilder()
            .setPath("this/is/random/path")
            .setLine(23)
            .setColumn(324)
            .setLineEnd(42)
            .setColumnEnd(42)
            .setLineContent("this is some random message")
            .build()


        val message = DaemonMessageGrpc.newBuilder()
            .setCompilerMessageSeverity(CompilerMessageSeverityGrpc.INFO)
            .setMessage("RANDOM MESSAGE [#$i]: this is random message ")
            .setCompilerMessageSourceLocation(location)
            .build()

        emit(CompileResponseGrpc.newBuilder().setDaemonMessage(message).build())
        delay(900)
    }
}



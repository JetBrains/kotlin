/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.daemon.common

import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import java.io.File


interface CompileServiceAsync {

    suspend fun checkCompilerId(expectedCompilerId: CompilerId): Boolean

    suspend fun getUsedMemory(): CompileService.CallResult<Long>

    suspend fun getDaemonOptions(): CompileService.CallResult<DaemonOptions>

    suspend fun getDaemonInfo(): CompileService.CallResult<String>

    suspend fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions>

    suspend fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing>

    // TODO: (-old-) consider adding another client alive checking mechanism, e.g. socket/socketPort

    suspend fun getClients(): CompileService.CallResult<List<String>>

    suspend fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int>

    suspend fun releaseCompileSession(sessionId: Int): CompileService.CallResult<Nothing>

    suspend fun shutdown(): CompileService.CallResult<Nothing>

    suspend fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean>

    suspend fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        compilationResults: CompilationResultsAsync?
    ): CompileService.CallResult<Int>

    suspend fun clearJarCache()

    suspend fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing>

    suspend fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBaseAsync,
        templateClasspath: List<java.io.File>,
        templateClassName: String
    ): CompileService.CallResult<Int>

    suspend fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacadeAsync>

    suspend fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCheckResult>

    suspend fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine
    ): CompileService.CallResult<ReplCompileResult>

    suspend fun classesFqNamesByFiles(sessionId: Int, sourceFiles: Set<File>): CompileService.CallResult<Set<String>>

    val serverPort: Int
        get() = 0

}
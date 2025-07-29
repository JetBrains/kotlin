/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.common

import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.daemon.common.CompilationResults
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.CompilerServicesFacadeBase
import org.jetbrains.kotlin.daemon.common.DaemonJVMOptions
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.daemon.common.ReplStateFacade
import java.io.File

class RemoteCompilationService: CompileService {
    override fun checkCompilerId(expectedCompilerId: CompilerId): Boolean {
        TODO("Not yet implemented")
    }

    override fun getUsedMemory(withGC: Boolean): CompileService.CallResult<Long> {
        TODO("Not yet implemented")
    }

    override fun getDaemonOptions(): CompileService.CallResult<DaemonOptions> {
        TODO("Not yet implemented")
    }

    override fun getDaemonInfo(): CompileService.CallResult<String> {
        TODO("Not yet implemented")
    }

    override fun getKotlinVersion(): CompileService.CallResult<String> {
        TODO("Not yet implemented")
    }

    override fun getDaemonJVMOptions(): CompileService.CallResult<DaemonJVMOptions> {
        TODO("Not yet implemented")
    }

    override fun registerClient(aliveFlagPath: String?): CompileService.CallResult<Nothing> {
        TODO("Not yet implemented")
    }

    override fun getClients(): CompileService.CallResult<List<String>> {
        TODO("Not yet implemented")
    }

    override fun leaseCompileSession(aliveFlagPath: String?): CompileService.CallResult<Int> {
        TODO("Not yet implemented")
    }

    override fun releaseCompileSession(sessionId: Int): CompileService.CallResult<Nothing> {
        TODO("Not yet implemented")
    }

    override fun shutdown(): CompileService.CallResult<Nothing> {
        TODO("Not yet implemented")
    }

    override fun scheduleShutdown(graceful: Boolean): CompileService.CallResult<Boolean> {
        TODO("Not yet implemented")
    }

    override fun compile(
        sessionId: Int,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        compilationResults: CompilationResults?,
    ): CompileService.CallResult<Int> {
        TODO("Not yet implemented")
    }

    override fun classesFqNamesByFiles(
        sessionId: Int,
        sourceFiles: Set<File>,
    ): CompileService.CallResult<Set<String>> {
        TODO("Not yet implemented")
    }

    override fun clearJarCache() {
        TODO("Not yet implemented")
    }

    override fun releaseReplSession(sessionId: Int): CompileService.CallResult<Nothing> {
        TODO("Not yet implemented")
    }

    override fun leaseReplSession(
        aliveFlagPath: String?,
        compilerArguments: Array<out String>,
        compilationOptions: CompilationOptions,
        servicesFacade: CompilerServicesFacadeBase,
        templateClasspath: List<File>,
        templateClassName: String,
    ): CompileService.CallResult<Int> {
        TODO("Not yet implemented")
    }

    override fun replCreateState(sessionId: Int): CompileService.CallResult<ReplStateFacade> {
        TODO("Not yet implemented")
    }

    override fun replCheck(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine,
    ): CompileService.CallResult<ReplCheckResult> {
        TODO("Not yet implemented")
    }

    override fun replCompile(
        sessionId: Int,
        replStateId: Int,
        codeLine: ReplCodeLine,
    ): CompileService.CallResult<ReplCompileResult> {
        TODO("Not yet implemented")
    }

}
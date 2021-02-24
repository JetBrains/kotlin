/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client.experimental

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.common.experimental.findCallbackServerSocket
import org.jetbrains.kotlin.daemon.common.ReportCategory
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

// TODO: reduce number of ports used then SOCKET_ANY_FREE_PORT is passed (same problem with other calls)

open class KotlinRemoteReplCompilerClientAsync(
    protected val compileService: CompileServiceAsync,
    clientAliveFlagFile: File?,
    targetPlatform: CompileService.TargetPlatform,
    args: Array<out String>,
    messageCollector: MessageCollector,
    templateClasspath: List<File>,
    templateClassName: String
) : ReplCompiler {
    val services = BasicCompilerServicesWithResultsFacadeServerServerSide(
        messageCollector,
        null,
        findCallbackServerSocket()
    )

    val sessionId = runBlocking {
        compileService.leaseReplSession(
                clientAliveFlagFile?.absolutePath,
                args,
                CompilationOptions(
                        CompilerMode.NON_INCREMENTAL_COMPILER,
                        targetPlatform,
                        arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.DAEMON_MESSAGE.code, ReportCategory.EXCEPTION.code, ReportCategory.OUTPUT_MESSAGE.code),
                        ReportSeverity.INFO.code,
                        emptyArray()),
                services.clientSide,
                templateClasspath,
                templateClassName
        ).get()
    }

    // dispose should be called at the end of the repl lifetime to free daemon repl session and appropriate resources
    open fun dispose() {
        runBlocking {
            try {
                compileService.releaseReplSession(sessionId)
            } catch (ex: java.lang.Exception) {
                // assuming that communication failed and daemon most likely is already down
            }
        }
    }

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
            RemoteReplCompilerStateAsync(
                    runBlocking { compileService.replCreateState(sessionId).get() },
                    lock
            )

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult = runBlocking {
        compileService.replCheck(sessionId, state.asState(RemoteReplCompilerStateAsync::class.java).replStateFacade.getId(), codeLine).get()
    }

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult = runBlocking {
        compileService.replCompile(sessionId, state.asState(RemoteReplCompilerStateAsync::class.java).replStateFacade.getId(), codeLine).get()
    }

}

/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.common.*
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

// TODO: reduce number of ports used then SOCKET_ANY_FREE_PORT is passed (same problem with other calls)

open class KotlinRemoteReplCompilerClient(
        protected val compileService: CompileService,
        clientAliveFlagFile: File?,
        targetPlatform: CompileService.TargetPlatform,
        args: Array<out String>,
        messageCollector: MessageCollector,
        templateClasspath: List<File>,
        templateClassName: String,
        port: Int = SOCKET_ANY_FREE_PORT
) : ReplCompiler {
    val services = BasicCompilerServicesWithResultsFacadeServer(messageCollector, null, port)

    val sessionId = compileService.leaseReplSession(
            clientAliveFlagFile?.absolutePath,
            args,
            CompilationOptions(
                    CompilerMode.NON_INCREMENTAL_COMPILER,
                    targetPlatform,
                    arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.DAEMON_MESSAGE.code, ReportCategory.EXCEPTION.code, ReportCategory.OUTPUT_MESSAGE.code),
                    ReportSeverity.INFO.code,
                    emptyArray()),
            services,
            templateClasspath,
            templateClassName
    ).get()

    // dispose should be called at the end of the repl lifetime to free daemon repl session and appropriate resources
    open fun dispose() {
        try {
            compileService.releaseReplSession(sessionId)
        }
        catch (ex: java.rmi.RemoteException) {
            // assuming that communication failed and daemon most likely is already down
        }
    }

    override fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
            RemoteReplCompilerState(compileService.replCreateState(sessionId).get(), lock)

    override fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult =
            compileService.replCheck(sessionId, state.asState(RemoteReplCompilerState::class.java).replStateFacade.getId(), codeLine).get()

    override fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult =
            compileService.replCompile(sessionId, state.asState(RemoteReplCompilerState::class.java).replStateFacade.getId(), codeLine).get()
}

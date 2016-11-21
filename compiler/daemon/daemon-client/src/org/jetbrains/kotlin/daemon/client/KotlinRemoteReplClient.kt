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

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.repl.*
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.RemoteOperationsTracer
import org.jetbrains.kotlin.daemon.common.SOCKET_ANY_FREE_PORT
import java.io.File
import java.io.InputStream
import java.io.OutputStream

// TODO: reduce number of ports used then SOCKET_ANY_FREE_PORT is passed (same problem with other calls)

open class KotlinRemoteReplClientBase(
        disposable: Disposable,
        protected val compileService: CompileService,
        clientAliveFlagFile: File?,
        targetPlatform: CompileService.TargetPlatform,
        templateClasspath: List<File>,
        templateClassName: String,
        scriptArgs: Array<Any?>? = null,
        scriptArgsTypes: Array<Class<*>>? = null,
        compilerMessagesOutputStream: OutputStream = System.err,
        evalOutputStream: OutputStream? = null,
        evalErrorStream: OutputStream? = null,
        evalInputStream: InputStream? = null,
        port: Int = SOCKET_ANY_FREE_PORT,
        operationsTracer: RemoteOperationsTracer? = null
) : ReplChecker {

    val sessionId = compileService.leaseReplSession(
            clientAliveFlagFile?.absolutePath,
            targetPlatform,
            CompilerCallbackServicesFacadeServer(port = port),
            templateClasspath,
            templateClassName,
            scriptArgs,
            scriptArgsTypes,
            RemoteOutputStreamServer(compilerMessagesOutputStream, port),
            evalOutputStream?.let { RemoteOutputStreamServer(it, port) },
            evalErrorStream?.let { RemoteOutputStreamServer(it, port) },
            evalInputStream?.let { RemoteInputStreamServer(it, port) },
            operationsTracer
    ).get()

    init {
        Disposer.register(disposable, Disposable {
            try {
                compileService.releaseReplSession(sessionId)
            }
            catch (ex: java.rmi.RemoteException) {
                // assuming that communication failed and daemon most likely is already down
            }
        })
    }

    override fun check(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplCheckResult {
        return compileService.remoteReplLineCheck(sessionId, codeLine, history).get()
    }
}

class KotlinRemoteReplCompiler(
        disposable: Disposable,
        compileService: CompileService,
        clientAliveFlagFile: File?,
        targetPlatform: CompileService.TargetPlatform,
        templateClasspath: List<File>,
        templateClassName: String,
        compilerMessagesOutputStream: OutputStream,
        port: Int = SOCKET_ANY_FREE_PORT,
        operationsTracer: RemoteOperationsTracer? = null
) : KotlinRemoteReplClientBase(
        disposable = disposable,
        compileService = compileService,
        clientAliveFlagFile = clientAliveFlagFile,
        targetPlatform = targetPlatform,
        templateClasspath = templateClasspath,
        templateClassName = templateClassName,
        compilerMessagesOutputStream = compilerMessagesOutputStream,
        evalOutputStream = null,
        evalErrorStream = null,
        evalInputStream = null,
        port = port,
        operationsTracer = operationsTracer
), ReplCompiler {

    override fun compile(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplCompileResult {
        return compileService.remoteReplLineCompile(sessionId, codeLine, history).get()
    }
}

class KotlinRemoteReplEvaluator(
        disposable: Disposable,
        compileService: CompileService,
        clientAliveFlagFile: File?,
        targetPlatform: CompileService.TargetPlatform,
        templateClasspath: List<File>,
        templateClassName: String,
        scriptArgs: Array<Any?>?,
        scriptArgsTypes: Array<Class<*>>?,
        compilerMessagesOutputStream: OutputStream,
        evalOutputStream: OutputStream?,
        evalErrorStream: OutputStream?,
        evalInputStream: InputStream?,
        port: Int = SOCKET_ANY_FREE_PORT,
        operationsTracer: RemoteOperationsTracer? = null
) : KotlinRemoteReplClientBase(
        disposable = disposable,
        compileService = compileService,
        clientAliveFlagFile = clientAliveFlagFile,
        targetPlatform = targetPlatform,
        templateClasspath = templateClasspath,
        templateClassName = templateClassName,
        scriptArgs = scriptArgs,
        scriptArgsTypes = scriptArgsTypes,
        compilerMessagesOutputStream = compilerMessagesOutputStream,
        evalOutputStream = evalOutputStream,
        evalErrorStream = evalErrorStream,
        evalInputStream = evalInputStream,
        port = port,
        operationsTracer = operationsTracer
), ReplEvaluator {

    override fun eval(codeLine: ReplCodeLine, history: List<ReplCodeLine>): ReplEvalResult {
        return compileService.remoteReplLineEval(sessionId, codeLine, history).get()
    }
}

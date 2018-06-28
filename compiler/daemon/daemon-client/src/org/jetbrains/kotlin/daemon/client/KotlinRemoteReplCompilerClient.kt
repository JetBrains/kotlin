/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.repl.IReplStageState
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.cli.common.repl.experimental.ReplCompiler
import org.jetbrains.kotlin.daemon.client.impls.KotlinRemoteReplCompilerClientImpl
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompileServiceAsync
import org.jetbrains.kotlin.daemon.common.impls.SOCKET_ANY_FREE_PORT
import org.jetbrains.kotlin.daemon.common.toRMI
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock

interface KotlinRemoteReplCompilerClient : ReplCompiler {

    val sessionId: Int

    // dispose should be called at the end of the repl lifetime to free daemon repl session and appropriate resources
    suspend fun dispose()

    override suspend fun createState(lock: ReentrantReadWriteLock): IReplStageState<*>

    override suspend fun check(
        state: IReplStageState<*>,
        codeLine: ReplCodeLine
    ): ReplCheckResult

    override suspend fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult

    companion object {
        fun instantiate(
            compileService: CompileServiceAsync,
            clientAliveFlagFile: File?,
            targetPlatform: CompileService.TargetPlatform,
            args: Array<out String>,
            messageCollector: MessageCollector,
            templateClasspath: List<File>,
            templateClassName: String,
            port: Int = SOCKET_ANY_FREE_PORT
        ): KotlinRemoteReplCompilerClient =
            when (compileService::class.java.simpleName) {
                "CompileServiceClientSideImpl" ->
                    ClassLoader
                        .getSystemClassLoader()
                        .loadClass("org.jetbrains.kotlin.daemon.client.experimental.KotlinRemoteReplCompilerClientAsync")
                        .getDeclaredConstructor(
                            CompileServiceAsync::class.java,
                            File::class.java,
                            CompileService.TargetPlatform::class.java,
                            Array<out String>::class.java,
                            MessageCollector::class.java,
                            List::class.java,
                            String::class.java,
                            Int::class.java
                        )
                        .newInstance(
                            compileService,
                            clientAliveFlagFile,
                            targetPlatform,
                            args,
                            messageCollector,
                            templateClasspath,
                            templateClassName,
                            port
                        ) as KotlinRemoteReplCompilerClient
                else -> KotlinRemoteReplCompilerWrapper(
                    KotlinRemoteReplCompilerClientImpl(
                        compileService.toRMI(),
                        clientAliveFlagFile,
                        targetPlatform,
                        args,
                        messageCollector,
                        templateClasspath,
                        templateClassName
                    )
                )
            }
    }
}
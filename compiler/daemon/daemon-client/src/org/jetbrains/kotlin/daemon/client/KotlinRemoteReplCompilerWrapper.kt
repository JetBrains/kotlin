/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.cli.common.repl.IReplStageState
import org.jetbrains.kotlin.cli.common.repl.ReplCheckResult
import org.jetbrains.kotlin.cli.common.repl.ReplCodeLine
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.jetbrains.kotlin.daemon.client.impls.KotlinRemoteReplCompilerClientImpl
import java.util.concurrent.locks.ReentrantReadWriteLock

class KotlinRemoteReplCompilerWrapper(
    val oldReplCompiler: KotlinRemoteReplCompilerClientImpl
) : KotlinRemoteReplCompilerClient {
    override suspend fun dispose() = oldReplCompiler.dispose()

    override suspend fun createState(lock: ReentrantReadWriteLock): IReplStageState<*> =
        oldReplCompiler.createState(lock)

    override suspend fun check(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCheckResult =
        oldReplCompiler.check(state, codeLine)

    override suspend fun compile(state: IReplStageState<*>, codeLine: ReplCodeLine): ReplCompileResult =
        oldReplCompiler.compile(state, codeLine)

    override val sessionId: Int = oldReplCompiler.sessionId
}
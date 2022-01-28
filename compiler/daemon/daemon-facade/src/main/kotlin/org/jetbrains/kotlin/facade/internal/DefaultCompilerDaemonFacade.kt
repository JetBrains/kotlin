/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.facade.internal

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.client.DaemonSettings
import org.jetbrains.kotlin.daemon.client.KotlinDaemonClient
import org.jetbrains.kotlin.daemon.client.ProtocolSettings
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.facade.CompilerDaemonFacade
import org.jetbrains.kotlin.facade.OutOfProcessCompilationSettings

internal class DefaultCompilerDaemonFacade : CompilerDaemonFacade {
    override fun <T : ProtocolSettings> getDaemonClient(settings: DaemonSettings<T>): KotlinDaemonClient {
        TODO("Not yet implemented")
    }

    override fun <T : ProtocolSettings> compileWithDaemon(
        settings: DaemonSettings<T>,
        options: CompilationOptions,
        compilerArguments: List<String>
    ): ExitCode = getDaemonClient(settings).compile(options, compilerArguments)

    override fun compileInProcess(options: CompilationOptions, compilerArguments: List<String>): ExitCode {
        TODO("Not yet implemented")
    }

    override fun compileOutOfProcess(
        settings: OutOfProcessCompilationSettings,
        options: CompilationOptions,
        compilerArguments: List<String>
    ): ExitCode {
        TODO("Not yet implemented")
    }

}
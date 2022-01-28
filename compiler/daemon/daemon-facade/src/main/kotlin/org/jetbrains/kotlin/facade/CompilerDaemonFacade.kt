/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.facade

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.daemon.client.DaemonSettings
import org.jetbrains.kotlin.daemon.client.KotlinDaemonClient
import org.jetbrains.kotlin.daemon.client.ProtocolSettings
import org.jetbrains.kotlin.daemon.common.CompilationOptions
import org.jetbrains.kotlin.facade.internal.DefaultCompilerDaemonFacade
import java.io.File

// TODO: think about progress reporting, metrics reporting, JPS integration problems?
interface CompilerDaemonFacade {
    fun <T : ProtocolSettings> getDaemonClient(
        settings: DaemonSettings<T>
    ): KotlinDaemonClient

    // TODO: is this method needed?
    /**
     * Compile using Gradle daemon
     */
    fun <T : ProtocolSettings> compileWithDaemon(
        settings: DaemonSettings<T>,
        options: CompilationOptions,
        compilerArguments: List<String>,
    ): ExitCode

    /**
     * Compile invoking Kotlin compiler in the same process
     */
    fun compileInProcess(
        options: CompilationOptions,
        compilerArguments: List<String>,
    ): ExitCode

    /**
     * Compile invoking Kotlin compiler in a separate process (each compilation runs inside separate process)
     */
    fun compileOutOfProcess(
        settings: OutOfProcessCompilationSettings,
        options: CompilationOptions,
        compilerArguments: List<String>
    ): ExitCode
}

class OutOfProcessCompilationSettings(
    val compilerClasspath: List<File>,
    val jvmArgs: List<String>,
)

fun getCompilerDaemonFacade(): CompilerDaemonFacade = DefaultCompilerDaemonFacade()
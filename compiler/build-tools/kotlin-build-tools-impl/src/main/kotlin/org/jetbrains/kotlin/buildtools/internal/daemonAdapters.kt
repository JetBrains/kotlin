/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.build.report.metrics.BuildMetrics
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.daemon.common.*
import java.io.Serializable
import java.rmi.server.UnicastRemoteObject

internal val JvmCompilationConfigurationImpl.asDaemonCompilationOptions: CompilationOptions
    get() {
        val ktsExtensionsAsArray = if (kotlinScriptFilenameExtensions.isEmpty()) null else kotlinScriptFilenameExtensions.toTypedArray()
        val reportCategories = arrayOf(ReportCategory.COMPILER_MESSAGE.code, ReportCategory.IC_MESSAGE.code) // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val reportSeverity = ReportSeverity.INFO.code // TODO: automagically compute the value, related to BasicCompilerServicesWithResultsFacadeServer
        val requestedCompilationResults = emptyArray<Int>() // TODO: automagically compute the value, related to DaemonCompilationResults
        return when (aggregatedIcConfiguration?.options) {
            is ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration -> TODO("Incremental compilation within the daemon is not yet supported")
            else -> CompilationOptions(
                compilerMode = CompilerMode.NON_INCREMENTAL_COMPILER,
                targetPlatform = CompileService.TargetPlatform.JVM,
                reportCategories = reportCategories,
                reportSeverity = reportSeverity,
                requestedCompilationResults = requestedCompilationResults,
                kotlinScriptExtensions = ktsExtensionsAsArray,
            )
        }
    }

internal class DaemonCompilationResults : CompilationResults,
    UnicastRemoteObject(
        SOCKET_ANY_FREE_PORT,
        LoopbackNetworkInterface.clientLoopbackSocketFactory,
        LoopbackNetworkInterface.serverLoopbackSocketFactory
    ) {
    /**
     * Possible combinations:
     * 1. [CompilationResultCategory.IC_COMPILE_ITERATION.code]       -> a [CompileIterationResult] instance
     * 2. [CompilationResultCategory.BUILD_REPORT_LINES.code]         -> a [List] of [String]
     * 3. [CompilationResultCategory.VERBOSE_BUILD_REPORT_LINES.code] -> a [List] of [String]
     * 4. [CompilationResultCategory.BUILD_METRICS.code]              -> a [BuildMetrics] instance
     **/
    override fun add(compilationResultCategory: Int, value: Serializable) {
        // TODO propagate the values to the caller via callbacks, requires to make metrics a part of the API
        println("Result category=$compilationResultCategory value=$value")
    }
}

internal val clientIsAliveFile by lazy {
    makeAutodeletingFlagFile()
}

internal fun createSessionIsAliveFlagFile() = makeAutodeletingFlagFile(keyword = "compilation-session")
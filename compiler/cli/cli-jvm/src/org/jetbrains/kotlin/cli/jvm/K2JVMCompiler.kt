/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmCliPipeline.Companion.createCustomPerformanceManagerOrNull
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager

class K2JVMCompiler : CLICompiler<K2JVMCompilerArguments>() {
    override val platform: TargetPlatform
        get() = JvmPlatforms.defaultJvmPlatform

    override fun doExecutePhased(
        arguments: K2JVMCompilerArguments,
        services: Services,
        basicMessageCollector: MessageCollector,
    ): ExitCode {
        return JvmCliPipeline(defaultPerformanceManager).execute(arguments, services, basicMessageCollector)
    }

    override fun createArguments(): K2JVMCompilerArguments = K2JVMCompilerArguments().apply {
        if (System.getenv("KOTLIN_REPORT_PERF") != null) {
            reportPerf = true
        }
    }

    override fun executableScriptFileName(): String = "kotlinc-jvm"

    override fun createMetadataVersion(versionArray: IntArray): BinaryVersion = MetadataVersion(*versionArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            doMain(K2JVMCompiler(), args)
        }
    }

    override fun createPerformanceManager(arguments: K2JVMCompilerArguments, services: Services): PerformanceManager {
        return createCustomPerformanceManagerOrNull(arguments, services) ?: defaultPerformanceManager
    }
}

fun main(args: Array<String>) = K2JVMCompiler.main(args)

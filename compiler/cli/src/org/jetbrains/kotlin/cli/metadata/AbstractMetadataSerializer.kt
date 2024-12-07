/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.metadata

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import java.io.File

abstract class AbstractMetadataSerializer<T>(
    val configuration: CompilerConfiguration,
    val environment: KotlinCoreEnvironment,
    definedMetadataVersion: BuiltInsBinaryVersion? = null
) {
    protected val metadataVersion =
        definedMetadataVersion ?: configuration.get(CommonConfigurationKeys.METADATA_VERSION) as? BuiltInsBinaryVersion
        ?: BuiltInsBinaryVersion.INSTANCE

    fun analyzeAndSerialize(): OutputInfo? {
        val destDir = environment.destDir
        if (destDir == null) {
            val configuration = environment.configuration
            val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
            messageCollector.report(CompilerMessageSeverity.ERROR, "Specify destination via -d")
            return null
        }

        val analysisResult = analyze() ?: return null

        val performanceManager = environment.configuration.getNotNull(CLIConfigurationKeys.PERF_MANAGER)
        performanceManager.notifyGenerationStarted()
        return serialize(analysisResult, destDir).also {
            performanceManager.notifyGenerationFinished()
        }
    }

    protected abstract fun analyze(): T?

    /**
     * @return number of written bytes and files
     * The return value is optional and might be omitted in implementations
     */
    protected abstract fun serialize(analysisResult: T, destDir: File): OutputInfo?

    data class OutputInfo(val totalSize: Int, val totalFiles: Int)
}
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.cli.jvm.config.configureJdkClasspathRoots
import org.jetbrains.kotlin.cli.metadata.AbstractMetadataSerializer.OutputInfo
import org.jetbrains.kotlin.cli.pipeline.ConfigurationPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataBuiltinsSerializerPhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelinePhase
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceManager
import java.io.File

object BuiltInsSerializer {
    fun analyzeAndSerialize(
        destDir: File,
        srcDirs: List<File>,
        extraClassPath: List<File>,
        dependOnOldBuiltIns: Boolean,
        useK2: Boolean,
        onComplete: (totalSize: Int, totalFiles: Int) -> Unit
    ) {
        val rootDisposable = Disposer.newDisposable("Disposable for ${K1BuiltInsSerializer::class.simpleName}.analyzeAndSerialize")
        val messageCollector = createMessageCollector()
        val performanceManager = object : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "test") {}
        try {
            val configuration = CompilerConfiguration().apply {
                this.messageCollector = messageCollector

                addKotlinSourceRoots(srcDirs.map { it.path })
                addJvmClasspathRoots(extraClassPath)
                configureJdkClasspathRoots()

                put(CLIConfigurationKeys.METADATA_DESTINATION_DIRECTORY, destDir)
                put(CommonConfigurationKeys.MODULE_NAME, "module for built-ins serialization")
                put(CLIConfigurationKeys.PERF_MANAGER, performanceManager)
                put(CommonConfigurationKeys.USE_LIGHT_TREE, true)
                put(
                    CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                    LanguageVersionSettingsImpl(
                        LanguageVersion.LATEST_STABLE,
                        ApiVersion.LATEST_STABLE,
                        analysisFlags = mapOf(
                            AnalysisFlags.stdlibCompilation to !dependOnOldBuiltIns,
                            AnalysisFlags.allowKotlinPackage to true
                        )
                    )
                )
            }

            val environment = KotlinCoreEnvironment.Companion.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)

            val (totalSize, totalFiles) = when (useK2) {
                false -> {
                    val serializer = K1BuiltInsSerializer(configuration, environment, dependOnOldBuiltIns)
                    serializer.analyzeAndSerialize()
                }
                true -> {
                    val diagnosticCollector = DiagnosticReporterFactory.createPendingReporter(messageCollector)
                    val input = ConfigurationPipelineArtifact(
                        configuration,
                        diagnosticCollector,
                        rootDisposable
                    )
                    val frontendOutput = MetadataFrontendPipelinePhase.executePhase(input)
                    val metadataOutput = MetadataBuiltinsSerializerPhase.executePhase(frontendOutput)
                    metadataOutput.outputInfo
                }
            } ?: OutputInfo(totalSize = 0, totalFiles = 0)

            onComplete(totalSize, totalFiles)
        } finally {
            messageCollector.flush()
            Disposer.dispose(rootDisposable)
        }
    }

    private fun createMessageCollector() = object : GroupingMessageCollector(
        PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, false),
        /* treatWarningsAsErrors = */ false,
        /* reportAllWarnings = */ false,
    ) {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            // Only report diagnostics without a particular location because there's plenty of errors in built-in sources
            // (functions without bodies, incorrect combination of modifiers, etc.)
            if (location == null) {
                super.report(severity, message, location)
            }
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.buildtools.api.arguments.CompilerPlugin
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension

private val sandboxPlugin: CompilerPlugin
    get() = CompilerPlugin(
        pluginId = "org.jetbrains.kotlin.plugin.sandbox",
        classpath = System.getProperty("PLUGIN_SANDBOX").split(File.pathSeparator).map { Paths.get(it) },
        rawArguments = emptyList(),
        orderingRequirements = emptySet(),
    )

@DisplayName("The JPS file mapping tracker")
class FileMappingTrackerJpsTest : BaseJpsTest() {

    @DisplayName("Every produced class file is reported together with the sources it was generated from")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-3")
    fun outputsAreMappedToSources(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val fixture = "basic-multimodule-project/module-3"
        jvmProject(strategyConfig) {
            val module = module(fixture)
            val fileMappingTracker = RecordingFileMappingTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] = fileMappingTracker
                }
            }) {
                val classMappings = fileMappingTracker.sourcesToOutput
                    .filter { [_, output] -> output.extension == "class" }
                    .associate { [sources, output] ->
                        output.relativeToModule(fixture) to sources.map { it.relativeToModule(fixture) }.sorted()
                    }

                assertEquals(
                    mapOf(
                        "/build/output/p/FooKt.class" to listOf("/src/foo.kt"),
                        "/build/output/p2/BooKt.class" to listOf("/src/boo.kt"),
                        "/build/output/p3/BarKt.class" to listOf("/src/bar.kt"),
                    ),
                    classMappings,
                )

                // the module mapping is also reported; its source list is an implementation detail
                assertTrue(
                    fileMappingTracker.sourcesToOutput.any { [_, output] -> output.extension == "kotlin_module" }
                ) { "The .kotlin_module output was not reported" }

                // no compiler plugins take part in this compilation
                assertTrue(fileMappingTracker.pluginReferencedSources.isEmpty()) {
                    "unexpected plugin-referenced sources: ${fileMappingTracker.pluginReferencedSources}"
                }
                assertTrue(fileMappingTracker.pluginGeneratedOutputs.isEmpty()) {
                    "unexpected plugin-generated outputs: ${fileMappingTracker.pluginGeneratedOutputs}"
                }
                assertTrue(fileMappingTracker.pluginGeneratedSources.isEmpty()) {
                    "unexpected plugin-generated sources: ${fileMappingTracker.pluginGeneratedSources}"
                }
            }
        }
    }

    @DisplayName("A compiler plugin that generates top-level declarations reports the sources it read and what it generated")
    @BtaV2StrategyAgnosticCompilationTest
    @TestMetadata("sandbox-plugin")
    fun pluginEventsAreReported(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val fixture = "sandbox-plugin"
        jvmProject(strategyConfig) {
            val module = module(fixture)
            val fileMappingTracker = RecordingFileMappingTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.compilerArguments[CommonCompilerArguments.COMPILER_PLUGINS] = listOf(sandboxPlugin)
                builder.withJpsIc {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] = fileMappingTracker
                    this[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = RecordingLookupTracker()
                }
            }) {
                // The plugin looks up the classes annotated with @DummyFunction, all of them in main.kt
                assertEquals(
                    setOf("/src/main.kt"),
                    fileMappingTracker.pluginReferencedSources.map { it.relativeToModule(fixture) }.toSet(),
                )

                // It generates a top-level function for each of them, in files that are not on disk
                assertTrue(fileMappingTracker.pluginGeneratedSources.isNotEmpty()) { "No plugin-generated sources were reported" }
                assertTrue(fileMappingTracker.pluginGeneratedSources.none { it.exists() }) {
                    "Plugin-generated sources must not be on disk: ${fileMappingTracker.pluginGeneratedSources}"
                }

                // The outputs built from these files are written; the classes compiled from main.kt alone are not reported
                assertTrue(fileMappingTracker.pluginGeneratedOutputs.isNotEmpty()) { "No plugin-generated outputs were reported" }
                assertTrue(fileMappingTracker.pluginGeneratedOutputs.all { it.exists() }) {
                    "Plugin-generated outputs must be written: ${fileMappingTracker.pluginGeneratedOutputs}"
                }

                val classesOfMainKt = setOf("/build/output/foo/A.class", "/build/output/foo/B.class", "/build/output/foo/MainKt.class")
                assertTrue(fileMappingTracker.pluginGeneratedOutputs.none { it.relativeToModule(fixture) in classesOfMainKt }) {
                    "Outputs of main.kt alone must not be reported as plugin-generated: ${fileMappingTracker.pluginGeneratedOutputs}"
                }
            }
        }
    }
}

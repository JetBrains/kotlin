/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("The JPS file mapping tracker")
class FileMappingTrackerJpsTest : BaseJpsTest() {

    @DisplayName("Every produced class file is reported together with the sources it was generated from")
    @TestMetadata("basic-multimodule-project/module-3")
    @Test
    fun outputsAreMappedToSources() {
        val fixture = "basic-multimodule-project/module-3"
        jvmProject(inProcess) {
            val module = module(fixture)
            val fileMappingTracker = RecordingFileMappingTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc {
                    this[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] = fileMappingTracker
                }
            }) {
                val classMappings = fileMappingTracker.sourcesToOutput
                    .filter { [_, output] -> output.endsWith(".class") }
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
                    fileMappingTracker.sourcesToOutput.any { [_, output] -> output.endsWith(".kotlin_module") }
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
}

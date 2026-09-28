/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jps.jvm.operations.jpsManagedIcConfigurationBuilder
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain.Companion.jvm
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertExactOutput
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import java.nio.file.Paths

@DisplayName("The JPS-managed incremental compilation configuration")
class JpsIcConfigurationTest : BaseJpsTest() {

    @DisplayName("The JPS IC configuration can be created, configured and set as INCREMENTAL_COMPILATION")
    @Test
    fun configurationRoundTrips() {
        val operation = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
        val components = SingleCacheComponents()
        val config = operation.jpsManagedIcConfigurationBuilder(components).build()

        assertSame(components, config.incrementalCompilationComponents)
        operation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = config
        assertSame(config, operation[JvmCompilationOperation.INCREMENTAL_COMPILATION])

        val built = operation.build()[JvmCompilationOperation.INCREMENTAL_COMPILATION]
        assertSame(
            components,
            (built as JvmJpsManagedIncrementalCompilationConfiguration).incrementalCompilationComponents,
        )
    }

    @DisplayName("Every JPS IC option round-trips through the implementation by its id")
    @Test
    fun allOptionsRoundTrip() {
        val builder = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
            .jpsManagedIcConfigurationBuilder(SingleCacheComponents())

        val lookup = RecordingLookupTracker()
        val mapping = RecordingFileMappingTracker()
        val enumWhen = RecordingEnumWhenTracker()
        val import = RecordingImportTracker()
        val const = RecordingInlineConstTracker()
        val expectActual = RecordingExpectActualTracker()

        builder[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = lookup
        builder[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER] = mapping
        builder[JvmJpsManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER] = enumWhen
        builder[JvmJpsManagedIncrementalCompilationConfiguration.IMPORT_TRACKER] = import
        builder[JvmJpsManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER] = const
        builder[JvmJpsManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER] = expectActual

        // readable back from the builder ...
        assertSame(lookup, builder[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER])
        assertSame(mapping, builder[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER])
        assertSame(enumWhen, builder[JvmJpsManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER])
        assertSame(import, builder[JvmJpsManagedIncrementalCompilationConfiguration.IMPORT_TRACKER])
        assertSame(const, builder[JvmJpsManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER])
        assertSame(expectActual, builder[JvmJpsManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER])

        // ... and from the built, immutable configuration
        val built = builder.build()
        assertSame(lookup, built[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER])
        assertSame(mapping, built[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER])
        assertSame(enumWhen, built[JvmJpsManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER])
        assertSame(import, built[JvmJpsManagedIncrementalCompilationConfiguration.IMPORT_TRACKER])
        assertSame(const, built[JvmJpsManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER])
        assertSame(expectActual, built[JvmJpsManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER])

        // defaults are null before anything is set
        val fresh = toolchain.jvm.jvmCompilationOperationBuilder(emptyList(), Paths.get(""))
            .jpsManagedIcConfigurationBuilder(SingleCacheComponents()).build()
        assertNull(fresh[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER])
        assertNull(fresh[JvmJpsManagedIncrementalCompilationConfiguration.FILE_MAPPING_TRACKER])
        assertNull(fresh[JvmJpsManagedIncrementalCompilationConfiguration.ENUM_WHEN_TRACKER])
        assertNull(fresh[JvmJpsManagedIncrementalCompilationConfiguration.IMPORT_TRACKER])
        assertNull(fresh[JvmJpsManagedIncrementalCompilationConfiguration.INLINE_CONST_TRACKER])
        assertNull(fresh[JvmJpsManagedIncrementalCompilationConfiguration.EXPECT_ACTUAL_TRACKER])
    }

    @DisplayName("A JPS-managed compilation succeeds and produces runnable output")
    @TestMetadata("kotlin-java-mixed")
    @Test
    fun endToEndCompilation() {
        jvmProject(inProcess) {
            val module = module("kotlin-java-mixed", compileJavaSources = true)
            module.compile(compilationConfigAction = { it.withJpsIc() }) {
                // `assertOutputs` is exact, not containment. `compileJavaSources = true` makes javac emit
                // apkg/AClass.class into the same output directory, so it has to be declared here.
                // META-INF/<module.moduleName>.kotlin_module is added automatically.
                assertOutputs("bpkg/MainKt.class", "bpkg/BClass.class", "apkg/AClass.class")
            }
            module.executeCompiledClass("bpkg.MainKt") { assertExactOutput("Output: <XYZ>") }
        }
    }

    @DisplayName("The JPS IC configuration is rejected with the daemon execution policy")
    @TestMetadata("basic-multimodule-project/module-1")
    @Test
    fun rejectedWithDaemon() {
        jvmProject(toolchain, daemon) {
            val module = module("basic-multimodule-project/module-1")
            module.compileAndThrow(compilationConfigAction = { it.withJpsIc() }) { e ->
                assertTrue(e is IllegalStateException) { "Unexpected exception type: ${e::class}" }
                assertTrue(
                    e.message?.contains(
                        "JvmJpsManagedIncrementalCompilationConfiguration is not supported with the daemon execution policy"
                    ) == true
                ) { "Unexpected message: ${e.message}" }
            }
        }
    }

    @DisplayName("The operation-level lookup tracker still fires with no incremental compilation configured")
    @TestMetadata("basic-multimodule-project/module-1")
    @Test
    fun operationLookupTrackerWithoutIcConfig() {
        jvmProject(inProcess) {
            val module = module("basic-multimodule-project/module-1")
            val lookupTracker = RecordingLookupTracker()
            module.compile(compilationConfigAction = { builder ->
                builder[BaseCompilationOperation.LOOKUP_TRACKER] = lookupTracker
                // deliberately NO INCREMENTAL_COMPILATION
            }) {
                assertTrue(lookupTracker.lookups.isNotEmpty()) {
                    "The operation-level lookup tracker must still fire when no IC configuration is set"
                }
            }
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(InternalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.tests.compilation.jps

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.JvmJpsManagedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("The JPS lookup tracker")
class LookupTrackerJpsTest : BaseJpsTest() {

    @DisplayName("The JPS lookup tracker receives lookups")
    @TestMetadata("basic-multimodule-project/module-1")
    @Test
    fun lookupsAreReported() {
        jvmProject(inProcess) {
            val module = module("basic-multimodule-project/module-1")
            val lookupTracker = RecordingLookupTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc { this[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = lookupTracker }
            }) {
                assertTrue(lookupTracker.lookups.isNotEmpty()) { "JPS lookup tracker didn't produce any output" }
            }
        }
    }

    @DisplayName("The JPS lookup tracker takes precedence over the operation-level one, with a warning")
    @TestMetadata("basic-multimodule-project/module-1")
    @Test
    fun jpsLookupTrackerTakesPrecedence() {
        jvmProject(inProcess) {
            val module = module("basic-multimodule-project/module-1")
            val jpsTracker = RecordingLookupTracker()
            val operationTracker = RecordingLookupTracker()
            module.compile(compilationConfigAction = { builder ->
                builder[BaseCompilationOperation.LOOKUP_TRACKER] = operationTracker
                builder.withJpsIc { this[JvmJpsManagedIncrementalCompilationConfiguration.LOOKUP_TRACKER] = jpsTracker }
            }) {
                assertTrue(jpsTracker.lookups.isNotEmpty()) { "JPS lookup tracker didn't produce any output" }
                assertTrue(operationTracker.lookups.isEmpty()) {
                    "The operation-level lookup tracker must not receive anything when the JPS one is set"
                }
                assertLogContainsPatterns(
                    LogLevel.WARN,
                    Regex(".*A lookup tracker is set both as BaseCompilationOperation\\.LOOKUP_TRACKER and as.*"),
                )
            }
        }
    }
}

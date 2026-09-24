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

@DisplayName("The JPS import tracker")
class ImportTrackerJpsTest : BaseJpsTest() {

    @DisplayName("The import tracker reports every resolved import directive")
    @TestMetadata("basic-multimodule-project/module-3")
    @Test
    fun importsAreReported() {
        val fixture = "basic-multimodule-project/module-3"
        jvmProject(inProcess) {
            val module = module(fixture)
            val importTracker = RecordingImportTracker()
            module.compile(compilationConfigAction = { builder ->
                builder.withJpsIc { this[JvmJpsManagedIncrementalCompilationConfiguration.IMPORT_TRACKER] = importTracker }
            }) {
                val actual = importTracker.reports
                    .map { [path, fqName] -> path.relativeToModule(fixture) to fqName }
                    .sortedBy { it.first }
                assertEquals(
                    listOf(
                        "/src/bar.kt" to "p.foo",
                        "/src/foo.kt" to "p2.*",
                    ),
                    actual,
                ) { "Import tracker didn't produce the expected output" }
            }
        }
    }
}

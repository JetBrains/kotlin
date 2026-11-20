/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.forward.tests

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.snapshotBasedIcConfiguration
import org.jetbrains.kotlin.buildtools.forward.tests.SmokeCompilationTest.Companion.jvmNonIncrementalCompilationOperation
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.exists
import kotlin.io.path.writeText

@DisplayName("The compiler reference index with an older API")
class CompilerRefIndexForwardCompatibilityTest : BaseCompilationTest() {
    @DisplayName("GENERATE_COMPILER_REF_INDEX still writes the index to the IC working directory")
    @DefaultForwardCompatibilityCompilationTest
    fun testGenerateCompilerRefIndex(strategyConfig: CompilerExecutionStrategyConfiguration) {
        val sources = listOf(workingDirectory.resolve("a.kt").apply { writeText("fun a() = 42") })
        val icWorkingDirectory = workingDirectory.resolve("ic-cache")
        val [toolchain, executionPolicy] = strategyConfig

        toolchain.createBuildSession().use { session ->
            val compilation = jvmNonIncrementalCompilationOperation(sources, workingDirectory.resolve("classes")) {
                this[JvmCompilationOperation.INCREMENTAL_COMPILATION] =
                    snapshotBasedIcConfiguration(icWorkingDirectory, SourcesChanges.Unknown, emptyList()) {
                        this[BaseIncrementalCompilationConfiguration.ROOT_PROJECT_DIR] = workingDirectory
                    }
                this[BaseCompilationOperation.GENERATE_COMPILER_REF_INDEX] = true
            }
            session.executeOperation(compilation, executionPolicy)
        }

        assertTrue(icWorkingDirectory.resolve(CriToolchain.DATA_PATH).resolve(CriToolchain.LOOKUPS_FILENAME).exists())
    }
}

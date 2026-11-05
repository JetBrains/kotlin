/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.moveTo
import kotlin.io.path.readText
import kotlin.io.path.writeText

@DisplayName("Incremental compilation of a module whose sources were moved to another directory")
class RelocatedSourcesTest : BaseCompilationTest() {
    @DisplayName("KT-90779: relocated sources dirtied by a classpath change are recompiled")
    @DefaultStrategyAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project")
    fun testRelocatedSourcesWithClasspathChange(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module1 = module("basic-multimodule-project/module-1")
            val module2 = module("basic-multimodule-project/module-2", listOf(module1))

            module1.compileIncrementally(SourcesChanges.Unknown)
            module2.compileIncrementally(SourcesChanges.Unknown) {
                assertOutputs("AKt.class", "BKt.class")
            }

            // an ABI change in the dependency that impacts `b.kt` of the module 2
            val barKt = module1.sourcesDirectory.resolve("bar.kt")
            barKt.writeText(barKt.readText().replace("bar()", "bar(i: Int = 1)"))
            module1.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = listOf(barKt.toFile()), removedFiles = emptyList()),
            )

            // the sources are moved as is, while the build directory with the IC caches stays in place
            val relocatedSourcesDirectory = module2.moduleDirectory.resolve("src-relocated")
            module2.sourcesDirectory.moveTo(relocatedSourcesDirectory)
            module2.sourcesDirectory = relocatedSourcesDirectory

            // build systems tracking sources by paths relative to the source directory root (e.g. Gradle) report no source changes in this case
            module2.compileIncrementally(
                SourcesChanges.Known(modifiedFiles = emptyList(), removedFiles = emptyList()),
            ) {
                // TODO: KT-90779 the output of `b.kt` is removed by its stale path in the IC caches, and nothing is recompiled.
                //  When fixed, the assertions should be `assertOutputs("AKt.class", "BKt.class")` and `assertCompiledSources("b.kt")`
                //  (or all the sources of the module if the fix is a fallback to non-incremental compilation)
                assertOutputs("AKt.class")
                assertNoCompiledSources()
            }
        }
    }
}

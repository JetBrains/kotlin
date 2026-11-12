/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration.Companion.HISTORY_FILE_DIR as JS_HISTORY_FILE_DIR
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration.Companion.HISTORY_FILE_DIR as WASM_HISTORY_FILE_DIR
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.AbstractProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAndPlatformAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JsModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.JvmModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.MetadataProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ProjectCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.model.WasmModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.WasmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.writeText

private val allBasicLibrarySources = setOf("A.kt", "DummyInLibMain.kt", "useAInLibMain.kt")
private val affectedLibrarySources = setOf("A.kt", "useAInLibMain.kt")

// Behavioral tests for `FORCE_RECOMPILATION` (KT-90388).
// The basic compatibility check lives in `IncrementalCompilationSmokeTest.forceRecompilationRebuildsAllSources`.

@DisplayName("Forced non-incremental compilation via the FORCE_RECOMPILATION IC option")
class ForceRecompilationTest : BaseCompilationTest() {
    @DisplayName("Next build after a forced build is incremental again")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @TestMetadata("js-ic-basic-lib")
    fun nextBuildIsIncrementalAgain(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val libModule = module("js-ic-basic-lib")
            // The reported rebuild reason differs between platforms:
            // JVM maps FORCE_RECOMPILATION to unknown classpath changes,
            // JS/Wasm reach the rebuild by deleting the build history file before the compilation
            val expectedRebuildReason = if (libModule is JvmModule) "Unknown inputs changes" else "Build history file not found"

            libModule.compileIncrementally(SourcesChanges.ToBeCalculated)
            libModule.compileIncrementally(
                SourcesChanges.ToBeCalculated,
                forceNonIncrementalCompilation = true,
            ) {
                assertCompiledSources(allBasicLibrarySources)
                assertLogContainsPatterns(
                    expectedLogLevel(),
                    ".*Non-incremental compilation will be performed: $expectedRebuildReason.*".toRegex()
                )
            }

            libModule.changeAPublicApi()
            libModule.compileIncrementally(SourcesChanges.ToBeCalculated) {
                assertCompiledSources(affectedLibrarySources)
            }
        }
    }

    @DisplayName("Dependent module is recompiled against an API change made in a forced build of its dependency")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @TestMetadata("basic-multimodule-project/module-1")
    fun dependentModuleSeesApiChangeFromForcedBuild(project: ProjectCreator) {
        project {
            assumeNotMetadata()
            val libModule = module("basic-multimodule-project/module-1")
            val appModule = module("basic-multimodule-project/module-2", dependencies = listOf(libModule))

            libModule.compileIncrementally(SourcesChanges.ToBeCalculated)
            appModule.compileIncrementally(SourcesChanges.ToBeCalculated)

            libModule.sourcesDirectory.resolve("bar.kt").writeText("class Bar")
            libModule.compileIncrementally(SourcesChanges.ToBeCalculated, forceNonIncrementalCompilation = true) {
                assertCompiledSources("bar.kt", "baz.kt", "foo.kt")
            }

            appModule.compileIncrementally(SourcesChanges.ToBeCalculated) {
                expectFailWithError(".*b\\.kt:\\d+:\\d+ Unresolved reference 'bar'.*".toRegex())
            }
        }
    }

    @DisplayName("Forced build and the next incremental build work when IC history is stored outside the working directory")
    @BtaV2StrategyAndPlatformAgnosticCompilationTest
    @TestMetadata("js-ic-basic-lib")
    fun separateHistoryFileDir(project: ProjectCreator) {
        project {
            assumeTrue(this is JsProject || this is WasmProject, "A separate history directory is only supported for JS and Wasm")
            val historyDirectory = projectDirectory.resolveSibling("${projectDirectory.fileName}-external-history")
            val libModule = module("js-ic-basic-lib")

            fun compileLib(force: Boolean = false, expectedSources: Set<String>? = null) {
                when (libModule) {
                    is JsModule -> libModule.compileIncrementally(
                        SourcesChanges.ToBeCalculated,
                        forceNonIncrementalCompilation = force,
                        icOptionsConfigAction = { it[JS_HISTORY_FILE_DIR] = historyDirectory },
                    ) {
                        expectedSources?.let { assertCompiledSources(it) }
                    }
                    is WasmModule -> libModule.compileIncrementally(
                        SourcesChanges.ToBeCalculated,
                        forceNonIncrementalCompilation = force,
                        icOptionsConfigAction = { it[WASM_HISTORY_FILE_DIR] = historyDirectory },
                    ) {
                        expectedSources?.let { assertCompiledSources(it) }
                    }
                    else -> error("HISTORY_FILE_DIR is only supported for JS and Wasm")
                }
            }

            compileLib()
            compileLib(force = true, expectedSources = allBasicLibrarySources)
            libModule.changeAPublicApi()
            compileLib(expectedSources = affectedLibrarySources)
        }
    }

    private fun AbstractProject<*, *, *>.expectedLogLevel(): LogLevel =
        if (defaultStrategyConfig is ExecutionPolicy.WithDaemon) LogLevel.DEBUG else LogLevel.INFO // TODO: KT-85024

    private fun AbstractProject<*, *, *>.assumeNotMetadata() {
        assumeFalse(this is MetadataProject, "Metadata compilation doesn't support incremental compilation")
    }

    private fun Module<*, *, *>.changeAPublicApi() {
        sourcesDirectory.resolve("A.kt").writeText("class A { val x = \"a\" }")
    }
}

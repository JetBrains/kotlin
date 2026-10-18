/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.TRACK_CONFIGURATION_INPUTS
import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.currentKotlinStdlibLocation
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import javax.tools.ToolProvider
import kotlin.io.path.createDirectories
import kotlin.io.path.pathString
import kotlin.io.path.writeText

/**
 * The sources are members of a module patched with `-Xpatch-module` when one of the sources compiled is in a patch path,
 * so an incremental round that compiles only some of the Kotlin sources could compile them in another module.
 */
@OptIn(ExperimentalCompilerArgument::class)
@DisplayName("Incremental compilation with -Xpatch-module")
class JavaModulePatchChangesTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Editing a Kotlin source in the patch path is compiled incrementally (internally tracked)")
    @TestMetadata("ic-scenarios/patch-module")
    fun testEditInPatchPathIsIncrementalInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleName = "ic-scenarios/patch-module"
            trackedModule(
                moduleName = moduleName,
                compilationConfigAction = patchModuleConfigAction(moduleName),
                icOptionsConfigAction = trackConfigurationInputs,
            ).editInPatchPathAndAssertIncrementalCompilation(strategyConfig)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Editing a Kotlin source in the patch path is compiled incrementally (externally tracked)")
    @TestMetadata("ic-scenarios/patch-module")
    fun testEditInPatchPathIsIncrementalExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleName = "ic-scenarios/patch-module"
            module(
                moduleName = moduleName,
                compilationConfigAction = patchModuleConfigAction(moduleName),
                icOptionsConfigAction = trackConfigurationInputs,
            ).editInPatchPathAndAssertIncrementalCompilation(strategyConfig)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Removing the last Kotlin source in the patch path rebuilds and fails the source outside it (internally tracked)")
    @TestMetadata("ic-scenarios/patch-module-last-source-removed")
    fun testRemovingLastPatchedSourceRebuildsInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleName = "ic-scenarios/patch-module-last-source-removed"
            trackedModule(
                moduleName = moduleName,
                compilationConfigAction = patchModuleConfigAction(moduleName),
                icOptionsConfigAction = trackConfigurationInputs,
            ).removeLastPatchedSourceAndAssertRebuildFails(strategyConfig)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Removing the last Kotlin source in the patch path rebuilds and fails the source outside it (externally tracked)")
    @TestMetadata("ic-scenarios/patch-module-last-source-removed")
    fun testRemovingLastPatchedSourceRebuildsExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleName = "ic-scenarios/patch-module-last-source-removed"
            module(
                moduleName = moduleName,
                compilationConfigAction = patchModuleConfigAction(moduleName),
                icOptionsConfigAction = trackConfigurationInputs,
            ).removeLastPatchedSourceAndAssertRebuildFails(strategyConfig)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Adding a Kotlin source outside the patch path recompiles all Kotlin sources (internally tracked)")
    @TestMetadata("ic-scenarios/patch-module")
    fun testAddingSourceOutsidePatchPathRebuildsInternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleName = "ic-scenarios/patch-module"
            trackedModule(
                moduleName = moduleName,
                compilationConfigAction = patchModuleConfigAction(moduleName),
                icOptionsConfigAction = trackConfigurationInputs,
            ).addSourceOutsidePatchPathAndAssertRebuild(strategyConfig)
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Adding a Kotlin source outside the patch path recompiles all Kotlin sources (externally tracked)")
    @TestMetadata("ic-scenarios/patch-module")
    fun testAddingSourceOutsidePatchPathRebuildsExternallyTracked(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val moduleName = "ic-scenarios/patch-module"
            module(
                moduleName = moduleName,
                compilationConfigAction = patchModuleConfigAction(moduleName),
                icOptionsConfigAction = trackConfigurationInputs,
            ).addSourceOutsidePatchPathAndAssertRebuild(strategyConfig)
        }
    }

    private fun ScenarioModule.editInPatchPathAndAssertIncrementalCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        replaceFileWithVersion("patch/lib/internal/A.kt", "modified")
        compile {
            assertLogDoesNotContainPatterns(
                expectedLogLevelForRebuildReason(strategyConfig),
                ".*Non-incremental compilation will be performed.*".toRegex(),
            )
            assertCompiledSources("patch/lib/internal/A.kt")
        }
    }

    private fun ScenarioModule.removeLastPatchedSourceAndAssertRebuildFails(strategyConfig: CompilerExecutionStrategyConfiguration) {
        deleteFile("patch/lib/internal/A.kt")
        compile {
            expectFail()
            assertLogContainsPatterns(
                expectedLogLevelForRebuildReason(strategyConfig),
                ".*Non-incremental compilation will be performed: ${BuildAttribute.JAVA_MODULE_PATCH_CHANGED.readableString}".toRegex(),
            )
            // C.kt is compiled in the unnamed module, as in a clean build.
            assertLogContainsPatterns(
                LogLevel.ERROR,
                ".*/C\\.kt:\\d+:\\d+ Symbol is declared in module 'lib' which does not export package 'lib\\.internal'.*".toRegex(),
            )
            assertCompiledSources("other/C.kt")
        }
    }

    private fun ScenarioModule.addSourceOutsidePatchPathAndAssertRebuild(strategyConfig: CompilerExecutionStrategyConfiguration) {
        createPredefinedFile("other/C.kt", "add")
        compile {
            val reason = BuildAttribute.JAVA_MODULE_PATCH_WITH_SOURCES_OUTSIDE.readableString
            assertLogContainsPatterns(
                expectedLogLevelForRebuildReason(strategyConfig),
                ".*Non-incremental compilation will be performed: $reason".toRegex(),
            )
            // C.kt uses lib.internal, which only the members of lib can access.
            assertCompiledSources("patch/lib/internal/A.kt", "patch/lib/internal/B.kt", "other/C.kt")
        }
    }

    /**
     * Patches the module `lib`, which exports no package, with the `patch` directory in the sources of the module [moduleName].
     */
    private fun patchModuleConfigAction(moduleName: String): (JvmCompilationOperation.Builder) -> Unit {
        val libraryDirectory = buildLibrary()
        val patchDirectory = workingDirectory.resolve(moduleName).resolve("src").resolve("patch")
        return {
            it.compilerArguments[JvmCompilerArguments.X_MODULE_PATH] = listOf(libraryDirectory, currentKotlinStdlibLocation)
            it.compilerArguments[JvmCompilerArguments.X_ADD_MODULES] = listOf("lib")
            it.compilerArguments[JvmCompilerArguments.X_PATCH_MODULE] = listOf("lib=${patchDirectory.pathString}")
        }
    }

    private fun buildLibrary(): Path {
        val sources = workingDirectory.resolve("lib-src").createDirectories()
        val moduleInfo = sources.resolve("module-info.java").apply {
            writeText("module lib { requires kotlin.stdlib; }")
        }
        val impl = sources.resolve("lib/internal/Impl.java").apply {
            parent.createDirectories()
            writeText("package lib.internal; public class Impl { public static String impl() { return \"impl\"; } }")
        }
        val destination = workingDirectory.resolve("lib").createDirectories()
        val compiler = ToolProvider.getSystemJavaCompiler() ?: error("System Java compiler not found. Ensure running on a JDK.")
        val exitCode = compiler.run(
            null, null, null,
            "-d", destination.pathString,
            "--module-path", currentKotlinStdlibLocation.pathString,
            moduleInfo.pathString, impl.pathString,
        )
        check(exitCode == 0) { "javac failed to compile the library module" }
        return destination
    }

    private val trackConfigurationInputs: (JvmSnapshotBasedIncrementalCompilationConfiguration.Builder) -> Unit = {
        it[TRACK_CONFIGURATION_INPUTS] = true
    }

    private fun expectedLogLevelForRebuildReason(strategyConfig: CompilerExecutionStrategyConfiguration): LogLevel =
        if (strategyConfig.second is ExecutionPolicy.WithDaemon) LogLevel.DEBUG else LogLevel.INFO // TODO: KT-85024
}

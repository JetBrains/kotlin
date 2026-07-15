/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.compile
import org.jetbrains.kotlin.buildtools.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class ClasspathMetadataIncrementalTest : BaseCompilationTest() {

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Verify that incremental compilation without JVM classpath metadata leads to incorrect resolution")
    @TestMetadata("jvm-classpath-metadata")
    fun testWithJvmClasspathMetadataDisabled(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = jvmClasspathMetadataModule(enabled = false)
            module.execute("MainKt", "KMP output: Any")

            module.replaceFileWithVersion("commonMain/foo.kt", "changeInt")

            module.compile(setOf("commonMain/foo.kt"))
            // Regression control: with the option disabled, the incremental recompilation of `foo.kt` incorrectly
            // sees `jvmMain`'s `bar(i: Int)` through leftover outputs, so `foo()` now returns "Int".
            module.execute("MainKt", "KMP output: Int")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Verify that incremental compilation with JVM classpath metadata leads to correct resolution")
    @TestMetadata("jvm-classpath-metadata")
    fun testWithJvmClasspathMetadataEnabled(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = jvmClasspathMetadataModule(enabled = true)
            module.execute("MainKt", "KMP output: Any")

            module.replaceFileWithVersion("commonMain/foo.kt", "changeInt")

            module.compile(setOf("commonMain/foo.kt"))
            // With the option enabled, commonMain still resolves against commonMain only, so `foo()` returns "Any".
            module.execute("MainKt", "KMP output: Any")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Verify incremental compilation with JVM classpath metadata resolves correctly across two common modules")
    @TestMetadata("two-common-modules")
    fun testTwoCommonModulesWithJvmClasspathMetadata(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = twoCommonModulesModule()
            module.execute("MainKt", "KMP output: Any")

            module.replaceFileWithVersion("intermediateMain/foo.kt", "changeInt")

            module.compile(setOf("intermediateMain/foo.kt"))
            // `foo()` lives in the intermediate common module; it must keep resolving `bar` against `commonMain`'s
            // `bar(Any)`, not the leaf `jvmMain`'s `bar(Int)`, so the result stays "Any".
            module.execute("MainKt", "KMP output: Any")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Verify incremental compilation preserves packages of files not recompiled")
    @TestMetadata("metadata-header-merge")
    fun testIncrementalCompilationPreservesUntouchedPackages(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = metadataHeaderMergeModule()

            module.replaceFileWithVersion("commonMain/com/example/two/foo.kt", "changeIntStep1")
            module.compile(setOf("commonMain/com/example/two/foo.kt"))

            module.replaceFileWithVersion("commonMain/com/example/two/foo.kt", "changeIntStep2")
            module.compile(setOf("commonMain/com/example/two/foo.kt"))
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Verify removed package does not break incremental compilation")
    @TestMetadata("metadata-header-merge")
    fun testRemovedPackageDoesNotBreakIncrementalCompilation(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = metadataHeaderMergeModule()

            module.deleteFile("commonMain/com/example/two/foo.kt")
            module.compile {
                assertNoCompiledSources()
            }

            module.replaceFileWithVersion("commonMain/com/example/one/bar.kt", "change")
            module.compile(setOf("commonMain/com/example/one/bar.kt"))
        }
    }
}

private typealias JvmScenario = Scenario<JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>

@OptIn(ExperimentalCompilerArgument::class)
private fun JvmScenario.jvmClasspathMetadataModule(enabled: Boolean) = module(
    "jvm-classpath-metadata",
    compilationConfigAction = configureKmpJvmFragments(enableClasspathMetadata = enabled),
    icOptionsConfigAction = {
        it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
    },
)

@OptIn(ExperimentalCompilerArgument::class)
private fun JvmScenario.metadataHeaderMergeModule() = module(
    "metadata-header-merge",
    compilationConfigAction = configureKmpJvmFragments(enableClasspathMetadata = true),
    icOptionsConfigAction = {
        it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
    },
)

@OptIn(ExperimentalCompilerArgument::class)
private fun JvmScenario.twoCommonModulesModule() = module(
    "two-common-modules",
    compilationConfigAction = configureKmpJvmFragments(enableClasspathMetadata = true),
    icOptionsConfigAction = {
        it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
    },
)

private fun configureKmpJvmFragments(enableClasspathMetadata: Boolean): (JvmCompilationOperation.Builder) -> Unit = { builder ->
    val fragmentHierarchy = listOf("commonMain", "intermediateMain", "jvmMain")
    fun fragmentOf(path: Path): String? = fragmentHierarchy.firstOrNull { fragment -> path.any { it.toString() == fragment } }

    val sourcesByFragment = builder.sources
        .mapNotNull { source -> fragmentOf(source)?.let { fragment -> fragment to source } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })

    val fragments = fragmentHierarchy.filter { it in sourcesByFragment }
    val fragmentSources = fragments.flatMap { fragment ->
        sourcesByFragment.getValue(fragment).map { "$fragment:${it.absolutePathString()}" }
    }
    val fragmentRefines = fragments.zipWithNext { parent, child -> "$child:$parent" }

    val args = buildList {
        add("-Xmulti-platform")
        add("-Xfragments=${fragments.joinToString(",")}")
        if (fragmentRefines.isNotEmpty()) {
            add("-Xfragment-refines=${fragmentRefines.joinToString(",")}")
        }
        add("-Xfragment-sources=${fragmentSources.joinToString(",")}")
        add("-Xuse-ic-classpath-metadata=$enableClasspathMetadata")
    }

    builder.compilerArguments.applyArgumentStrings(args)
}

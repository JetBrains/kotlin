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
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
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
    @DisplayName("Verify recompiling an actual declaration together with its expect declaration")
    @TestMetadata("expect-actual-metadata")
    fun testRecompilationOfActualAndExpect(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = expectActualModule()
            module.execute("MainKt", "KMP output: fooJvm")

            module.replaceFileWithVersion("jvmMain/actualFoo.kt", "change")

            module.compile(setOf("jvmMain/actualFoo.kt", "commonMain/expectFoo.kt"))
            module.execute("MainKt", "KMP output: fooJvm")
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

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-88997: incremental compilation of a common source using an expect fake override with an intermediate fragment")
    @TestMetadata("expect-fake-override-metadata")
    fun testExpectFakeOverrideWithIntermediateFragment(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = expectFakeOverrideModule()
            module.execute("JvmKt", "fakeOverrideResult=initial")

            module.replaceFileWithVersion("commonMain/fakeOverrideResult.kt", "change")

            module.compile(setOf("commonMain/fakeOverrideResult.kt"))
            module.execute("JvmKt", "fakeOverrideResult=common")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-89044: adding an overload to an expect class with an actual typealias recompiles common call sites")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testExpectClassOverloadAddedViaActualTypealias(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = expectTypealiasOverloadModule()
            module.execute("MainKt", "KMP output: Any")

            module.replaceFileWithVersion("commonMain/base.kt", "addOverload")
            module.replaceFileWithVersion("jvmMain/b1.kt", "addOverload")

            // `result.kt` resolved `A1().foo(42)` against the expect class, so its lookup is `foo` in scope `A1`,
            // not `B1`. Only a diff of the cached common metadata for `base.kt` produces that symbol.
            module.compile(setOf("commonMain/base.kt", "jvmMain/b1.kt", "jvmMain/actual.kt", "commonMain/result.kt"))
            module.execute("MainKt", "KMP output: Int")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-89044: removing an overload from an expect class with an actual typealias recompiles common call sites")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testExpectClassOverloadRemovedViaActualTypealias(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = expectTypealiasOverloadModule()

            module.replaceFileWithVersion("commonMain/base.kt", "addOverload")
            module.replaceFileWithVersion("jvmMain/b1.kt", "addOverload")
            module.compile(setOf("commonMain/base.kt", "jvmMain/b1.kt", "jvmMain/actual.kt", "commonMain/result.kt"))
            module.execute("MainKt", "KMP output: Int")

            module.replaceFileWithVersion("commonMain/base.kt", "removeOverload")
            module.replaceFileWithVersion("jvmMain/b1.kt", "removeOverload")
            // The diff of the cached expect-class metadata reports `foo` as changed, so `result.kt` re-resolves to `foo(Any)`.
            module.compile(setOf("commonMain/base.kt", "jvmMain/b1.kt", "jvmMain/actual.kt", "commonMain/result.kt"))
            module.execute("MainKt", "KMP output: Any")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-89044: deleting a common file with an expect class drops its cached metadata without breaking later builds")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testDeletedExpectClassSourceIsRemovedFromMetadataCache(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = expectTypealiasOverloadModule()
            module.execute("MainKt", "KMP output: Any")

            module.deleteFile("commonMain/base.kt")
            module.deleteFile("jvmMain/actual.kt")
            module.deleteFile("jvmMain/b1.kt")
            module.replaceFileWithVersion("commonMain/result.kt", "noA1")
            // Deleted files are never "compiled"; only the rewritten call site is.
            module.compile(setOf("commonMain/result.kt"))
            module.execute("MainKt", "KMP output: none")

            // A follow-up incremental build must not trip over stale pending/removed metadata of `base.kt`.
            module.replaceFileWithVersion("commonMain/result.kt", "noA1")
            module.compile(setOf("commonMain/result.kt"))
            module.execute("MainKt", "KMP output: none")
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("KT-89044: deleting an expect class alone recompiles its unmodified common call site")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testDeletedExpectClassDirtiesUnmodifiedDependent(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val module = expectTypealiasOverloadModule()
            module.execute("MainKt", "KMP output: Any")

            module.deleteFile("commonMain/base.kt")
            module.deleteFile("jvmMain/actual.kt")
            module.deleteFile("jvmMain/b1.kt")

            // `result.kt` is untouched, so the only way it can stop compiling is by being recompiled: the removal of the cached
            // metadata of `base.kt` must mark its call site of `A1` dirty instead of leaving a stale `ResultKt.class` behind.
            module.compile {
                expectFailWithError(".*commonMain/result\\.kt:6:25 Unresolved reference 'A1'.*".toRegex())
                assertCompiledSources("commonMain/result.kt")
            }
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
private fun JvmScenario.expectActualModule() = module(
    "expect-actual-metadata",
    compilationConfigAction = configureKmpJvmFragments(enableClasspathMetadata = true),
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

@OptIn(ExperimentalCompilerArgument::class)
private fun JvmScenario.expectFakeOverrideModule() = module(
    "expect-fake-override-metadata",
    compilationConfigAction = configureKmpJvmFragments(enableClasspathMetadata = true),
    icOptionsConfigAction = {
        it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
    },
)

@OptIn(ExperimentalCompilerArgument::class)
private fun JvmScenario.expectFakeOverrideModuleNoIntermediate() = module(
    "expect-fake-override-metadata-no-intermediate",
    compilationConfigAction = configureKmpJvmFragments(enableClasspathMetadata = true),
    icOptionsConfigAction = {
        it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
    },
)

@OptIn(ExperimentalCompilerArgument::class)
private fun JvmScenario.expectTypealiasOverloadModule() = module(
    "expect-typealias-overload-metadata",
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
        add("-Xuse-metadata-on-incremental-classpath=$enableClasspathMetadata")
    }

    builder.compilerArguments.applyCommandLineArguments(args)
}

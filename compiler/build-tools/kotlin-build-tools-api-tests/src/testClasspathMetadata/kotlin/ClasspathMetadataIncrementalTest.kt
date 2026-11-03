/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration.Companion.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.js.operations.JsKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.wasm.operations.WasmKlibCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.*
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.*
import org.jetbrains.kotlin.buildtools.tests.compilation.util.compile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import java.nio.file.Path
import kotlin.io.path.absolutePathString

/**
 * Incremental compilation of common KMP sources must resolve them against their own fragment, like a non-incremental build does.
 *
 * The platform declarations in the test data deliberately differ in return type from the common ones they would shadow,
 * so resolving a common call against the platform view of the module fails the compilation.
 */
@DisplayName("Incremental compilation of common sources within a KMP module")
internal class ClasspathMetadataIncrementalTest : BaseCompilationTest() {

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("A recompiled common source resolves against common declarations only")
    @TestMetadata("common-overload-resolution")
    fun testCommonSourceResolvesAgainstCommonDeclarations(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("common-overload-resolution")

            module.replaceFileWithVersion("commonMain/foo.kt", "changeInt")

            // `foo()` must keep resolving `bar(42)` to `commonMain`'s `bar(Any): String`, not `platformMain`'s `bar(Int): Int`
            module.compile(setOf("commonMain/foo.kt"))
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Regression control: legacy incremental compilation resolves a recompiled common source against platform declarations")
    @TestMetadata("common-overload-resolution")
    fun testLegacyIncrementalCompilationResolvesAgainstPlatformDeclarations(strategyConfig: CompilerExecutionStrategyConfiguration) {
        // the legacy mode only exists on JVM
        jvmScenario(strategyConfig) {
            val module = kmpModule("common-overload-resolution", kmpFragmentsConfiguration(useMetadataOnIncrementalClasspath = false))

            module.replaceFileWithVersion("commonMain/foo.kt", "changeInt")

            // The recompiled `foo.kt` incorrectly sees `platformMain`'s `bar(i: Int): Int` through leftover outputs,
            // so `foo(): String` no longer compiles. This proves the test data detects the bug the other tests guard against.
            module.compile {
                expectFailWithError(".*commonMain/foo\\.kt.*mismatch.*".toRegex(RegexOption.IGNORE_CASE))
                assertCompiledSources("commonMain/foo.kt")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("A recompiled intermediate source resolves against its own and common declarations only")
    @TestMetadata("two-common-modules")
    fun testIntermediateSourceResolvesAgainstCommonDeclarations(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("two-common-modules")

            module.replaceFileWithVersion("intermediateMain/foo.kt", "changeInt")

            // `foo()` lives in the intermediate fragment; it must keep resolving `bar(42)` to `commonMain`'s `bar(Any): String`,
            // not the leaf `platformMain`'s `bar(Int): Int`
            module.compile(setOf("intermediateMain/foo.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Incremental compilation preserves packages of files not recompiled")
    @TestMetadata("metadata-header-merge")
    fun testIncrementalCompilationPreservesUntouchedPackages(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("metadata-header-merge")

            module.replaceFileWithVersion("commonMain/com/example/two/foo.kt", "changeIntStep1")
            module.compile(setOf("commonMain/com/example/two/foo.kt"))

            module.replaceFileWithVersion("commonMain/com/example/two/foo.kt", "changeIntStep2")
            module.compile(setOf("commonMain/com/example/two/foo.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Recompiling an actual declaration together with its expect declaration")
    @TestMetadata("expect-actual-metadata")
    fun testRecompilationOfActualAndExpect(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("expect-actual-metadata")

            module.replaceFileWithVersion("platformMain/actualFoo.kt", "change")

            module.compile(setOf("platformMain/actualFoo.kt", "commonMain/expectFoo.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("A removed package does not break incremental compilation")
    @TestMetadata("metadata-header-merge")
    fun testRemovedPackageDoesNotBreakIncrementalCompilation(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("metadata-header-merge")

            module.deleteFile("commonMain/com/example/two/foo.kt")
            module.compile {
                assertNoCompiledSources()
                // the outputs of the now-empty `com.example.two` package must be gone, not just left stale
                when (this@scenario) {
                    is JvmScenarioDsl -> {
                        assertOutputs("com/example/one/BarKt.class", "platformplaceholder/PlaceholderKt.class")
                        assertNoOutputDirectory("com/example/two")
                    }
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        assertKnmFileCount(expectedCount = 1, packageFqName = "com.example.one")
                        assertNoKnmFiles(packageFqName = "com.example.two")
                    }
                }
            }

            module.replaceFileWithVersion("commonMain/com/example/one/bar.kt", "change")
            module.compile(setOf("commonMain/com/example/one/bar.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-88997: incremental compilation of a common source using an expect fake override with an intermediate fragment")
    @TestMetadata("expect-fake-override-metadata")
    fun testExpectFakeOverrideWithIntermediateFragment(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("expect-fake-override-metadata")

            module.replaceFileWithVersion("commonMain/fakeOverrideResult.kt", "change")

            // `Derived().select(B())` must resolve to the common `Base.select(A): String`, not the platform `select(B): Int`
            module.compile(setOf("commonMain/fakeOverrideResult.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89300: incremental compilation of a common source using interface delegation over an expect interface")
    @TestMetadata("interface-delegation-metadata")
    fun testInterfaceDelegationOverExpectInterface(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("interface-delegation-metadata")

            module.replaceFileWithVersion("commonMain/delegationResult.kt", "change")

            // `select(42)` must resolve to the expect `select(Any): String`, not the actual-only `select(Int): Int`
            module.compile(setOf("commonMain/delegationResult.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89044: adding an overload to an expect class with an actual typealias recompiles common call sites")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testExpectClassOverloadAddedViaActualTypealias(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("expect-typealias-overload-metadata")

            module.replaceFileWithVersion("commonMain/base.kt", "addOverload")
            module.replaceFileWithVersion("platformMain/b1.kt", "addOverload")

            // `result.kt` resolved `A1().foo(42)` against the expect class, so its lookup is `foo` in scope `A1`,
            // not `B1`. Only a diff of the cached common metadata for `base.kt` produces that symbol.
            module.compile(setOf("commonMain/base.kt", "platformMain/b1.kt", "platformMain/actual.kt", "commonMain/result.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89044: removing an overload from an expect class with an actual typealias recompiles common call sites")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testExpectClassOverloadRemovedViaActualTypealias(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("expect-typealias-overload-metadata")

            module.replaceFileWithVersion("commonMain/base.kt", "addOverload")
            module.replaceFileWithVersion("platformMain/b1.kt", "addOverload")
            module.compile(setOf("commonMain/base.kt", "platformMain/b1.kt", "platformMain/actual.kt", "commonMain/result.kt"))

            module.replaceFileWithVersion("commonMain/base.kt", "removeOverload")
            module.replaceFileWithVersion("platformMain/b1.kt", "removeOverload")
            // The diff of the cached expect-class metadata reports `foo` as changed, so `result.kt` re-resolves to `foo(Any)`.
            module.compile(setOf("commonMain/base.kt", "platformMain/b1.kt", "platformMain/actual.kt", "commonMain/result.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89044: deleting a common file with an expect class drops its cached metadata without breaking later builds")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testDeletedExpectClassSourceIsRemovedFromMetadataCache(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("expect-typealias-overload-metadata")

            module.deleteFile("commonMain/base.kt")
            module.deleteFile("platformMain/actual.kt")
            module.deleteFile("platformMain/b1.kt")
            module.replaceFileWithVersion("commonMain/result.kt", "noA1")
            // Deleted files are never "compiled"; only the rewritten call site is.
            module.compile(setOf("commonMain/result.kt"))

            // A follow-up incremental build must not trip over stale pending/removed metadata of `base.kt`.
            module.replaceFileWithVersion("commonMain/result.kt", "noA1")
            module.compile(setOf("commonMain/result.kt"))
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89044: deleting an expect class alone recompiles its unmodified common call site")
    @TestMetadata("expect-typealias-overload-metadata")
    fun testDeletedExpectClassDirtiesUnmodifiedDependent(scenario: ScenarioCreator) {
        scenario {
            val module = kmpModule("expect-typealias-overload-metadata")

            module.deleteFile("commonMain/base.kt")
            module.deleteFile("platformMain/actual.kt")
            module.deleteFile("platformMain/b1.kt")

            // `result.kt` is untouched, so the only way it can stop compiling is by being recompiled: the removal of the cached
            // metadata of `base.kt` must mark its call site of `A1` dirty instead of leaving a stale output behind.
            module.compile {
                expectFailWithError(".*commonMain/result\\.kt:6:25 Unresolved reference 'A1'.*".toRegex())
                assertCompiledSources("commonMain/result.kt")
            }
        }
    }
}

/**
 * Creates a module whose sources are split into KMP fragments by their top-level directory:
 * `commonMain` <- `intermediateMain` <- `platformMain`.
 */
private fun Scenario<*, *>.kmpModule(
    moduleName: String,
    // the lambdas are shared between tests, so that the initial compilation of a module is cached
    compilationConfigAction: (BaseCompilationOperation.Builder) -> Unit = kmpFragmentsConfiguration(useMetadataOnIncrementalClasspath = true),
): ScenarioModule = module(
    moduleName,
    compilationConfigAction = compilationConfigAction,
    icOptionsConfigAction = { it: BaseIncrementalCompilationConfiguration.Builder ->
        it[UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM] = true
    },
)

private fun kmpFragmentsConfiguration(useMetadataOnIncrementalClasspath: Boolean): (BaseCompilationOperation.Builder) -> Unit = { builder ->
    val fragmentHierarchy = listOf("commonMain", "intermediateMain", "platformMain")
    fun fragmentOf(path: Path): String? = fragmentHierarchy.firstOrNull { fragment -> path.any { it.toString() == fragment } }

    val sources = when (builder) {
        is JvmCompilationOperation.Builder -> builder.sources
        is JsKlibCompilationOperation.Builder -> builder.sources
        is WasmKlibCompilationOperation.Builder -> builder.sources
        else -> error("Unsupported compilation operation: $builder")
    }
    val sourcesByFragment = sources
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
        // JS and Wasm always use the cached fragment metadata for incremental compilation; JVM still requires an opt-in
        if (builder is JvmCompilationOperation.Builder) {
            add("-Xuse-metadata-on-incremental-classpath=$useMetadataOnIncrementalClasspath")
        }
    }

    builder.compilerArguments.applyCommandLineArguments(args)
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertKnmFileCount
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoKnmFiles
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoOutputDirectory
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ModuleContext
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JsScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JvmScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.Scenario
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.WasmScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Package replaced by a same-named class in incremental compilation")
class PackageToClassChangesTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89297: Replacing a package with a same-named class having a companion function should recompile the fully-qualified usages")
    @TestMetadata("ic-scenarios/kt-89297")
    fun testReplacingPackageWithSameNamedClass(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-89297")

            // `test.bar.A()` in `main.kt` used to resolve to the constructor of `test.bar.A`; after the change it must resolve
            // to `test.bar.Companion.A()`. For that on JVM, the emptied `test/bar/` output directory must not survive the removal of
            // `test/bar/A.class`, otherwise the compiler keeps treating `test.bar` as an existing package.
            // On klib platforms, the related package fragment directory must not survive the removal of the file.
            mod.deleteFile("bar/A.kt")
            mod.createPredefinedFile("bar.kt", "companion")

            mod.compile {
                assertCompiledSources("bar.kt", "main.kt")
                assertPackageReplacedByClass(this@scenario, "test.bar")
            }

            mod.executeOnJvm()
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89297: Deleting all the sources of a package should remove its empty output directory")
    @TestMetadata("ic-scenarios/kt-89297-package-removal")
    fun testDeletingAllPackageSources(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-89297-package-removal")

            // the package has two files, both are deleted one by one, while the `bar/` sources directory itself stays
            mod.deleteFile("bar/A.kt")
            mod.deleteFile("bar/B.kt")
            mod.createPredefinedFile("bar.kt", "companion")

            mod.compile {
                assertCompiledSources("bar.kt", "main.kt")
                assertPackageReplacedByClass(this@scenario, "test.bar")
            }

            mod.executeOnJvm()
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89297: Emptying a nested package should remove the empty output directories up to the output root")
    @TestMetadata("ic-scenarios/kt-89297-nested")
    fun testEmptyingNestedPackage(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-89297-nested")

            // `test.bar.baz.A()` must resolve to the nested object `test.bar.baz` afterwards, so both `test/bar/baz` and `test/bar`
            // output directories must be gone: the cleanup goes bottom-up until a non-empty directory is met.
            mod.deleteFile("bar/baz/A.kt")
            mod.createPredefinedFile("bar.kt", "companion")

            mod.compile {
                assertCompiledSources("bar.kt", "main.kt")
                when (this@scenario) {
                    is JvmScenarioDsl -> {
                        assertOutputs(
                            "test/MainKt.class",
                            "test/bar.class",
                            "test/bar\$baz.class",
                            "test/bar\$A1.class",
                        )
                        assertNoOutputDirectory("test/bar/baz")
                        assertNoOutputDirectory("test/bar")
                    }
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // one package fragment per source file: `main.kt` and `bar.kt`, both in the `test` package
                        assertKnmFileCount(expectedCount = 2, packageFqName = "test")
                        assertNoKnmFiles(packageFqName = "test.bar.baz")
                        assertNoKnmFiles(packageFqName = "test.bar")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }

            mod.executeOnJvm()
        }
    }

    /**
     * Asserts that the former [packageFqName] package left nothing behind in the output: neither the package output directory
     * (JVM) nor the package fragment directory of the klib (klib platforms), and the outputs are the ones of the replacing class.
     */
    context(_: ModuleContext)
    private fun CompilationOutcome.assertPackageReplacedByClass(scenario: Scenario<*, *>, packageFqName: String) {
        when (scenario) {
            is JvmScenarioDsl -> {
                assertOutputs("test/MainKt.class", "test/bar.class", "test/bar\$Companion.class", "test/bar\$A1.class")
                assertNoOutputDirectory(packageFqName.replace('.', '/'))
            }
            is JsScenarioDsl, is WasmScenarioDsl -> {
                // one package fragment per source file: `main.kt` and `bar.kt`, both in the `test` package
                assertKnmFileCount(expectedCount = 2, packageFqName = "test")
                assertNoKnmFiles(packageFqName)
            }
            else -> error("Unsupported scenario type: $scenario")
        }
    }

    context(scenario: Scenario<*, *>)
    private fun ScenarioModule.executeOnJvm() {
        if (scenario is JvmScenarioDsl) {
            execute(mainClass = "test.MainKt", exactOutput = "companion")
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.buildtools.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Package replaced by a same-named class in incremental compilation")
class PackageToClassChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89297: Replacing a package with a same-named class having a companion function should recompile the fully-qualified usages")
    @TestMetadata("ic-scenarios/kt-89297")
    fun testReplacingPackageWithSameNamedClass(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-89297")

            // `test.bar.A()` in `main.kt` used to resolve to the constructor of `test.bar.A`; after the change it must resolve
            // to `test.bar.Companion.A()`. For that, the emptied `test/bar/` output directory must not survive the removal of
            // `test/bar/A.class`, otherwise the compiler keeps treating `test.bar` as an existing package.
            mod.deleteFile("bar/A.kt")
            mod.createPredefinedFile("bar.kt", "companion")

            mod.compile {
                assertCompiledSources("bar.kt", "main.kt")
                assertOutputs("test/MainKt.class", "test/bar.class", "test/bar\$Companion.class", "test/bar\$A1.class")
            }

            mod.execute(mainClass = "test.MainKt", exactOutput = "companion")
        }
    }
}

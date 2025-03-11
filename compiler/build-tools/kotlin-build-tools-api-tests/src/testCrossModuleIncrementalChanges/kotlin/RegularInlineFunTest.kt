/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.compile
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.execute
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

class RegularInlineFunTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("When regular inline function changes, its call site is recompiled")
    @TestMetadata("ic-scenarios/regular-inline-fun/basic-change/lib")
    fun testBasicCase(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/regular-inline-fun/basic-change/lib")
            val app = module(
                "ic-scenarios/regular-inline-fun/basic-change/app",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, true),
            )

            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "nice data")

            lib.replaceFileWithVersion("com/example/ictest/inlineFun.kt", "changeConstantPool")

            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlineFun.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "even nicer data")

            lib.changeFile("com/example/ictest/inlineFun.kt") { it.replace("return item", "return \"bar\"") }
            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlineFun.kt"))
            app.compile(expectedDirtySet = setOf("com/example/ictest/callSite.kt"))
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "bar")

            lib.changeFile("com/example/ictest/inlineFun.kt") { it.replace("return \"bar\"", "return \"foo\"") }
            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlineFun.kt"))
            app.compile(expectedDirtySet = setOf("com/example/ictest/callSite.kt"))
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "foo")
        }
    }
}

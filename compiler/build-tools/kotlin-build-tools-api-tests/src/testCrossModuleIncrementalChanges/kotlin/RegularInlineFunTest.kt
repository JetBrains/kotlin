/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
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
            )

            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "nice data")

            lib.replaceFileWithVersion("com/example/ictest/inlineFun.kt", "changeConstantPool")

            // the recompilation of call site is not necessary yet: constant belongs to the outer class, and it's not copied to the caller
            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlineFun.kt"))
            app.compile(expectedDirtySet = setOf())
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "even nicer data")

            lib.changeFile("com/example/ictest/inlineFun.kt") { it.replace("return item", "return \"bar\"") }
            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlineFun.kt"))
            app.compile(expectedDirtySet = setOf("com/example/ictest/callSite.kt"))
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "bar")

            // and this is different from the second step: now constant belongs to the inline fun itself
            lib.changeFile("com/example/ictest/inlineFun.kt") { it.replace("return \"bar\"", "return \"foo\"") }
            lib.compile(expectedDirtySet = setOf("com/example/ictest/inlineFun.kt"))
            app.compile(expectedDirtySet = setOf("com/example/ictest/callSite.kt"))
            app.execute(mainClass = "com.example.ictest.CallSiteKt", exactOutput = "foo")
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Recompilation of call site affected by an inline fun in a value class")
    @TestMetadata("ic-scenarios/inline-fun-in-value-class/lib")
    fun testJvmInlineValueClass(strategyConfig: CompilerExecutionStrategyConfiguration) {
        scenario(strategyConfig) {
            val lib = module("ic-scenarios/inline-fun-in-value-class/lib")
            val app = module(
                "ic-scenarios/inline-fun-in-value-class/app",
                dependencies = listOf(lib),
            )
            app.execute(mainClass = "CallSiteKt", exactOutput = "bar_123")

            lib.replaceFileWithVersion("callable.kt", "changeConstant")

            lib.compile(expectedDirtySet = setOf("callable.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = "foo_bar_123")

            lib.replaceFileWithVersion("callable.kt", "withChainOfInlineFuns")

            lib.compile(expectedDirtySet = setOf("callable.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = "123_bar")

            lib.replaceFileWithVersion("callable.kt", "withChainOfInlineFunsAndNewConstant")

            lib.compile(expectedDirtySet = setOf("callable.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt"))
            app.execute(mainClass = "CallSiteKt", exactOutput = "123_foo_bar")
        }
    }
}

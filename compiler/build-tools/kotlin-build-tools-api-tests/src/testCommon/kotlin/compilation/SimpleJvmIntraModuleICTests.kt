/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider
import org.junit.jupiter.api.DisplayName

@DisplayName("Smoke tests for incremental compilation within a single module via the build tools API")
class SimpleJvmIntraModuleICTests : DefaultIncrementalCompilationTest() {
    @CompilationTest
    fun smokeTest(buildRunnerProvider: BuildRunnerProvider) {
        scenario(buildRunnerProvider) {
            val module1 = module("jvm-module1")
            compileAll {
                expectSuccess(module1) {
                    compiledSources("foo.kt", "bar.kt", "baz.kt")
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
            }
            changeFile(module1, "bar.kt") {
                // change signature of the bar method
                """
                fun bar() {
                    foo()
                }
                """.trimIndent()
            }
            compileAll {
                expectSuccess(module1) {
                    compiledSources("bar.kt")
                    outputFiles("FooKt.class", "BarKt.class", "BazKt.class")
                }
            }
        }
    }
}
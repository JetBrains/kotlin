/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.tests.compilation.runner.BuildRunnerProvider
import org.junit.jupiter.api.DisplayName

@DisplayName("Smoke tests for cross-module incremental compilation via the build tools API")
class SimpleJvmCrossModuleICTests : IncrementalBaseCompilationTest() {
    @CompilationTest
    fun compilationAvoidance(buildRunnerProvider: BuildRunnerProvider) {
        scenario(buildRunnerProvider) {
            val module1 = module("jvm-module1")
            val module2 = module("jvm-module2") {
                dependsOn(module1)
            }
            compileAll {
                expectSuccess(module1) {
                    compiledSources("foo.kt", "bar.kt", "baz.kt")
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
                expectSuccess(module2) {
                    compiledSources("a.kt", "b.kt")
                    outputFiles("AKt.class", "BKt.class")
                }
            }
            changeFile(module1, "bar.kt") {
                // change the form of the bar method
                """
                class Bar {
                    fun bar() {
                        foo()
                    }
                }
                """.trimIndent()
            }
            compileAll {
                expectSuccess(module1) {
                    compiledSources("bar.kt")
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
                expectSuccess(module2) {
                    compiledSources(emptySet())
                    outputFiles("AKt.class", "BKt.class")
                }
            }
        }
    }

    @CompilationTest
    fun signatureChange(buildRunnerProvider: BuildRunnerProvider) {
        scenario(buildRunnerProvider) {
            val module1 = module("jvm-module1")
            val module2 = module("jvm-module2") {
                dependsOn(module1)
            }
            compileAll {
                expectSuccess(module1) {
                    compiledSources("foo.kt", "bar.kt", "baz.kt")
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
                expectSuccess(module2) {
                    compiledSources("a.kt", "b.kt")
                    outputFiles("AKt.class", "BKt.class")
                }
            }
            changeFile(module1, "bar.kt") {
                // change signature of the bar method
                """
                class Bar {
                    fun bar(s: String = "") {
                        println(s)
                        foo()
                    }
                }
                """.trimIndent()
            }
            compileAll {
                expectSuccess(module1) {
                    compiledSources("bar.kt")
                    outputFiles("FooKt.class", "Bar.class", "BazKt.class")
                }
                expectSuccess(module2) {
                    compiledSources("b.kt")
                    outputFiles("AKt.class", "BKt.class")
                }
            }
        }
    }
}
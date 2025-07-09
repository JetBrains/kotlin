/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTestArgumentProvider
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTestArgumentProvider.Companion.namedStrategyArguments
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.compile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.streams.asStream


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest()
@ArgumentsSource(
    StrategyAgnosticSnapshotterTestArgumentProvider::class
)
private annotation class StrategyAgnosticSnapshotterTest

private class StrategyAgnosticSnapshotterTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().flatMap<Named<*>, Arguments> { namedStrategyArg ->
            sequenceOf(
                Arguments.of(namedStrategyArg, ClassSnapshotGranularity.CLASS_LEVEL, named("ignore inlined classes", false)),
                Arguments.of(namedStrategyArg, ClassSnapshotGranularity.CLASS_LEVEL, named("use inlined classes", true)),
                Arguments.of(namedStrategyArg, ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, named("ignore inlined classes", false)),
                Arguments.of(namedStrategyArg, ClassSnapshotGranularity.CLASS_MEMBER_LEVEL, named("use inlined classes", true)),
            )
        }.asStream()
    }
}

/**
 * Test cases related to the exclusion of [SourceDebugExtension]: it is not ABI-significant,
 * unless there is an inline function declared inside the class
 *
 * We assume that propagating correct debug info to the call site is more important than compilation performance,
 * because issues with incorrect debug info might be hard to debug and understand, and, in the worst case, notice
 */
class ClasspathSnapshottingWithDebugInfoTest : BaseCompilationTest() {

    private fun ScenarioModule.addCallSite() {
        createFile(
            "callSite.kt",
            """
                    val result = Calc().calculate()

                    fun main(args: Array<String>) {
                        println(result)
                    }
                """.trimIndent()
        )
    }

    private fun ScenarioModule.addCalcWithInlineFunUsage() {
        createFile(
            "calc.kt",
            """
                    class Calc {
                        fun problemFun() {
                            listOf<String>().forEach { it ->
                                // do nothing
                            }
                        }
                    
                        fun calculate(): Int {
                            return 123
                        }
                    }
                """.trimIndent()
        )
    }

    private fun ScenarioModule.addCalcWithBasicLambda() {
        createFile(
            "calc.kt",
            """
                    class Calc {
                        fun problemFun() {
                            val casualLambdaToMakeSnapshotsJumpy = { 321 }
                        }
                    
                        fun calculate(): Int {
                            return 123
                        }
                    }
                """.trimIndent()
        )
    }

    private fun ScenarioModule.addCalcWithInlineFunDeclaration() {
        createFile(
            "calc.kt",
            """
                    class Calc {
                        inline fun bigProblemFun() {
                            println(2)
                        }
                    
                        fun calculate(): Int {
                            return 123
                        }
                    }
                """.trimIndent()
        )
    }

    @StrategyAgnosticSnapshotterTest
    @DisplayName("Class abiHash sensitivity in presence of inner classes")
    @TestMetadata("empty")
    fun testMainCase(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        granularity: ClassSnapshotGranularity,
        snapshotInlinedClasses: Boolean
    ) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(granularity, snapshotInlinedClasses)
            )

            app.addCallSite()

            // TODO also test - inline fun call site
            // TODO what if i add another lambda there?
            lib.addCalcWithBasicLambda()

            lib.compile()
            app.compile()

            lib.changeFile("calc.kt") {
                it.replace(
                    "return 123",
                    """
                        val unused = 42
                        return 1234
                    """.trimIndent()
                )
            }

            lib.compile(expectedDirtySet = setOf("calc.kt"))
            app.compile(expectedDirtySet = emptySet())
        }
    }

    @StrategyAgnosticSnapshotterTest
    @DisplayName("Class abiHash sensitivity in presence of an inline function call site")
    @TestMetadata("empty")
    fun testScenarioWithInlineFunUsageInLib(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        granularity: ClassSnapshotGranularity,
        snapshotInlinedClasses: Boolean
    ) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(
                    ClassSnapshotGranularity.CLASS_LEVEL,
                    useInlineLambdaSnapshotting = false //TODO if fixed, test with both flags
                )
            )

            app.addCallSite()

            lib.addCalcWithInlineFunUsage()

            lib.compile()
            app.compile()

            lib.changeFile("calc.kt") {
                it.replace(
                    "return 123",
                    """
                        val unused = 42
                        return 1234
                    """.trimIndent()
                )
            }

            lib.compile(expectedDirtySet = setOf("calc.kt"))
            app.compile(expectedDirtySet = emptySet())
        }
    }
}

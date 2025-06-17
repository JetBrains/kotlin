/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation

import org.jetbrains.kotlin.buildtools.api.ExecutionPolicy
import org.jetbrains.kotlin.buildtools.api.KotlinToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.DefaultStrategyAgnosticCompilationTestArgumentProvider.Companion.namedStrategyArguments
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.SnapshotConfig
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.ScenarioModule
import org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario.scenario
import org.jetbrains.kotlin.buildtools.api.tests.compilation.util.compile
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.util.stream.Stream


@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest()
@ArgumentsSource(
    StrategyAgnosticSnapshotterTestArgumentProvider::class
)
private annotation class StrategyAgnosticSnapshotterTest

private class StrategyAgnosticSnapshotterTestArgumentProvider : ArgumentsProvider {
    override fun provideArguments(context: ExtensionContext): Stream<out Arguments> {
        return namedStrategyArguments().flatMap { namedStrategyArg ->
            sequenceOf(
                Arguments.of(
                    namedStrategyArg,
                    ClassSnapshotGranularity.CLASS_LEVEL,
                    named("ignore inlined classes", false)
                ),
                Arguments.of(
                    namedStrategyArg,
                    ClassSnapshotGranularity.CLASS_LEVEL,
                    named("use inlined classes", true)
                ),
                Arguments.of(
                    namedStrategyArg,
                    ClassSnapshotGranularity.CLASS_MEMBER_LEVEL,
                    named("ignore inlined classes", false)
                ),
                Arguments.of(
                    namedStrategyArg,
                    ClassSnapshotGranularity.CLASS_MEMBER_LEVEL,
                    named("use inlined classes", true)
                ),
            )
        }.stream()
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
                            val casualLambda = { 321 }
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
        snapshotInlinedClasses: Boolean,
    ) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(granularity, snapshotInlinedClasses)
            )

            app.addCallSite()
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
        snapshotInlinedClasses: Boolean,
    ) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(granularity, snapshotInlinedClasses)
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

    @StrategyAgnosticSnapshotterTest
    @DisplayName("Class abiHash sensitivity in presence of an inline function declaration")
    @TestMetadata("empty")
    fun testScenarioWithInlineFunDeclarationInLib(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        granularity: ClassSnapshotGranularity,
        snapshotInlinedClasses: Boolean,
    ) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(granularity, snapshotInlinedClasses)
            )

            app.addCallSite()
            lib.addCalcWithInlineFunDeclaration()

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
            app.compile(expectedDirtySet = emptySet()) // no change expected

            lib.changeFile("calc.kt") {
                it.replace(
                    "inline fun bigProblemFun() {",
                    """
                        
                        
                        inline fun bigProblemFun() {
                        
                        
                    """ // straight up whitespace manipulation
                )
            }

            lib.compile(expectedDirtySet = setOf("calc.kt"))

            val expectedDirtySet = when (granularity) {
                ClassSnapshotGranularity.CLASS_MEMBER_LEVEL -> emptySet()
                ClassSnapshotGranularity.CLASS_LEVEL -> setOf("callSite.kt")
            }
            app.compile(expectedDirtySet = expectedDirtySet)
            // why does it behave differently? because of snapshotDiff logic. external classes are initially filtered by the classAbiHash.
            // if we're working with class-level snapshots, that's end of story, and these classes are marked dirty.
            // but, if we have member-level snapshots, we then compare the abis of individual members inside the changed classes.
            // if there's no member-level difference, nothing is added to the dirty set.
            // that actually means that there's one more bug! with member-level snapshotting,
            // we would fail to propagate the debuginfo change to the call sites, and it's been broken since forever
        }
    }

    @StrategyAgnosticSnapshotterTest
    @DisplayName("Class abiHash sensitivity when an unreachable local class is added")
    @TestMetadata("empty")
    fun testScenarioWhereLocalClassIsAdded(
        strategyConfig: CompilerExecutionStrategyConfiguration,
        granularity: ClassSnapshotGranularity,
        snapshotInlinedClasses: Boolean,
    ) {
        scenario(strategyConfig) {
            val lib = module("empty")
            val app = module(
                "empty2",
                dependencies = listOf(lib),
                snapshotConfig = SnapshotConfig(granularity, snapshotInlinedClasses)
            )

            app.addCallSite()
            lib.createFile(
                "calc.kt",
                """
                    class Calc {
                        fun calculate(): Int {
                            return 123
                        }
                    }
                """.trimIndent()
            )

            lib.compile()
            app.compile()

            lib.changeFile("calc.kt") {
                it.replace(
                    "class Calc {",
                    """
                        class Calc {
                        
                        private val unused = object : Runnable {
                            //val throwInALambdaJustForFun = { 1 }
                        
                            override fun run() { error("nope") }
                        }
                    """.trimIndent()
                )
            }

            lib.compile(expectedDirtySet = setOf("calc.kt"))
            app.compile(expectedDirtySet = setOf("callSite.kt")) // not ideal
            /*
             * so KT-62556 is unrelated after all: here the difference begins with
             *
             * // access flags 0x19
             * public final static INNERCLASS Calc$unused$1 null null
             */
        }
    }
}

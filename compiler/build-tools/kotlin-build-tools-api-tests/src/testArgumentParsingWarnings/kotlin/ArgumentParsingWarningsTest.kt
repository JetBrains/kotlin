/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests

import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.JVM_TARGET
import org.jetbrains.kotlin.buildtools.api.arguments.enums.JvmTarget
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatternExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsSubstringExactlyTimes
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertNoWarnings
import org.jetbrains.kotlin.buildtools.tests.compilation.model.BtaV2StrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

/**
 * KT-88381
 */
@DisplayName("Argument-parsing warnings are reported through the Build Tools API")
class ArgumentParsingWarningsTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument passed multiple times within a single applyArgumentStrings call is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentPassedMultipleTimesInOneCallReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=11", "-jvm-target=17"))
            }) {
                assertPassedMultipleTimes("-jvm-target", "11", "17")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument set via the typed API and then via applyArgumentStrings is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentSetViaTypedApiThenArgumentStringsReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments[JVM_TARGET] = JvmTarget.JVM_11
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=17"))
            }) {
                assertPassedMultipleTimes("-jvm-target", "11", "17")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument set via applyArgumentStrings and then via the typed API is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentSetViaArgumentStringsThenTypedApiReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        // this is the shape reported in KT-88381: the Kotlin Gradle plugin derives -jvm-target from the toolchain,
        // and the user additionally passes -jvm-target through freeCompilerArgs
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=17"))
                it.compilerArguments[JVM_TARGET] = JvmTarget.JVM_11
            }) {
                assertPassedMultipleTimes("-jvm-target", "17", "11")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument set just once via applyArgumentStrings is not reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentSetOnlyViaApplyArgumentStringsReportsNoWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=17"))
            }) {
                assertNoWarnings()
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument set just once via typed API is not reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentSetOnlyViaTypiedApiReportsNoWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments[JVM_TARGET] = JvmTarget.JVM_11
            }) {
                assertNoWarnings()
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An unknown advanced flag is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testUnknownAdvancedFlagReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xnot-a-real-flag"))
            }) {
                // an unknown -X flag is not an error, the compilation is expected to succeed
                assertLogContainsPatternExactlyTimes(
                    LogLevel.WARN,
                    ".*Flag is not supported by this version of the compiler: -Xnot-a-real-flag.*".toRegex(RegexOption.IGNORE_CASE),
                    1,
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("A deprecated argument name is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testDeprecatedArgumentNameReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xjsr305-annotations=strict"))
            }) {
                assertLogContainsPatternExactlyTimes(
                    LogLevel.WARN,
                    ".*-Xjsr305-annotations is deprecated\\. Please use -Xjsr305 instead.*".toRegex(RegexOption.IGNORE_CASE),
                    1,
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("A removed argument is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testRemovedArgumentReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xcontext-receivers"))
            }) {
                assertLogContainsPatternExactlyTimes(
                    LogLevel.WARN,
                    ".*The argument '-Xcontext-receivers' was removed in Kotlin .*\\. It has no effect\\..*".toRegex(RegexOption.IGNORE_CASE),
                    1,
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("A lifecycle-deprecated argument is still reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testDeprecatedLifecycleArgumentReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        // unlike a removed argument, a deprecated one does reach the compiler, which reports `DEPRECATED_CLI_ARG`
        // itself -- reporting argument-parsing warnings on the Build Tools API side must not duplicate it
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xsuppress-warning=NOTHING_TO_INLINE"))
            }) {
                assertLogContainsSubstringExactlyTimes(
                    LogLevel.WARN,
                    "The argument '-Xsuppress-warning' is deprecated since Kotlin",
                    1,
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("A plain valid argument produces no argument-parsing warning")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testValidArgumentReportsNoWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-no-stdlib"))
            }) {
                assertNoWarnings()
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument set via applyArgumentStrings, the typed API and applyArgumentStrings again is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentSetViaArgumentStringsAroundTypedApiReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        // a value configured through the typed API in between two applyArgumentStrings calls has to keep its position
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=11"))
                it.compilerArguments[JVM_TARGET] = JvmTarget.JVM_17
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=21"))
            }) {
                assertPassedMultipleTimes("-jvm-target", "11", "17", "21")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("An argument set via two separate applyArgumentStrings calls is reported")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testArgumentSetViaTwoArgumentStringsCallsReportsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=11"))
                it.compilerArguments.applyArgumentStrings(listOf("-jvm-target=17"))
            }) {
                assertPassedMultipleTimes("-jvm-target", "11", "17")
            }
        }
    }

    /**
     * [values] are expected in configuration order — the compiler reports them in the order they were supplied, and
     * the last one is the one it actually uses.
     */
    private fun CompilationOutcome.assertPassedMultipleTimes(arg: String, vararg values: String) {
        val renderedValues = values.joinToString("', '")
        assertLogContainsPatternExactlyTimes(
            LogLevel.WARN,
            ".*Argument '$arg' is passed multiple times: '$renderedValues'\\. The last value will be used\\..*"
                .toRegex(RegexOption.IGNORE_CASE),
            1,
        )
    }
}

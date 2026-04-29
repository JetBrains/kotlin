/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.restricted

import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AssertionsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import kotlin.io.path.absolutePathString

@DisplayName("Restricted arguments via applyArgumentStrings")
class RestrictedArgumentsTest : BaseCompilationTest() {
    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-Xbuild-file emits a warning")
    fun testXbuildFileWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        testBuildFile(strategyConfig, "-Xbuild-file")
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-module (deprecated name for -Xbuild-file) emits a warning")
    fun testModuleDeprecatedNameWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        testBuildFile(strategyConfig, "-module")
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Restricted argument among valid arguments still emits a warning")
    fun testRestrictedAmongValidArgumentsWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        testBuildFile(strategyConfig, "-Xbuild-file", additionalArg = "-no-stdlib")
    }

    private fun testBuildFile(strategyConfig: CompilerExecutionStrategyConfiguration, argument: String, additionalArg: String? = null) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            val moduleFile = workingDirectory.resolve("some/path.xml")
            module.checkRestrictedArgument(
                "-Xbuild-file", "-module",
                errorSince = KotlinReleaseVersion.v2_5_0,
                configuredArgs = listOfNotNull(additionalArg, "$argument=$moduleFile"),
                expectedCompilationError = true,
            ) {
                assertLogContainsLines(LogLevel.ERROR, "Module definition file does not exist: ${moduleFile.absolutePathString()}")
                if (additionalArg != null) {
                    assertLogContainsPatterns(LogLevel.DEBUG, "Kotlin compiler args: .* $additionalArg .*".toRegex())
                }
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-d emits a warning")
    fun testDestinationWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.checkRestrictedArgument(
                "-d",
                errorSince = KotlinReleaseVersion.v2_5_0,
                configuredArgs = listOf("-d", "output/dir")
            ) {
                assertLogContainsLines(
                    LogLevel.WARN,
                    "Argument '-d' is not supported in the Build Tools API. " +
                            "The destination is configured via the ${JvmPlatformToolchain::jvmCompilationOperationBuilder::parameters.get()[2].name} " +
                            "parameter of ${JvmPlatformToolchain::jvmCompilationOperationBuilder.name}. " +
                            "This warning will become an error starting from Kotlin 2.5.0."
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-include-runtime emits a warning")
    fun testIncludeRuntimeWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.checkRestrictedArgument(
                "-include-runtime",
                errorSince = KotlinReleaseVersion.v2_5_0,
                configuredArgs = listOf("-include-runtime")
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-expression emits a warning")
    fun testExpressionWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        testExpression(strategyConfig, listOf("-expression=hello"))
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-e (short for -expression) emits a warning")
    fun testShortExpressionWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        testExpression(strategyConfig, listOf("-e", "hello"))
    }

    private fun testExpression(strategyConfig: CompilerExecutionStrategyConfiguration, actualArgs: List<String>) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.checkRestrictedArgument(
                "-expression", "-e",
                errorSince = KotlinReleaseVersion.v2_5_0,
                configuredArgs = actualArgs,
                expectedCompilationError = true,
            ) {
                assertLogContainsLines(LogLevel.ERROR, "Unable to evaluate script, no scripting plugin loaded")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-Xrepl emits a warning")
    fun testXReplWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.checkRestrictedArgument(
                "-Xrepl",
                errorSince = KotlinReleaseVersion.v2_5_0,
                configuredArgs = listOf("-Xrepl"),
                expectedCompilationError = true,
            ) {
                assertLogContainsLines(LogLevel.ERROR, "Unable to run REPL, no scripting plugin loaded")
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-Xenable-incremental-compilation emits a warning")
    fun testEnableIncrementalCompilationWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.checkRestrictedArgument(
                "-Xenable-incremental-compilation",
                errorSince = KotlinReleaseVersion.v2_5_0,
                configuredArgs = listOf("-Xenable-incremental-compilation")
            ) {
                assertLogContainsLines(
                    LogLevel.WARN,
                    "Argument '-Xenable-incremental-compilation' is not supported in the Build Tools API. " +
                            "Configure it via the ${JvmCompilationOperation::class.simpleName}.${JvmCompilationOperation::INCREMENTAL_COMPILATION.name}" +
                            " option instead. This warning will become an error starting from Kotlin 2.5.0."
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Multiple restricted arguments emit warnings for each")
    fun testMultipleRestrictedArgumentsWarnings(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.checkRestrictedArguments(
                listOf("-include-runtime") to KotlinReleaseVersion.v2_5_0,
                listOf("-Xenable-incremental-compilation") to KotlinReleaseVersion.v2_5_0,
                configuredArgs = listOf("-include-runtime", "-Xenable-incremental-compilation"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Non-restricted arguments do not produce warnings about unsupported arguments")
    fun testNonRestrictedArgumentsNoWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-no-stdlib"))
            }) {
                assertLogDoesNotContainPatterns(LogLevel.WARN, Regex(".*is not supported in the Build Tools API.*"))
                assertLogContainsPatterns(LogLevel.DEBUG, "Kotlin compiler args: .* -no-stdlib .*".toRegex())
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Dropped argument does not produce a warning about unsupported arguments")
    fun testDroppedArgumentNoWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xallow-kotlin-package"))
            }) {
                assertLogDoesNotContainPatterns(LogLevel.WARN, Regex(".*is not supported in the Build Tools API.*"))
                assertLogContainsPatterns(LogLevel.DEBUG, "Kotlin compiler args: .* -Xallow-kotlin-package .*".toRegex())
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Wrong case for enum argument value emits a warning")
    fun testWrongCaseForEnumValue(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("jvm-module-1")

            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xassertions=jVm"))
                @OptIn(ExperimentalCompilerArgument::class)
                assertEquals(it.compilerArguments[X_ASSERTIONS], AssertionsMode.JVM)
            }) {
                assertLogContainsLines(
                    LogLevel.WARN,
                    "Case mismatch for -Xassertions: expected 'jvm', got 'jVm'. This will become an error in Kotlin compiler version 2.6.0"
                )
            }
        }
    }

    private fun CompilationOutcome.assertRestrictedArgWarning(
        argumentAliases: List<String>,
        errorSince: KotlinReleaseVersion,
    ) {
        val aliasesAsString = argumentAliases.joinToString(separator = "/") { "'$it'" }
        assertLogContainsPatterns(
            LogLevel.WARN,
            Regex(".*Argument $aliasesAsString is not supported in the Build Tools API.* This warning will become an error starting from Kotlin ${errorSince.releaseName}.")
        )
    }

    private fun assertRestrictedArgError(
        argumentAliases: List<String>,
        exception: CompilerArgumentsParseException,
    ) {
        val aliasesAsString = argumentAliases.joinToString(separator = "/") { "'$it'" }
        assert(
            exception.message?.contains("$aliasesAsString is not supported in the Build Tools API.") == true &&
                    exception.message?.contains("will become an error") == false
        ) {
            "CompilerArgumentsParseException should be thrown for $aliasesAsString with expected message, but it was not: ${exception.message}"
        }
    }

    private fun Module<JvmCompilationOperation, JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.checkRestrictedArgument(
        vararg argumentAliases: String,
        errorSince: KotlinReleaseVersion,
        configuredArgs: List<String>,
        expectedCompilationError: Boolean = false,
        additionalCompilationAssertions: CompilationOutcome.() -> Unit = {},
    ) = checkRestrictedArguments(
        argumentAliases.toList() to errorSince,
        configuredArgs = configuredArgs,
        expectedCompilationError = expectedCompilationError,
        additionalCompilationAssertions = additionalCompilationAssertions,
    )

    private fun Module<JvmCompilationOperation, JvmCompilationOperation.Builder, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder>.checkRestrictedArguments(
        vararg restrictedArgs: Pair<List<String>, KotlinReleaseVersion>,
        configuredArgs: List<String>,
        expectedCompilationError: Boolean = false,
        additionalCompilationAssertions: CompilationOutcome.() -> Unit = {},
    ) {
        val currentVersion = KotlinToolingVersion(project.kotlinToolchain.getCompilerVersion())
        val firstErrorSince = restrictedArgs.first().second
        val isWarning = currentVersion < KotlinToolingVersion(firstErrorSince.major, firstErrorSince.minor, firstErrorSince.patch, "dev-1")

        if (isWarning) {
            compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(configuredArgs)
            }) {
                if (expectedCompilationError) {
                    expectFail()
                }
                for ((aliases, errorSince) in restrictedArgs) {
                    assertRestrictedArgWarning(aliases, errorSince)
                }
                additionalCompilationAssertions()
            }
        } else {
            // Error args require separate compilations because the first error throws an exception
            for ((aliases, _) in restrictedArgs) {
                val compilationBody = {
                    compile(compilationConfigAction = {
                        it.compilerArguments.applyArgumentStrings(configuredArgs)
                    }) {
                        if (expectedCompilationError) {
                            expectFail()
                        }
                        additionalCompilationAssertions()
                    }
                }
                val exception = assertThrows<CompilerArgumentsParseException> { compilationBody() }
                assertRestrictedArgError(aliases, exception)
            }
        }
    }
}

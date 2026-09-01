/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.restricted

import org.jetbrains.kotlin.arguments.dsl.base.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.Companion.X_ASSERTIONS
import org.jetbrains.kotlin.buildtools.api.arguments.enums.AssertionsMode
import org.jetbrains.kotlin.buildtools.api.jvm.JvmPlatformToolchain
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsLines
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogDoesNotContainPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
            val module = module("basic-multimodule-project/module-1")
            val moduleFile = workingDirectory.resolve("some/path.xml")
            module.checkRestrictedArguments(
                restrictedArg("-Xbuild-file", "-module", errorSince = KotlinReleaseVersion.v2_5_0),
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
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-d", errorSince = KotlinReleaseVersion.v2_5_0),
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
    @DisplayName("-d reports during execution on metadata")
    fun testDestinationReportsDuringExecutionOnMetadata(strategyConfig: CompilerExecutionStrategyConfiguration) {
        metadataProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-d", errorSince = KotlinReleaseVersion.v2_5_0),
                configuredArgs = listOf("-d", "output/dir")
            ) {
                assertLogContainsLines(
                    LogLevel.WARN,
                    "Argument '-d' is not supported in the Build Tools API. " +
                            "The destination is configured via the ${KotlinMetadataPlatformToolchain::metadataKlibCompilationOperationBuilder::parameters.get()[2].name} " +
                            "parameter of ${KotlinMetadataPlatformToolchain::metadataKlibCompilationOperationBuilder.name}. " +
                            "This warning will become an error starting from Kotlin 2.5.0."
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-Xlegacy-metadata-jar-k2 reports restricted usage")
    fun testLegacyMetadataJar(strategyConfig: CompilerExecutionStrategyConfiguration) {
        metadataProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-Xlegacy-metadata-jar-k2", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xlegacy-metadata-jar-k2")
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("-include-runtime emits a warning")
    fun testIncludeRuntimeWarningDuringExecution(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-include-runtime", errorSince = KotlinReleaseVersion.v2_5_0),
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
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-expression", "-e", errorSince = KotlinReleaseVersion.v2_5_0),
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
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-Xrepl", errorSince = KotlinReleaseVersion.v2_5_0),
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
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-Xenable-incremental-compilation", errorSince = KotlinReleaseVersion.v2_5_0),
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
            val module = module("basic-multimodule-project/module-1")
            module.checkRestrictedArguments(
                restrictedArg("-include-runtime", errorSince = KotlinReleaseVersion.v2_5_0),
                restrictedArg("-Xenable-incremental-compilation", errorSince = KotlinReleaseVersion.v2_5_0),
                configuredArgs = listOf("-include-runtime", "-Xenable-incremental-compilation"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Non-restricted arguments do not produce warnings about unsupported arguments")
    fun testNonRestrictedArgumentsNoWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmProject(strategyConfig) {
            val module = module("basic-multimodule-project/module-1")
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
            val module = module("basic-multimodule-project/module-1")
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
            val module = module("basic-multimodule-project/module-1")

            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(listOf("-Xassertions=jVm"))
                @OptIn(ExperimentalCompilerArgument::class)
                assertEquals(AssertionsMode.JVM, it.compilerArguments[X_ASSERTIONS])
            }) {
                assertLogContainsLines(
                    LogLevel.WARN,
                    "Case mismatch for -Xassertions: expected 'jvm', got 'jVm'. This will become an error in Kotlin compiler version 2.6.0"
                )
            }
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("JS klib compilation: -ir-output-dir emits a warning")
    fun testJsIrOutputDirWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedArguments(
                restrictedArg("-ir-output-dir", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-ir-output-dir", "output/dir"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("JS linking: -Xir-produce-js and -Xinclude emit warnings")
    fun testJsLinkingRestrictedArgumentsWarnings(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedLinkingArguments(
                restrictedArg("-Xir-produce-js", errorSince = KotlinReleaseVersion.v2_6_0),
                restrictedArg("-Xinclude", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xir-produce-js", "-Xinclude=${module.outputDirectory}"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Wasm klib compilation: -ir-output-dir emits a warning")
    fun testWasmIrOutputDirWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedArguments(
                restrictedArg("-ir-output-dir", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-ir-output-dir", "output/dir"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Wasm linking: -Xir-produce-js and -Xinclude emit warnings")
    fun testWasmLinkingRestrictedArgumentsWarnings(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedLinkingArguments(
                restrictedArg("-Xir-produce-js", errorSince = KotlinReleaseVersion.v2_6_0),
                restrictedArg("-Xinclude", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xir-produce-js", "-Xinclude=${module.outputDirectory}"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("JS klib compilation: -Xir-produce-klib-dir emits a warning")
    fun testJsIrProduceKlibDirWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedArguments(
                restrictedArg("-Xir-produce-klib-dir", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xir-produce-klib-dir"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("JS klib compilation: -Xir-produce-klib-file emits a warning")
    fun testJsIrProduceKlibFileWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedArguments(
                restrictedArg("-Xir-produce-klib-file", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xir-produce-klib-file"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Wasm klib compilation: -Xir-produce-klib-dir emits a warning")
    fun testWasmIrProduceKlibDirWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedArguments(
                restrictedArg("-Xir-produce-klib-dir", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xir-produce-klib-dir"),
            )
        }
    }

    @BtaV2StrategyAgnosticCompilationTest
    @DisplayName("Wasm klib compilation: -Xir-produce-klib-file emits a warning")
    fun testWasmIrProduceKlibFileWarning(strategyConfig: CompilerExecutionStrategyConfiguration) {
        wasmProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            module.checkRestrictedArguments(
                restrictedArg("-Xir-produce-klib-file", errorSince = KotlinReleaseVersion.v2_6_0),
                configuredArgs = listOf("-Xir-produce-klib-file"),
            )
        }
    }

    private fun restrictedArg(
        vararg argumentAliases: String,
        errorSince: KotlinReleaseVersion,
    ): Pair<List<String>, KotlinReleaseVersion> = argumentAliases.toList() to errorSince

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

    private fun Module<out BaseCompilationOperation, *, *>.checkRestrictedArguments(
        vararg restrictedArgs: Pair<List<String>, KotlinReleaseVersion>,
        configuredArgs: List<String>,
        expectedCompilationError: Boolean = false,
        additionalCompilationAssertions: CompilationOutcome.() -> Unit = {},
    ) {
        val currentVersion = KotlinToolingVersion(project.kotlinToolchain.getCompilerVersion())
        runRestrictedArgumentsCheck(
            currentVersion = currentVersion,
            restrictedArgs = restrictedArgs.toList(),
            configuredArgs = configuredArgs,
            expectedCompilationError = expectedCompilationError,
            additionalCompilationAssertions = additionalCompilationAssertions,
        ) { compilationConfigAction, assertions ->
            compile(compilationConfigAction = compilationConfigAction) { assertions() }
        }
    }

    private fun <O : BaseCompilationOperation, B : BaseCompilationOperation.Builder, M> M.checkRestrictedLinkingArguments(
        vararg restrictedArgs: Pair<List<String>, KotlinReleaseVersion>,
        configuredArgs: List<String>,
        expectedCompilationError: Boolean = false,
        additionalCompilationAssertions: CompilationOutcome.() -> Unit = {},
    ) where M : Module<*, *, *>, M : LinkableModule<O, B> {
        compile()
        val currentVersion = KotlinToolingVersion(project.kotlinToolchain.getCompilerVersion())
        runRestrictedArgumentsCheck(
            currentVersion = currentVersion,
            restrictedArgs = restrictedArgs.toList(),
            configuredArgs = configuredArgs,
            expectedCompilationError = expectedCompilationError,
            additionalCompilationAssertions = additionalCompilationAssertions,
        ) { compilationConfigAction, assertions ->
            link(compilationConfigAction = compilationConfigAction) { assertions() }
        }
    }

    private fun isWarningPhase(
        currentVersion: KotlinToolingVersion,
        restrictedArgs: List<Pair<List<String>, KotlinReleaseVersion>>,
    ): Boolean {
        val firstErrorSince = restrictedArgs.minOf { it.second }
        return currentVersion < KotlinToolingVersion(firstErrorSince.major, firstErrorSince.minor, firstErrorSince.patch, "dev-1")
    }

    private fun runRestrictedArgumentsCheck(
        currentVersion: KotlinToolingVersion,
        restrictedArgs: List<Pair<List<String>, KotlinReleaseVersion>>,
        configuredArgs: List<String>,
        expectedCompilationError: Boolean,
        additionalCompilationAssertions: CompilationOutcome.() -> Unit,
        runOperation: (
            compilationConfigAction: (BaseCompilationOperation.Builder) -> Unit,
            assertions: CompilationOutcome.() -> Unit,
        ) -> Unit,
    ) {
        if (isWarningPhase(currentVersion, restrictedArgs)) {
            runOperation({ it.compilerArguments.applyArgumentStrings(configuredArgs) }) {
                if (expectedCompilationError) {
                    expectFail()
                }
                for ([aliases, errorSince] in restrictedArgs) {
                    assertRestrictedArgWarning(aliases, errorSince)
                }
                additionalCompilationAssertions()
            }
        } else {
            // Error args require separate compilations because the first error throws an exception
            val exception = assertThrows<CompilerArgumentsParseException> {
                runOperation({ it.compilerArguments.applyArgumentStrings(configuredArgs) }) {
                    if (expectedCompilationError) {
                        expectFail()
                    }
                    additionalCompilationAssertions()
                }
            }
            assertTrue(
                restrictedArgs.flatMap { it.first }.any { alias ->
                    exception.message?.contains("'$alias' is not supported in the Build Tools API.") == true
                }
            ) {
                "Exception was: \"${exception.message}\" and did not contain any of ${restrictedArgs.flatMap { it.first }.joinToString()}"
            }
        }
    }
}

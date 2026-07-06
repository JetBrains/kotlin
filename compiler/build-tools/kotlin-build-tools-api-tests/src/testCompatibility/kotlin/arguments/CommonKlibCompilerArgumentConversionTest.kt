/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.AllCommonKlibCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.JS_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.JS_LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.WASM_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.CommonKlibArgumentOperationKind.WASM_LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.InvalidArgumentValueCommonKlibCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.InvalidRawValueCommonKlibCompilerArgumentsBtaV2StrategyAgnosticTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonklib.NullableCommonKlibCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.wasmProject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

internal class CommonKlibCompilerArgumentConversionTest : BaseCompilationTest() {
    @AllCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> CommonKlibArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val arguments = buildArguments {
                setArgument(this, value)
            }

            assertEquals(
                expectedArgumentStringsFor(getValueString(value)),
                arguments.toArgumentStrings(),
            )
        }
    }

    @AllCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> CommonKlibArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val arguments = buildArguments()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            arguments.toArgumentStrings(),
        )
    }

    @AllCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument can be set and retrieved")
    fun <T> CommonKlibArgumentConfiguration<T>.testBtaArgumentGetWhenSet() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val arguments = buildArguments {
                setArgument(this, value)
            }

            assertEquals(value, getArgument(arguments))
        }
    }

    @AllCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> CommonKlibArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val arguments = buildArguments()

        assertEquals(
            getDefaultValueString(), getValueString(getArgument(arguments))
        )
    }

    @AllCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument strings are converted to BTA argument")
    fun <T> CommonKlibArgumentConfiguration<T>.testRawArgumentStringsConversion() {
        assumeArgumentSupported()
        for (value in argumentRawValues) {
            val arguments = buildArguments {
                applyArgumentStrings(expectedArgumentStringsFor(value))
            }

            assertEquals(value, getValueString(getArgument(arguments)))
        }
    }

    @AllCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> CommonKlibArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val arguments = buildArguments {
            applyArgumentStrings(listOf())
        }

        assertEquals(
            getDefaultValueString(), getValueString(getArgument(arguments))
        )
    }

    @InvalidArgumentValueCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument with non-existent argument value fails conversion")
    fun <T> CommonKlibArgumentConfiguration<T>.testInvalidArgumentConversionFails() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            assertThrows<CompilerArgumentsParseException> {
                buildArguments {
                    setArgument(this, invalidValue)
                }
            }
        }
    }

    @InvalidRawValueCommonKlibCompilerArgumentsBtaV2StrategyAgnosticTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at compilation time")
    fun testInvalidRawArgumentCompilationFails(config: Pair<CommonKlibArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>) {
        val [argumentConfig, strategyConfig] = config
        argumentConfig.assumeArgumentSupported()

        when (argumentConfig.operationKind) {
            JS_KLIB -> jsProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                for (invalidValue in argumentConfig.invalidRawValues) {
                    module.compile(compilationConfigAction = {
                        it.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
                    }) {
                        expectFail()
                        assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
                    }
                }
            }
            JS_LINKING -> jsProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                module.compile()
                for (invalidValue in argumentConfig.invalidRawValues) {
                    module.link(compilationConfigAction = {
                        it.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
                    }) {
                        expectFail()
                        assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
                    }
                }
            }
            WASM_KLIB -> wasmProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                for (invalidValue in argumentConfig.invalidRawValues) {
                    module.compile(compilationConfigAction = {
                        it.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
                    }) {
                        expectFail()
                        assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
                    }
                }
            }
            WASM_LINKING -> wasmProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                module.compile()
                for (invalidValue in argumentConfig.invalidRawValues) {
                    module.link(compilationConfigAction = {
                        it.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
                    }) {
                        expectFail()
                        assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
                    }
                }
            }
        }
    }

    @NullableCommonKlibCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> CommonKlibArgumentConfiguration<T?>.testNullBtaArgument() {
        assumeArgumentSupported()
        val arguments = buildArguments {
            setArgument(this, null)
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            arguments.toArgumentStrings(),
        )
    }

    private fun CommonKlibArgumentConfiguration<*>.assumeArgumentSupported() {
        assumeTrue(isPlatformSupported(), "Test requires selected platform BTA support")
        assumeTrue(
            kotlinToolchain.getCompilerVersion() >= availableSinceVersion.toString(),
            "Test requires compiler version >= $availableSinceVersion"
        )
    }
}

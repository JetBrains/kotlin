/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.*
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.*
import org.jetbrains.kotlin.buildtools.tests.arguments.util.assumeArgumentAvailable
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows

internal class CommonJsAndWasmCompilerArgumentConversionTest : BaseCompilationTest() {
    @AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
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

    @AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val arguments = buildArguments()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            arguments.toArgumentStrings(),
        )
    }

    @AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument can be set and retrieved")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testBtaArgumentGetWhenSet() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val arguments = buildArguments {
                setArgument(this, value)
            }

            assertEquals(value, getArgument(arguments))
        }
    }

    @AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val arguments = buildArguments()

        assertEquals(
            getDefaultValueString(), getValueString(getArgument(arguments))
        )
    }

    @AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument strings are converted to BTA argument")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testRawArgumentStringsConversion() {
        assumeArgumentSupported()
        for (value in argumentRawValues) {
            val arguments = buildArguments {
                applyArgumentStrings(expectedArgumentStringsFor(value))
            }

            assertEquals(value, getValueString(getArgument(arguments)))
        }
    }

    @AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val arguments = buildArguments {
            applyArgumentStrings(listOf())
        }

        assertEquals(
            getDefaultValueString(), getValueString(getArgument(arguments))
        )
    }

    @InvalidArgumentValueCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument with non-existent argument value fails conversion")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testInvalidArgumentConversionFails() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            assertThrows<CompilerArgumentsParseException> {
                buildArguments {
                    setArgument(this, invalidValue)
                }
            }
        }
    }

    @InvalidRawValueCommonJsAndWasmCompilerArgumentsBtaV2StrategyAgnosticTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at compilation time")
    fun testInvalidRawArgumentCompilationFails(config: Pair<CommonJsAndWasmArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>) {
        val [argumentConfig, strategyConfig] = config
        argumentConfig.assumeArgumentSupported()

        when (argumentConfig.operationKind) {
            JS_KLIB -> jsProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                argumentConfig.testInvalidRawArgumentKlibCompilationFails(module)
            }
            JS_LINKING -> jsProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                module.compile()
                argumentConfig.testInvalidRawArgumentLinkingFails(module)
            }
            WASM_KLIB -> wasmProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                argumentConfig.testInvalidRawArgumentKlibCompilationFails(module)
            }
            WASM_LINKING -> wasmProject(strategyConfig) {
                val module = module("js-ic-basic-lib")
                module.compile()
                argumentConfig.testInvalidRawArgumentLinkingFails(module)
            }
        }
    }

    @NullableCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> CommonJsAndWasmArgumentConfiguration<T?>.testNullBtaArgument() {
        assumeArgumentSupported()
        val arguments = buildArguments {
            setArgument(this, null)
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            arguments.toArgumentStrings(),
        )
    }

    private fun CommonJsAndWasmArgumentConfiguration<*>.assumeArgumentSupported() {
        assumeTrue(isPlatformSupported(), "Test requires selected platform BTA support")
        assumeArgumentAvailable()
    }

    private fun <B : BaseCompilationOperation.Builder> CommonJsAndWasmArgumentConfiguration<*>.testInvalidRawArgumentKlibCompilationFails(
        module: Module<*, B, *>,
    ) {
        for (invalidValue in invalidRawValues) {
            module.compile(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(expectedArgumentStringsFor(invalidValue))
            }) {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
            }
        }
    }

    private fun <B : BaseCompilationOperation.Builder> CommonJsAndWasmArgumentConfiguration<*>.testInvalidRawArgumentLinkingFails(
        module: LinkableModule<*, B>,
    ) {
        for (invalidValue in invalidRawValues) {
            module.link(compilationConfigAction = {
                it.compilerArguments.applyArgumentStrings(expectedArgumentStringsFor(invalidValue))
            }) {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
            }
        }
    }
}

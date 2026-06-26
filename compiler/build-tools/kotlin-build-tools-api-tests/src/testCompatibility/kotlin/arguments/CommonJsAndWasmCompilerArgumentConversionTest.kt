/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.AllCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.JS_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.JS_LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.WASM_KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.CommonJsAndWasmArgumentOperationKind.WASM_LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.InvalidArgumentValueCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.InvalidRawValueCommonJsAndWasmCompilerArgumentsBtaV2StrategyAgnosticTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.commonjswasm.NullableCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LinkableModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.wasmProject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
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

    // Kept @Disabled because the JS/Wasm SearchPathType arguments do not reject a path containing File.pathSeparator (KT-87212).
    // The current buggy behavior is pinned by testPathSeparatorInValueCurrentlyAccepted below; once the
    // validation is added, that test will fail - then enable this test and delete it.
    @Disabled("KT-87212: enable once File.pathSeparator validation is added for JS/Wasm SearchPathType arguments")
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

    // Exact mirror of the @Disabled testInvalidArgumentConversionFails above,
    // but asserting the current buggy behavior: the conversion does NOT reject a path containing
    // File.pathSeparator. Runs only for the three SearchPathType arguments that have invalidArgumentValues.
    @InvalidArgumentValueCommonJsAndWasmCompilerArgumentsWithBtaVersionsTest
    @DisplayName("KNOWN BUG (KT-87212): a path containing File.pathSeparator is silently accepted, not rejected")
    fun <T> CommonJsAndWasmArgumentConfiguration<T>.testPathSeparatorInvalidValuesAccepted() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            // BUG: once File.pathSeparator validation is added there, this will throw CompilerArgumentsParseException
            // and this assertion will fail - the signal to delete this test and enable
            // testInvalidArgumentConversionFails above (remove its @Disabled).
            assertDoesNotThrow {
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
        assumeTrue(
            kotlinToolchain.getCompilerVersion() >= availableSinceVersion.toString(),
            "Test requires compiler version >= $availableSinceVersion"
        )
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

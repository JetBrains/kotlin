/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.js.AllJsCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.js.InvalidRawValueJsCompilerArgumentsBtaV2StrategyAgnosticTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.js.JsArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.js.JsArgumentOperationKind.KLIB
import org.jetbrains.kotlin.buildtools.tests.arguments.model.js.JsArgumentOperationKind.LINKING
import org.jetbrains.kotlin.buildtools.tests.arguments.model.js.NullableJsCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.util.assumeArgumentAvailable
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LinkableModule
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.Module
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jsProject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.DisplayName

internal class JsCompilerArgumentConversionTest : BaseCompilationTest() {
    @AllJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> JsArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
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

    @AllJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> JsArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val arguments = buildArguments()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            arguments.toArgumentStrings(),
        )
    }

    @AllJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument can be set and retrieved")
    fun <T> JsArgumentConfiguration<T>.testBtaArgumentGetWhenSet() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val arguments = buildArguments {
                setArgument(this, value)
            }

            assertEquals(value, getArgument(arguments))
        }
    }

    @AllJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> JsArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val arguments = buildArguments()

        assertEquals(
            getDefaultValueString(), getValueString(getArgument(arguments))
        )
    }

    @AllJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument strings are converted to BTA argument")
    fun <T> JsArgumentConfiguration<T>.testRawArgumentStringsConversion() {
        assumeArgumentSupported()
        for (value in argumentRawValues) {
            val arguments = buildArguments {
                applyArgumentStrings(expectedArgumentStringsFor(value))
            }

            assertEquals(value, getValueString(getArgument(arguments)))
        }
    }

    @AllJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> JsArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val arguments = buildArguments {
            applyArgumentStrings(listOf())
        }

        assertEquals(
            getDefaultValueString(), getValueString(getArgument(arguments))
        )
    }

    @InvalidRawValueJsCompilerArgumentsBtaV2StrategyAgnosticTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at compilation time")
    fun testInvalidRawArgumentCompilationFails(config: Pair<JsArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>) {
        val [argumentConfig, strategyConfig] = config
        argumentConfig.assumeArgumentSupported()

        jsProject(strategyConfig) {
            val module = module("js-ic-basic-lib")
            when (argumentConfig.operationKind) {
                KLIB -> argumentConfig.testInvalidRawArgumentKlibCompilationFails(module)
                LINKING -> {
                    module.compile()
                    argumentConfig.testInvalidRawArgumentLinkingFails(module)
                }
            }
        }
    }

    @NullableJsCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> JsArgumentConfiguration<T?>.testNullBtaArgument() {
        assumeArgumentSupported()
        val arguments = buildArguments {
            setArgument(this, null)
        }

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            arguments.toArgumentStrings(),
        )
    }

    private fun JsArgumentConfiguration<*>.assumeArgumentSupported() {
        assumeTrue(isPlatformSupported(), "Test requires selected platform BTA support")
        assumeArgumentAvailable()
    }

    private fun <B : BaseCompilationOperation.Builder> JsArgumentConfiguration<*>.testInvalidRawArgumentKlibCompilationFails(
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

    private fun <B : BaseCompilationOperation.Builder> JsArgumentConfiguration<*>.testInvalidRawArgumentLinkingFails(
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

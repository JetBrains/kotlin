/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments

import org.jetbrains.kotlin.buildtools.api.CompilerArgumentsParseException
import org.jetbrains.kotlin.buildtools.api.metadata.KotlinMetadataPlatformToolchain.Companion.metadata
import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata.AllMetadataCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata.InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata.InvalidRawValueMetadataCompilerArgumentsBtaV2StrategyAgnosticTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata.InvalidRawValueMetadataCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata.MetadataArgumentConfiguration
import org.jetbrains.kotlin.buildtools.tests.arguments.model.metadata.NullableMetadataCompilerArgumentsWithBtaVersionsTest
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.metadataProject
import org.jetbrains.kotlin.buildtools.tests.compilation.model.supportsMetadata
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import java.nio.file.Paths
import kotlin.io.path.writeText

internal class MetadataCompilerArgumentConversionTest : BaseCompilationTest() {

    @AllMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument is converted to a raw argument")
    fun <T> MetadataArgumentConfiguration<T>.testBtaArgumentToArgumentString() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }.build()

            assertEquals(
                expectedArgumentStringsFor(getValueString(value)),
                operation.compilerArguments.toArgumentStrings(),
            )
        }
    }

    @AllMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument has the default value when BTA argument is not set")
    fun <T> MetadataArgumentConfiguration<T>.testBtaArgumentNotSetByDefault() {
        assumeArgumentSupported()
        val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        assertEquals(
            expectedArgumentStringsFor(getDefaultValueString()),
            operation.compilerArguments.toArgumentStrings(),
        )
    }

    @AllMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument can be set and retrieved")
    fun <T> MetadataArgumentConfiguration<T>.testBtaArgumentGetWhenSet() {
        assumeArgumentSupported()
        for (value in argumentValues) {
            val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments[argumentKey] = value
            }.build()

            assertEquals(value, operation.compilerArguments[argumentKey])
        }
    }

    @AllMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when not set")
    fun <T> MetadataArgumentConfiguration<T>.testBtaArgumentGetWhenNull() {
        assumeArgumentSupported()
        val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).build()

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[argumentKey])
        )
    }

    @AllMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("Raw argument strings are converted to BTA argument")
    fun <T> MetadataArgumentConfiguration<T>.testRawArgumentStringsConversion() {
        assumeArgumentSupported()
        for (value in argumentRawValues) {
            val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                compilerArguments.applyArgumentStrings(expectedArgumentStringsFor(value))
            }.build()

            assertEquals(value, getValueString(operation.compilerArguments[argumentKey]))
        }
    }

    @AllMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument has the default value when no raw arguments are applied")
    fun <T> MetadataArgumentConfiguration<T>.testNoRawArgumentStrings() {
        assumeArgumentSupported()
        val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments.applyArgumentStrings(listOf())
        }.build()

        assertEquals(
            getDefaultValueString(), getValueString(operation.compilerArguments[argumentKey])
        )
    }

    // Kept @Disabled because the Metadata SearchPathType argument CLASSPATH does NOT yet reject a path containing File.pathSeparator (KT-87212).
    // The current buggy behavior is pinned by testPathSeparatorInvalidValuesAccepted below; once the validation
    // is added, that test will fail - then enable this test and delete it.
    @Disabled("KT-87212: enable once File.pathSeparator validation is added for the Metadata CLASSPATH argument")
    @InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument with non-existent argument value fails conversion")
    fun <T> MetadataArgumentConfiguration<T>.testInvalidArgumentConversionFails() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            assertThrows<CompilerArgumentsParseException> {
                kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                    compilerArguments[argumentKey] = invalidValue
                }.build()
            }
        }
    }

    @InvalidArgumentValueMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("KNOWN BUG (KT-87212): a path containing File.pathSeparator is silently accepted, not rejected")
    fun <T> MetadataArgumentConfiguration<T>.testPathSeparatorInvalidValuesAccepted() {
        assumeArgumentSupported()
        for (invalidValue in invalidArgumentValues) {
            // BUG: once File.pathSeparator validation is added there, this will throw CompilerArgumentsParseException
            // and this assertion will fail - the signal to delete this test and enable
            // testInvalidArgumentConversionFails above (remove its @Disabled).
            assertDoesNotThrow {
                kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                    compilerArguments[argumentKey] = invalidValue
                }.build()
            }
        }
    }

    // Checks the EXPECTED (deferred) behavior: an invalid enum raw value should be reported at execution
    // (COMPILATION_ERROR), not thrown from applyArgumentStrings. Kept @Disabled because Metadata
    // X_TARGET_PLATFORM (enum-list) currently throws eagerly (see testInvalidRawArgumentThrowsEagerly).
    @Disabled("KT-87218: Metadata X_TARGET_PLATFORM (enum-list) throws from applyArgumentStrings instead of deferring the error to execution")
    @InvalidRawValueMetadataCompilerArgumentsBtaV2StrategyAgnosticTest
    @DisplayName("Raw argument with non-existent BTA argument value is rejected at compilation time")
    fun testInvalidRawArgumentCompilationFails(config: Pair<MetadataArgumentConfiguration<*>, CompilerExecutionStrategyConfiguration>) {
        val argumentConfig = config.first
        val strategyConfig = config.second
        argumentConfig.assumeArgumentSupported()

        metadataProject(strategyConfig) {
            val module = module("empty")
            module.sourcesDirectory.resolve("box.kt").writeText("fun box(): String = \"OK\"")
            for (invalidValue in argumentConfig.invalidRawValues) {
                module.compile(compilationConfigAction = {
                    it.compilerArguments.applyArgumentStrings(argumentConfig.expectedArgumentStringsFor(invalidValue))
                }) {
                    expectFail()
                    assertLogContainsPatterns(LogLevel.ERROR, Regex(".*${Regex.escape(invalidValue)}.*"))
                }
            }
        }
    }

    @InvalidRawValueMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("KNOWN BUG (KT-87218): an invalid enum value should be deferred to execution, but applyArgumentStrings rejects it eagerly")
    fun <T> MetadataArgumentConfiguration<T>.testInvalidRawArgumentThrowsEagerly() {
        assumeArgumentSupported()
        for (invalidValue in invalidRawValues) {
            // BUG: per the BTA contract the error should be deferred to execution (like other enum arguments),
            // but applyArgumentStrings throws eagerly. When fixed this assertThrows will fail - the signal to
            // delete this test and enable testInvalidRawArgumentCompilationFails above.
            assertThrows<CompilerArgumentsParseException> {
                kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
                    compilerArguments.applyArgumentStrings(expectedArgumentStringsFor(invalidValue))
                }.build()
            }
        }
    }

    @NullableMetadataCompilerArgumentsWithBtaVersionsTest
    @DisplayName("BTA argument of null value is converted to raw argument")
    fun <T> MetadataArgumentConfiguration<T?>.testNullBtaArgument() {
        assumeArgumentSupported()
        val operation = kotlinToolchain.metadata.metadataKlibCompilationOperationBuilder(emptyList(), Paths.get(".")).apply {
            compilerArguments[argumentKey] = null
        }.build()

        assertEquals(
            expectedArgumentStringsFor(getValueString(null)),
            operation.compilerArguments.toArgumentStrings(),
        )
    }

    private fun MetadataArgumentConfiguration<*>.assumeArgumentSupported() {
        assumeTrue(kotlinToolchain.supportsMetadata(), "Test requires Metadata BTA support")
        assumeTrue(
            kotlinToolchain.getCompilerVersion() >= argumentKey.availableSinceVersion.toString(),
            "Test requires compiler version >= ${argumentKey.availableSinceVersion}"
        )
    }
}

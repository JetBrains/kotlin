/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl

import org.jetbrains.kotlin.arguments.description.*
import org.jetbrains.kotlin.arguments.description.removed.*
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.arguments.stable.description.actualCommonCompilerArguments as stableCommonCompilerArguments
import org.jetbrains.kotlin.arguments.stable.description.actualCommonKlibBasedArguments as stableCommonKlibBasedArguments
import org.jetbrains.kotlin.arguments.stable.description.actualCommonToolsArguments as stableCommonToolsArguments
import org.jetbrains.kotlin.arguments.stable.description.actualJsArguments as stableJsArguments
import org.jetbrains.kotlin.arguments.stable.description.actualJvmCompilerArguments as stableJvmCompilerArguments
import org.jetbrains.kotlin.arguments.stable.description.actualMetadataArguments as stableMetadataArguments
import org.jetbrains.kotlin.arguments.stable.description.actualNativeArguments as stableNativeArguments
import org.jetbrains.kotlin.arguments.stable.description.actualWasmArguments as stableWasmArguments
import org.jetbrains.kotlin.arguments.stable.dsl.base.KotlinCompilerArgumentsLevel as StableKotlinCompilerArgumentsLevel

class DeprecatedArgumentsConsistencyTest {
    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test deprecated argument name consistency between stable and current compiler arguments")
    fun testArgumentNameConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .forEach { stableArgument ->
                assertTrue(
                    actual = currentLevelCompilerArguments.singleOrNull { it.name == stableArgument.name } != null,
                    message = "Argument with '${stableArgument.name}' name is not found in current compiler arguments."
                )
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test deprecated argument short name consistency between stable and current compiler arguments")
    fun testArgumentShortNameConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .filter { it.shortName != null }
            .forEach { stableArgument ->
                assertTrue(
                    actual = currentLevelCompilerArguments.singleOrNull { it.shortName == stableArgument.shortName } != null,
                    message = "Deprecated argument with '${stableArgument.shortName}' short name is not found in current compiler arguments."
                )
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test deprecated argument description consistency between stable and current compiler arguments")
    fun testArgumentDescriptionConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .forEach { stableArgument ->
                val currentArgument = currentLevelCompilerArguments.single { it.name == stableArgument.name }

                assertTrue(
                    actual = stableArgument.description.current == currentArgument.description.current ||
                            stableArgument.description.current in currentArgument.description.valueInVersions.values,
                    message = "Deprecated argument '${stableArgument.name}' description is not found in the current compiler argument description. " +
                            "Please ensure that previous description is kept in 'valueInVersions' map for backward compatibility."
                )

                stableArgument.description.valueInVersions.forEach { entry ->
                    assertTrue(
                        actual = currentArgument.description.valueInVersions.containsKey(entry.key.asCurrent) &&
                                currentArgument.description.valueInVersions.getValue(entry.key.asCurrent) == entry.value,
                        message = "Deprecated argument '${stableArgument.name}' description 'valueInVersions' $entry is not found in the current " +
                                "compiler argument description. " +
                                "Please ensure that previous description 'valueInVersions' are kept in 'valueInVersions' map for backward compatibility."
                    )
                }
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test deprecated argument type consistency between stable and current compiler arguments")
    fun testArgumentValueTypeConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .forEach { stableArgument ->
                val currentArgument = currentLevelCompilerArguments.single { it.name == stableArgument.name }

                assertEquals(
                    getSuperclassGenericType(stableArgument.valueType::class),
                    getSuperclassGenericType(currentArgument.valueType::class),
                    "Deprecated argument '${stableArgument.name}' type is not consistent with the current compiler argument type: " +
                            "current  type: ${currentArgument.valueType}, stable type: ${stableArgument.valueType}." +
                            "Please ensure that argument type is not changed between releases."
                )
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test deprecated argument description consistency between stable and current compiler arguments")
    fun testArgumentValueDescriptionConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .forEach { stableArgument ->
                val currentArgument = currentLevelCompilerArguments.single { it.name == stableArgument.name }

                assertTrue(
                    actual = stableArgument.valueDescription.current == currentArgument.valueDescription.current ||
                            stableArgument.valueDescription.current in currentArgument.valueDescription.valueInVersions.values,
                    message = "Deprecated argument '${stableArgument.name}' argument type current description not found in the current compiler " +
                            "argument type description. " +
                            "Please ensure that previous argument type description is kept in 'valueInVersions' map for backward compatibility."
                )

                stableArgument.valueDescription.valueInVersions.forEach { entry ->
                    assertTrue(
                        actual = currentArgument.description.valueInVersions.containsKey(entry.key.asCurrent) &&
                                currentArgument.description.valueInVersions.getValue(entry.key.asCurrent) == entry.value,
                        message = "Deprecated argument '${stableArgument.name}' description 'valueInVersions' $entry is not found in the current " +
                                "compiler argument description. " +
                                "Please ensure that previous description 'valueInVersions' are kept in 'valueInVersions' map for backward compatibility."
                    )
                }
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test release versions metadata consistency between stable and current compiler deprecated arguments")
    fun testReleaseVersionsMetadataConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .forEach { stableArgument ->
                val currentArgument = currentLevelCompilerArguments.single { it.name == stableArgument.name }

                assertEquals(
                    expected = stableArgument.releaseVersionsMetadata.introducedVersion.asCurrent,
                    actual = currentArgument.releaseVersionsMetadata.introducedVersion,
                    message = "Introduced version metadata inconsistency for argument: ${stableArgument.name}"
                )

                stableArgument.releaseVersionsMetadata.stabilizedVersion?.run {
                    assertEquals(
                        expected = this.asCurrent,
                        actual = currentArgument.releaseVersionsMetadata.stabilizedVersion,
                        message = "Stablized version metadata inconsistency for argument: ${stableArgument.name}"
                    )
                }

                stableArgument.releaseVersionsMetadata.deprecatedVersion?.run {
                    assertEquals(
                        expected = this.asCurrent,
                        actual = currentArgument.releaseVersionsMetadata.deprecatedVersion,
                        message = "Deprecated version metadata inconsistency for argument: ${stableArgument.name}"
                    )
                }
            }
    }

    companion object {
        @JvmStatic
        fun currentArgumentsLevels(): Stream<Arguments> = Stream.of(
            Arguments.of(stableCommonToolsArguments, actualCommonToolsArguments.mergeWith(removedCommonToolsArguments)),
            Arguments.of(stableCommonCompilerArguments, actualCommonCompilerArguments.mergeWith(removedCommonCompilerArguments)),
            Arguments.of(stableJvmCompilerArguments, actualJvmCompilerArguments.mergeWith(removedJvmCompilerArguments)),
            Arguments.of(stableCommonKlibBasedArguments, actualCommonKlibBasedArguments),
            Arguments.of(stableJsArguments, actualJsArguments.mergeWith(removedJsArguments)),
            Arguments.of(stableWasmArguments, actualWasmArguments.mergeWith(removedWasmArguments)),
            Arguments.of(stableNativeArguments, actualNativeArguments.mergeWith(removedNativeArguments)),
            Arguments.of(stableMetadataArguments, actualMetadataArguments.mergeWith(removedMetadataArguments)),
        )
    }
}
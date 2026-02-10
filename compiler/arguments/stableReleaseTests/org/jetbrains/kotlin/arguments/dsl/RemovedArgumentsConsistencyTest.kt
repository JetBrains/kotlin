/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl

import org.jetbrains.kotlin.arguments.description.removed.removedCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedCommonToolsArguments
import org.jetbrains.kotlin.arguments.description.removed.removedJsArguments
import org.jetbrains.kotlin.arguments.description.removed.removedJvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.removed.removedMetadataArguments
import org.jetbrains.kotlin.arguments.description.removed.removedNativeArguments
import org.jetbrains.kotlin.arguments.description.removed.removedWasmArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.jetbrains.kotlin.arguments.stable.description.removed.removedCommonToolsArguments as stableRemovedCommonToolsArguments
import org.jetbrains.kotlin.arguments.stable.description.removed.removedCommonCompilerArguments as stableRemovedCommonCompilerArguments
import org.jetbrains.kotlin.arguments.stable.description.removed.removedJsArguments as stableRemovedJsArguments
import org.jetbrains.kotlin.arguments.stable.description.removed.removedJvmCompilerArguments as stableRemovedJvmCompilerArguments
import org.jetbrains.kotlin.arguments.stable.description.removed.removedWasmArguments as stableRemovedWasmArguments
import org.jetbrains.kotlin.arguments.stable.description.removed.removedNativeArguments as stableRemovedNativeArguments
import org.jetbrains.kotlin.arguments.stable.description.removed.removedMetadataArguments as stableRemovedMetadataArguments
import org.jetbrains.kotlin.arguments.stable.dsl.base.KotlinCompilerArgumentsLevel as StableKotlinCompilerArgumentsLevel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RemovedArgumentsConsistencyTest {

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test removed argument name consistency between stable and current compiler arguments")
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
    @DisplayName("Test removed argument short name consistency between stable and current compiler arguments")
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
                    message = "Removed argument with '${stableArgument.shortName}' short name is not found in current compiler arguments."
                )
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test removed argument description consistency between stable and current compiler arguments")
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
                    message = "Removed argument '${stableArgument.name}' description is not found in the current compiler argument description. " +
                            "Please ensure that previous description is kept in 'valueInVersions' map for backward compatibility."
                )

                stableArgument.description.valueInVersions.forEach { entry ->
                    assertTrue(
                        actual = currentArgument.description.valueInVersions.containsKey(entry.key.asCurrent) &&
                                currentArgument.description.valueInVersions.getValue(entry.key.asCurrent) == entry.value,
                        message = "Removed argument '${stableArgument.name}' description 'valueInVersions' $entry is not found in the current " +
                                "compiler argument description. " +
                                "Please ensure that previous description 'valueInVersions' are kept in 'valueInVersions' map for backward compatibility."
                    )
                }
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test removed argument type consistency between stable and current compiler arguments")
    fun testArgumentTypeConsistency(
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
                    getSuperclassGenericType(currentArgument.argumentType::class),
                    "Removed argument '${stableArgument.name}' type is not consistent with the current compiler argument type: " +
                            "current  type: ${currentArgument.argumentType}, stable type: ${stableArgument.valueType}." +
                            "Please ensure that argument type is not changed between releases."
                )
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test removed argument description consistency between stable and current compiler arguments")
    fun testArgumentTypeDescriptionConsistency(
        stableLevel: StableKotlinCompilerArgumentsLevel,
        actualLevel: KotlinCompilerArgumentsLevel,
    ) {
        val currentLevelCompilerArguments = actualLevel.arguments
        stableLevel.arguments
            .filterDeprecated()
            .forEach { stableArgument ->
                val currentArgument = currentLevelCompilerArguments.single { it.name == stableArgument.name }

                assertTrue(
                    actual = stableArgument.valueDescription.current == currentArgument.argumentTypeDescription.current ||
                            stableArgument.valueDescription.current in currentArgument.argumentTypeDescription.valueInVersions.values,
                    message = "Removed argument '${stableArgument.name}' argument type current description not found in the current compiler " +
                            "argument type description. " +
                            "Please ensure that previous argument type description is kept in 'valueInVersions' map for backward compatibility."
                )

                stableArgument.valueDescription.valueInVersions.forEach { entry ->
                    assertTrue(
                        actual = currentArgument.description.valueInVersions.containsKey(entry.key.asCurrent) &&
                                currentArgument.description.valueInVersions.getValue(entry.key.asCurrent) == entry.value,
                        message = "Removed argument '${stableArgument.name}' description 'valueInVersions' $entry is not found in the current " +
                                "compiler argument description. " +
                                "Please ensure that previous description 'valueInVersions' are kept in 'valueInVersions' map for backward compatibility."
                    )
                }
            }
    }

    @ParameterizedTest
    @MethodSource("currentArgumentsLevels")
    @DisplayName("Test release versions metadata consistency between stable and current compiler removed arguments")
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

                stableArgument.releaseVersionsMetadata.removedVersion?.run {
                    assertEquals(
                        expected = this.asCurrent,
                        actual = currentArgument.releaseVersionsMetadata.removedVersion,
                        message = "Deprecated version metadata inconsistency for argument: ${stableArgument.name}"
                    )
                }
            }
    }

    companion object {
        @JvmStatic
        fun currentArgumentsLevels(): Stream<Arguments> = Stream.of(
            Arguments.of(stableRemovedCommonToolsArguments, removedCommonToolsArguments),
            Arguments.of(stableRemovedCommonCompilerArguments, removedCommonCompilerArguments),
            Arguments.of(stableRemovedJvmCompilerArguments, removedJvmCompilerArguments),
            //Arguments.of(stableCommonKlibBasedArguments, <no-removed-one>),
            Arguments.of(stableRemovedJsArguments, removedJsArguments),
            Arguments.of(stableRemovedWasmArguments, removedWasmArguments),
            Arguments.of(stableRemovedNativeArguments, removedNativeArguments),
            Arguments.of(stableRemovedMetadataArguments, removedMetadataArguments),
        )
    }
}
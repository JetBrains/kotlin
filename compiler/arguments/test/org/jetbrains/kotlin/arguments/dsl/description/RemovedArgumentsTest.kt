/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments.dsl.description

import org.jetbrains.kotlin.arguments.description.actualCommonCompilerArguments
import org.jetbrains.kotlin.arguments.description.actualCommonKlibBasedArguments
import org.jetbrains.kotlin.arguments.description.actualCommonToolsArguments
import org.jetbrains.kotlin.arguments.description.actualJsArguments
import org.jetbrains.kotlin.arguments.description.actualJvmCompilerArguments
import org.jetbrains.kotlin.arguments.description.actualMetadataArguments
import org.jetbrains.kotlin.arguments.description.actualNativeArguments
import org.jetbrains.kotlin.arguments.description.actualWasmArguments
import org.jetbrains.kotlin.arguments.dsl.base.KotlinCompilerArgumentsLevel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.streams.asStream
import kotlin.test.assertTrue

class RemovedArgumentsTest {

    @DisplayName("Removed arguments are located in the correct place")
    @MethodSource("compilerArgumentsLevels")
    @ParameterizedTest(name = "For level: {0}")
    fun removedArgumentsAreInSeparateLevelDefinition(
        levelDescription: CompilerArgsLevelDescription
    ) {
        val nonMovedArgs = levelDescription.actualLevel.arguments.filter {
            it.releaseVersionsMetadata.removedVersion != null
        }

        assertTrue(
            nonMovedArgs.isEmpty(),
            "The following arguments in ${levelDescription.actualLevelPropertyName} level definition " +
                    "should be moved into ${levelDescription.removedLevelPropertyName}:\n${nonMovedArgs.joinToString(separator = ",\n") { it.name }}"
        )
    }

    companion object {
        @JvmStatic
        fun compilerArgumentsLevels(): Stream<CompilerArgsLevelDescription> = sequenceOf(
            CompilerArgsLevelDescription(
                actualCommonCompilerArguments,
                "actualCommonCompilerArguments",
                "removedCommonCompilerArguments",
            ),
            CompilerArgsLevelDescription(
                actualCommonKlibBasedArguments,
                "actualCommonKlibBasedArguments",
                "removedCommonKlibBasedArguments",
            ),
            CompilerArgsLevelDescription(
                actualCommonToolsArguments,
                "actualCommonToolsArguments",
                "removedCommonToolsArguments",
            ),
            CompilerArgsLevelDescription(
                actualJsArguments,
                "actualJsArguments",
                "removedJsArguments",
            ),
            CompilerArgsLevelDescription(
                actualJvmCompilerArguments,
                "actualJvmCompilerArguments",
                "removedJvmCompilerArguments",
            ),
            CompilerArgsLevelDescription(
                actualMetadataArguments,
                "actualMetadataArguments",
                "removedMetadataArguments",
            ),
            CompilerArgsLevelDescription(
                actualNativeArguments,
                "actualNativeArguments",
                "removedNativeArguments"
            ),
            CompilerArgsLevelDescription(
                actualWasmArguments,
                "actualWasmArguments",
                "removedWasmArguments",
            ),
        ).asStream()
    }

    class CompilerArgsLevelDescription(
        val actualLevel: KotlinCompilerArgumentsLevel,
        val actualLevelPropertyName: String,
        val removedLevelPropertyName: String,
    ) {
        override fun toString(): String {
            return actualLevelPropertyName
        }
    }
}